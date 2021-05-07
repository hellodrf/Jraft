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
import org.springframework.web.client.ResourceAccessException;

import java.io.Serializable;
import java.util.*;
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

    /**
     * States
     * Follower, Candidate and Leader
     * Use transition() to change state
     * eg. changing state from current state (this.state) to leader:
     * this.state.transition(State.LEADER, this)
     */
    public enum State {
        FOLLOWER((node) -> { // FROM
            node.getLogger().info("ElectionTimer cancelled");
            var timer = node.timers.get("electionTimer");
            if (timer != null) {
                timer.cancel();
            }
        }, (node) -> { // TO
            node.getLogger().debug("Converting to follower");
            node.lastHeartbeat = System.currentTimeMillis();
            node.resetElectionTimer();
        }),

        CANDIDATE((node) -> { // FROM

        }, (node) -> { // TO
            node.getLogger().debug("Converting to candidate");
        }),

        LEADER((node) -> { // FROM
            node.getLogger().info("HeartbeatTimer cancelled");
            var timer = node.timers.get("heartbeatTimer");
            if (timer != null) {
                timer.cancel();
            }
        }, (node) -> { // TO
            node.timers.put("heartbeatTimer", new Timer("heartbeatTimer", true));
            node.timers.get("heartbeatTimer").schedule(new TimerTask() {
                @Override
                public void run() {
                    if (node.killed || node.state != State.LEADER) {
                        return;
                    }
                    node.getLogger().debug("Heartbeat ticked!");
                    node.heartbeat();
                }
            }, 0, node.config.HEARTBEAT_FREQUENCY);
            node.getLogger().info("Converting to leader and starting heartbeat");
            if (node.context instanceof ClusteredRaftContext) {
                ((ClusteredRaftContext) node.context).notifyMonitor("N" + node.id+" - I AM LEADER (TERM "
                        +node.currentTerm.get() + ")");
            }
        });

        final Consumer<RaftNode> from; // executed upon *transitioning FROM this state*
        final Consumer<RaftNode> to; // executed upon *transitioning TO this state*

        State(Consumer<RaftNode> from, Consumer<RaftNode> to) {
            this.from = from;
            this.to = to;
        }

        public void transition(State targetState, RaftNode node) {
            if (this == targetState) return;
            node.state = targetState;
            this.from.accept(node);
            targetState.to.accept(node);
        }
    }

    /**
     * Standard Raft params
     */
    private int id;
    private volatile State state;
    private volatile boolean killed;
    private AtomicInteger currentTerm = new AtomicInteger(0);
    private volatile int votedFor = -1;
    private volatile int voteTerm = -1;
    private volatile int lastApplied = -1;

    private List<LogEntry> logEntries = Collections.synchronizedList(new ArrayList<>());

    /**
     * Additional params
     */
    private volatile long lastHeartbeat;

    private final long ELECTION_DELAY;
    private boolean DEBUG_DISCONNECT = false;

    /**
     * Locks
     */
    @JsonIgnore
    private final ReadWriteLock electionMutex = new ReentrantReadWriteLock();

    /**
     * Timers
     * electionTimer, heartbeatTimer
     */
    @JsonIgnore
    private Map<String, Timer> timers = new ConcurrentHashMap<>();

    /**
     * References to other services
     */
    @JsonIgnore
    private final StateMachine stateMachine;
    @JsonIgnore
    private final AsyncService asyncService;
    @JsonIgnore
    private RaftContext context;
    @JsonIgnore
    private final RaftConfig config;

    /**
     * Do not modify! Autowired by SpringIOC
     */
    @Autowired
    public RaftNode(StateMachine stateMachine, AsyncService asyncService, RaftConfig config) {
        this.config = config;
        this.stateMachine = stateMachine;
        this.asyncService = asyncService;
        this.ELECTION_DELAY = ThreadLocalRandom.current().nextLong(config.MIN_ELECTION_DELAY,
                config.MAX_ELECTION_DELAY + 1);
    }

    /**
     * Use this method to initialise RaftNode & start async processes.
     * Will be invoked by RaftContext after creation.
     */
    @Async
    public void start() {
        this.context = config.getRaftContext();
        this.killed = false;
        this.state = State.FOLLOWER;
        this.id = context.getMyID(this);
        this.lastHeartbeat = System.currentTimeMillis();
        getLogger().info("Node " + this.id + " initialised, waiting for election timer");
        try {
            Thread.sleep(ELECTION_DELAY);
        } catch (InterruptedException ignored) {
        }

        if (this.getCurrentTerm().get() == 0) {
            // no leader exists (or the term will be updated by heartbeat)
            this.election("cold start-up", false);
        }
    }

    /**
     * Destructor
     */
    public void shutdown() {
        this.killed = true;
        timers.forEach((name, timer) -> timer.cancel());
    }

    /**
     * Use this logger (not the same as LogEntries!) to log all info/warnings
     *
     * @return logger
     */
    private Logger getLogger() {
        return org.apache.logging.log4j.LogManager.getLogger(this.toString());
    }

    /**
     * Covert node to an informative string
     *
     * @return string representing this node
     */
    public String toString() {
        return "N" + id + "[" + state.toString().substring(0, 4) + "|Term" + currentTerm.get() + "|V" + votedFor + "|VT" + voteTerm +
                "|HBDue" + (ELECTION_DELAY - System.currentTimeMillis() + lastHeartbeat) + "|D" + (killed ? 1 : 0) + "]";
    }

    /**
     * Push a new log entry to consensus
     *
     * @param entry log entry
     * @return promised entry index
     */
    public int newEntry(String entry) {
        if (getState() != State.LEADER) {
            return -1;
        }
        // DO Something
        return 0;
    }

    /**
     * Private helper function
     */
    private boolean incrementAndCheckVoteCount(AtomicInteger voteCount, int threshold) {
        if (voteCount.incrementAndGet() > threshold && this.state == State.CANDIDATE) {
            this.state.transition(State.LEADER, this);
            getLogger().info("Election finalised, I am leader of term " + getCurrentTerm().get());
            return true;
        } else if (this.state != State.CANDIDATE) {
            getLogger().debug("I am not Candidate? Cannot perform vote check");
        }
        return false;
    }

    /**
     * Start a new election
     *
     * @param reason description for this election (solely for logging purposes)
     * @param retry  is this a retry attempt? (triggered by failed election)
     */
    @Async
    public void election(String reason, boolean retry) {
        if (this.killed || this.state != State.FOLLOWER) {
            getLogger().info("Election ceased prematurely (no need to elect)");
            return;
        }
        try {
            electionMutex.writeLock().lock();
            if (this.killed || this.state != State.FOLLOWER) {
                getLogger().info("Election ceased prematurely (no need to elect)");
                return;
            }
            getLogger().info("Election triggered by " + reason);
            this.state.transition(State.CANDIDATE, this);
            this.votedFor = this.id;
            this.voteTerm = this.currentTerm.incrementAndGet();
            var voteCount = new AtomicInteger(1);
            int threshold = config.getClusterSize() / 2;
            var req = this.logEntries.size() > 0 ?
                    new RequestVoteRequest(this.currentTerm.get(), this.id, logEntries.size() - 1,
                            this.logEntries.get(logEntries.size() - 1).term) :
                    new RequestVoteRequest(this.currentTerm.get(), this.id, -1, -1);
            var latch = new CountDownLatch(config.getClusterSize() - 1);
            for (int i = 0; i < config.getClusterSize(); i++) {
                if (this.killed || this.state != State.CANDIDATE) {
                    return;
                }
                if (i == this.id) continue;
                int rcv = i;
                asyncService.go(() -> {
                    try {
                        if (this.killed || this.state != State.CANDIDATE) {
                            return;
                        }
                        getLogger().info("Sending RequestVote to " + rcv);
                        RequestVoteReply reply;
                        try {
                            reply = (RequestVoteReply) context.sendMessage(rcv, req);
                        } catch (Exception e) {
                            getLogger().warn("RequestVote RPC to N" + rcv + " timed out?");
                            return;
                        }
                        if (this.killed || this.state != State.CANDIDATE) {
                            getLogger().debug("I am not Candidate? Cannot process votes");
                            return;
                        }
                        if (reply.isVoteGranted()) {
                            incrementAndCheckVoteCount(voteCount, threshold);
                            getLogger().info("Vote received from " + rcv +
                                    " (VC=" + voteCount.get() + "|TH=" + threshold + ")");
                        } else {
                            getLogger().info("De-Vote received from " + rcv +
                                    " (VC=" + voteCount.get() + "|TH=" + threshold + ")");
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            if (latch.await(1000, config.GLOBAL_TIMEUNIT)) {
                if (voteCount.get() > threshold) {
                    incrementAndCheckVoteCount(voteCount, threshold);
                } else {
                    if (this.state != State.CANDIDATE) {
                        return;
                    }
                    getLogger().warn("Election failed, try starting a new election");
                    Thread.sleep(ELECTION_DELAY);
                    if (this.state != State.CANDIDATE) {
                        return;
                    }
                    this.state.transition(State.FOLLOWER, this);
                    election("failed election", true);
                }
            } else {
                if (this.state != State.CANDIDATE) {
                    return;
                }
                getLogger().warn("Election latch timeout, some nodes are down?");
                if (incrementAndCheckVoteCount(voteCount, threshold)) {
                    return;
                }
                Thread.sleep(ELECTION_DELAY);
                if (this.state != State.CANDIDATE) {
                    return;
                }
                this.state.transition(State.FOLLOWER, this);
                election("failed election", true);
            }
        } catch (InterruptedException e) {
            getLogger().info("Election latch interrupted?");
        } finally {
            electionMutex.writeLock().unlock();
        }
    }

    /**
     * Send one heartbeat to all followers
     */
    public void heartbeat() {
        if (this.state != State.LEADER) return;
        var msg = new AppendEntriesRequest(this, null);
        for (int i = 0; i < config.getClusterSize(); i++) {
            if (i == this.id) continue;
            int finalI = i;
            asyncService.go(() -> {
                try {
                    getLogger().debug("Sending heartbeat to N" + finalI);
                    var reply = (AppendEntriesReply) context.sendMessage(finalI, msg);
                    if (!reply.isSuccess()) {
                        getLogger().warn("Heartbeat rejected by N" + finalI);
                        this.state.transition(State.FOLLOWER, this);
                    } else {
                        getLogger().info("Heartbeat accepted by N" + finalI);
                    }
                } catch (ResourceAccessException e) {
                    getLogger().warn("Heartbeat RPC to N" + finalI + " timed out?");
                }
            });
        }
    }

    /**
     * Check if the leader timed-out (by checking time of last heartbeat)
     */
    private synchronized void checkElectionTimeout() {
        if (this.killed || this.state != State.FOLLOWER) {
            return;
        }
        if (System.currentTimeMillis() - this.lastHeartbeat >= this.ELECTION_DELAY) {
            this.election("Leader timeout", false);
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
    }

    /**
     * reset the election timer to ELECTION_DELAY
     */
    private synchronized void resetElectionTimer() {
        if (this.timers.get("electionTimer") != null) {
            this.timers.get("electionTimer").cancel();
        }
        this.timers.put("electionTimer", new Timer("electionTimer", true));
        var node = this;
        this.timers.get("electionTimer").schedule(new TimerTask() {
            @Override
            public void run() {
                node.checkElectionTimeout();
            }
        }, this.ELECTION_DELAY);
    }

    /**
     * Handler for appendEntries RPC
     *
     * @param msg request msg
     * @return reply msg
     */
    public AppendEntriesReply appendEntriesHandler(AppendEntriesRequest msg) {
        var reply = new AppendEntriesReply(this.getCurrentTerm().get(), false);

        // Validate rpc
        if (this.currentTerm.get() < msg.getTerm()) {
            if (state != State.FOLLOWER) {
                getLogger().info(msg.getType() + " RPC from N" + msg.getLeaderID() + " has term " + msg.getTerm() +
                        " > my term " + this.getCurrentTerm().get());
            }
            this.currentTerm.set(msg.getTerm());
        } else if (this.currentTerm.get() > msg.getTerm()) {
            getLogger().info(msg.getType() + " RPC rejected - Term outdated (MSG" +
                    msg.getTerm() +"|CUR"+ this.getCurrentTerm().get() + ")");
            return reply;
        }

        this.voteTerm = msg.getTerm();
        this.votedFor = msg.getLeaderID();
        this.lastHeartbeat = System.currentTimeMillis();
        this.resetElectionTimer();
        reply.setSuccess(true);
        if (state != State.FOLLOWER) {
            this.state.transition(State.FOLLOWER, this);
            getLogger().debug("Valid AppendEntries RPC received from N" + msg.getLeaderID()
                    + ", converting to follower");
        }
        if (msg.getEntries() == null) {
            getLogger().debug("Heartbeat received from N" + msg.getLeaderID());
            return reply;
        }

        // do log stuff here

        return reply;
    }

    /**
     * Handler for requestVote RPC
     *
     * @param msg request msg
     * @return reply msg
     */
    public RequestVoteReply requestVoteHandler(RequestVoteRequest msg) {
        if (this.currentTerm.get() < msg.getTerm()) {
            if (state != State.FOLLOWER) {
                getLogger().info(msg.getType() + " RPC from N" + msg.getCandidateID() + " has term " + msg.getTerm() +
                        " > my term " + this.getCurrentTerm().get() + ", converting to follower");
            }
            this.currentTerm.set(msg.getTerm());
            this.state.transition(State.FOLLOWER, this);
            if (this.getCurrentTerm().get() != 0) {
                // prevent double election
                this.resetElectionTimer();
            }
        }
        var reply = new RequestVoteReply(this.currentTerm.get(), false);

        if (this.currentTerm.get() > msg.getTerm()) {
            getLogger().info("De-voted N" + msg.getCandidateID() + " - term outdated");
            return reply;
        }
        if (this.voteTerm >= msg.getTerm()) {
            getLogger().info("De-voted N" + msg.getCandidateID() + " - already voted");
            return reply;
        }
        if (this.logEntries.size() - 1 > msg.getLastLogIndex()) {
            getLogger().info("De-voted N" + msg.getCandidateID() + " -  logs outdated");
            return reply;
        }
        reply.setVoteGranted(true);
        this.voteTerm = msg.getTerm();
        this.votedFor = msg.getCandidateID();
        getLogger().info("Voted N" + msg.getCandidateID());
        return reply;
    }

    /**
     * Dispatch msg to their handlers
     *
     * @param msg request msg
     * @return reply msg
     */
    public Message dispatchRequest(Message msg) {
        if (msg instanceof AppendEntriesRequest) {
            return appendEntriesHandler((AppendEntriesRequest) msg);
        } else if (msg instanceof RequestVoteRequest) {
            return requestVoteHandler((RequestVoteRequest) msg);
        }
        throw new IllegalArgumentException("Invalid message type: " + msg.getType());
    }
}
