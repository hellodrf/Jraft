package com.cervidae.jraft.node;

import com.cervidae.jraft.async.AsyncService;
import com.cervidae.jraft.msg.*;
import com.cervidae.jraft.statemachine.StateMachine;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

@Data
@Component
@Lazy
@Scope("prototype")
public class RaftNode implements Serializable {

    public static final long serialVersionUID = 187779204898809752L;

    public enum State {
        FOLLOWER((node) -> { // FROM
            node.log.info("electionTimer cancelled");
            var timer = node.timers.get("electionTimer");
            if (timer != null) {
                timer.cancel();
            }
        }, (node) -> { // TO
            node.log.debug("Converting to follower");
            node.lastHeartbeat = System.currentTimeMillis();
            node.timers.put("electionTimer", new Timer("electionTimer", true));
            node.timers.get("electionTimer").schedule(new TimerTask() {
                @Override
                public void run() {
                    node.checkElectionTimeout();
                }
            }, node.ELECTION_DELAY);
        }),

        CANDIDATE((node) -> { // FROM

        }, (node) -> { // TO
            node.log.debug("Converting to candidate");
        }),

        LEADER((node) -> { // FROM

        }, (node) -> { // TO
            node.log.info("Converting to leader & Starting heartbeat");
            node.heartbeat();
            node.timers.put("heartbeatTimer", new Timer("heartbeatTimer", true));
            node.timers.get("heartbeatTimer").schedule(new TimerTask() {
                @Override
                public void run() {
                    if (node.killed || node.state != State.LEADER) {
                        return;
                    }
                    node.heartbeat();
                }
            }, 0, RaftConfiguration.HEARTBEAT_FREQUENCY);
        });

        final Consumer<RaftNode> from; // executed upon *transitioning FROM this state*
        final Consumer<RaftNode> to; // executed upon *transitioning TO this state*

        State(Consumer<RaftNode> from, Consumer<RaftNode> to) {
            this.from = from;
            this.to = to;
        }

        public void transition(State targetState, RaftNode node) {
            if (this == targetState) return;
            this.from.accept(node);
            targetState.to.accept(node);
            node.state = targetState;
        }
    }

    private int id;
    private volatile State state;
    private volatile boolean killed;
    private AtomicInteger currentTerm = new AtomicInteger(0);
    private volatile int votedFor = -1;
    private volatile int voteTerm = -1;
    private volatile int lastApplied = -1;
    private volatile long lastHeartbeat;
    private final long ELECTION_DELAY;

    private List<LogEntry> logEntries = new CopyOnWriteArrayList<>();

    @JsonIgnore
    final ReadWriteLock masterMutex = new ReentrantReadWriteLock();
    @JsonIgnore
    final ReadWriteLock electionMutex = new ReentrantReadWriteLock();
    @JsonIgnore
    final ReadWriteLock logMutex = new ReentrantReadWriteLock();

    @JsonIgnore
    private Map<String, Timer> timers = new ConcurrentHashMap<>();

    @JsonIgnore
    final StateMachine stateMachine;
    @JsonIgnore
    final AsyncService asyncService;
    @JsonIgnore
    private RaftContext context;
    @JsonIgnore
    private Logger log;
    @JsonIgnore
    private final RaftConfiguration config;

    /**
     * Do not modify! Autowired by SpringIOC
     */
    @Autowired
    public RaftNode(StateMachine stateMachine, AsyncService asyncService, RaftConfiguration config) {
        this.config = config;
        this.stateMachine = stateMachine;
        this.asyncService = asyncService;
        this.ELECTION_DELAY = ThreadLocalRandom.current().nextLong(RaftConfiguration.MIN_ELECTION_DELAY,
                RaftConfiguration.MAX_ELECTION_DELAY + 1);
    }

    /**
     * Use this method to initialise RaftNode & start async processes.
     * Will be invoked by RaftContext after creation.
     */
    @Async
    public void start() {
        masterMutex.writeLock().lock();
        try {
            this.context = config.getRaftContext();
            this.killed = false;
            this.state = State.FOLLOWER;

            this.id = context.getMyID(this);
            this.lastHeartbeat = System.currentTimeMillis();
            this.log = org.apache.logging.log4j.LogManager.getLogger(this.getClass().getName() + this.id);
            State.FOLLOWER.to.accept(this);

            asyncService.go(() -> {

            }); // do this if you want to go async

            log.info("Node " + this.id + " initialised");
        } finally {
            masterMutex.writeLock().unlock();
        }
    }

    public void shutdown() {
        this.killed = true;
        timers.forEach((name, timer) -> timer.cancel());
    }

    public State getState() {
        masterMutex.readLock().lock();
        electionMutex.readLock().lock();
        try {
            return this.state;
        } finally {
            masterMutex.readLock().unlock();
            electionMutex.readLock().unlock();
        }
    }

    public int newEntry(LogEntry entry) {
        if (getState() != State.LEADER) {
            return -1;
        }
        // DO Something
        return 0;
    }

    public synchronized void incrementAndCheckVoteCount(AtomicInteger voteCount, int threshold) {
        if (voteCount.incrementAndGet() == threshold+1 || this.state == State.CANDIDATE) {
            log.info("election won!");
            this.state.transition(State.LEADER, this);
        }
    }

    @Async
    public void election(String reason) {
        if (this.killed || this.state != State.FOLLOWER) {
            log.info("Election ceased prematurely (no need to elect)");
            return;
        }
        try {
            electionMutex.writeLock().lock();
            if (this.killed || this.state != State.FOLLOWER) {
                log.info("Election ceased prematurely (no need to elect)");
                return;
            }
            log.info("Election triggered by " + reason);
            this.state.transition(State.CANDIDATE, this);
            this.currentTerm.incrementAndGet();
            this.votedFor = this.id;
            this.voteTerm = this.currentTerm.get();
            var voteCount = new AtomicInteger(1);
            int threshold = config.getClusterSize() / 2;
            var req = this.logEntries.size() > 0 ?
                    new RequestVoteRequest(this.currentTerm.get(), this.id, logEntries.size() - 1,
                            this.logEntries.get(logEntries.size() - 1).term) :
                    new RequestVoteRequest(this.currentTerm.get(), this.id, -1, -1);
            electionMutex.writeLock().unlock();

            for (int i = 0; i < config.getClusterSize(); i++) {
                if (this.killed || this.state != State.CANDIDATE) {
                    return;
                }
                if (i == this.id) continue;
                int rcv = i;
                asyncService.go(() -> {
                    if (this.killed || this.state != State.CANDIDATE) {
                        return;
                    }
                    log.info("sending RequestVote to " + rcv);
                    RequestVoteReply reply;
                    try {
                        reply = (RequestVoteReply) context.sendMessage(rcv, req);
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                        return;
                    }
                    if (this.killed || this.state != State.CANDIDATE) {
                        return;
                    }
                    if (reply.isVoteGranted()) {
                        log.info("vote received from " + rcv);
                        incrementAndCheckVoteCount(voteCount, threshold);
                    } else {
                        log.info("de-vote received from " + rcv);
                    }
                });
            }
            System.out.println("latch unlocked!");
        } finally {

        }
    }

    public void heartbeat() {
        var msg = new AppendEntriesRequest(this, null);
        for (int i = 0; i < config.getClusterSize(); i++) {
            if (i== this.id) continue;
            int finalI = i;
            asyncService.go(()->{
                try {
                    log.info("sending heartbeat to N" + finalI);
                    var reply = (AppendEntriesReply)context.sendMessage(finalI, msg);
                    if (!reply.isSuccess()) {
                        log.info("heartbeat rejected by N" + finalI);
                    } else {
                        log.info("heartbeat accepted by N" + finalI);
                    }
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public synchronized void checkElectionTimeout() {
        try {
            this.electionMutex.writeLock().lock();
            if (this.killed || this.state != State.FOLLOWER) {
                return;
            }
            if (System.currentTimeMillis() - this.lastHeartbeat >= this.ELECTION_DELAY) {
                this.election("heartbeat timeout");
                return;
            }
            var next = this.ELECTION_DELAY - (System.currentTimeMillis() - this.lastHeartbeat);
            Assert.isTrue(next > 0, "timer error? " + next);
            this.timers.get("electionTimer").cancel();
            this.timers.put("electionTimer", new Timer("electionTimer", true));
            var node = this;
            this.timers.get("electionTimer").schedule(new TimerTask() {
                @Override
                public void run() {
                    node.checkElectionTimeout();
                }
            }, next);
        } finally {
            this.electionMutex.writeLock().unlock();
        }
    }

    public synchronized void resetElectionTimer() {
        this.timers.get("electionTimer").cancel();
        this.timers.put("electionTimer", new Timer("electionTimer", true));
        var node = this;
        this.timers.get("electionTimer").schedule(new TimerTask() {
            @Override
            public void run() {
                node.checkElectionTimeout();
            }
        }, this.ELECTION_DELAY);
    }

    public synchronized AppendEntriesReply appendEntriesHandler(AppendEntriesRequest msg) {
        if (this.currentTerm.get() < msg.getTerm()) {
            this.log.info(msg.getType() + " RPC from N" + msg.getLeaderID() + "has term " + msg.getTerm() +
                    " > current term" + this.getCurrentTerm().get() + ", converting to follower");
            this.currentTerm.set(msg.getTerm());
            this.state.transition(State.FOLLOWER, this);
            this.voteTerm = this.currentTerm.get();
            this.votedFor = msg.getLeaderID();
        }

        var reply = new AppendEntriesReply(this.getCurrentTerm().get(), false);
        if (this.currentTerm.get() > msg.getTerm()) {
            this.log.info(msg.getType() + " RPC rejected - Term outdated :" +  msg.getTerm() + this.getCurrentTerm().get());
            return reply;
        }

        this.lastHeartbeat = System.currentTimeMillis();
        this.resetElectionTimer();
        reply.setSuccess(true);

        // do log stuff here

        return reply;
    }

    public synchronized RequestVoteReply requestVoteHandler(RequestVoteRequest msg) {
        if (this.currentTerm.get() < msg.getTerm()) {
            this.log.info(msg.getType() + " RPC from N" + msg.getCandidateID() + " has term " + msg.getTerm() +
                    " > my term " + this.getCurrentTerm().get() + ", converting to follower");
            this.currentTerm.set(msg.getTerm());
            this.state.transition(State.FOLLOWER, this);
            this.resetElectionTimer();
        }
        var reply = new RequestVoteReply(this.currentTerm.get(), false);

        if (this.currentTerm.get() > msg.getTerm()) {
            return reply;
        }

        if (this.voteTerm >= msg.getTerm()) {
            return reply;
        }

        if (this.logEntries.size() - 1 > msg.getLastLogIndex()) {
            return reply;
        }

        reply.setVoteGranted(true);
        this.voteTerm = this.currentTerm.get();
        this.votedFor = msg.getCandidateID();
        return reply;
    }

    public Message dispatchRequest(Message msg) {
        if (msg instanceof AppendEntriesRequest) {
            return appendEntriesHandler((AppendEntriesRequest) msg);
        } else if (msg instanceof RequestVoteRequest) {
            return requestVoteHandler((RequestVoteRequest) msg);
        }
        throw new IllegalArgumentException("Invalid message type: " + msg.getType());
    }
}
