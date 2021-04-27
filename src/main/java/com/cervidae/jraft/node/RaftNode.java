package com.cervidae.jraft.node;

import com.cervidae.jraft.async.ArgRunnable;
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

import java.io.Serializable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Data
@Component
@Lazy
@Scope("prototype")
public class RaftNode implements Serializable {

    public static final long serialVersionUID = 187779204898809752L;

    public enum State {
        FOLLOWER((node)->{ // FROM

        }, (node)->{ // TO

        }),

        CANDIDATE((node)->{ // FROM

        }, (node)->{ // TO

        }),

        LEADER((node)->{ // FROM

        }, (node)->{ // TO
            
        });

        // ArgRunnable: refer to jraft.async.ArgRunnable
        ArgRunnable<RaftNode> from; // executed upon *transitioning FROM this state*
        ArgRunnable<RaftNode> to; // executed upon *transitioning TO this state*

        State(ArgRunnable<RaftNode> from, ArgRunnable<RaftNode> to) {
            this.from = from;
            this.to = to;
        }

        public State transition(State targetState, RaftNode node) {
            this.from.run(node);
            targetState.to.run(node);
            return targetState;
        }
    }

    private final TimeUnit GLOBAL_TIMEUNIT = TimeUnit.MILLISECONDS;

    private final long ELECTION_DELAY = 500L;

    private int id;

    private State state;

    private boolean killed;

    @JsonIgnore
    private final RaftConfiguration config;

    @JsonIgnore
    private Logger log;

    @JsonIgnore
    private RaftContext context;

    @JsonIgnore
    private Map<String, Timer> timers = new ConcurrentHashMap<>();

    @JsonIgnore
    final ReadWriteLock masterMutex = new ReentrantReadWriteLock();

    @JsonIgnore
    final ReadWriteLock electionMutex = new ReentrantReadWriteLock();

    @JsonIgnore
    final ReadWriteLock logMutex = new ReentrantReadWriteLock();

    @JsonIgnore
    final StateMachine stateMachine;

    @JsonIgnore
    final AsyncService asyncService;

    /**
     * Do not modify! Autowired by SpringIOC
     */
    @Autowired
    public RaftNode(StateMachine stateMachine, AsyncService asyncService, RaftConfiguration config) {
        this.config = config;
        this.stateMachine = stateMachine;
        this.asyncService = asyncService;
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
            this.log = org.apache.logging.log4j.LogManager.getLogger(this.getClass().getName() + "-" + this.id);

            asyncService.go(() -> {
                throw new RuntimeException();
            }); // do this if you want to go async

            timers.put("electionTimer", new Timer());
            timers.get("electionTimer").schedule(new TimerTask() {
                @Override
                public void run() {

                }
            }, ELECTION_DELAY, ELECTION_DELAY);

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


    public void election() {

    }

    public void heartbeat() {

    }

    public AppendEntriesReply appendEntriesHandler(AppendEntriesRequest msg) {
        return null;
    }

    public RequestVoteReply requestVoteHandler(RequestVoteRequest msg) {
        return null;
    }

    public Message dispatchRequest(Message msg) {
        if (msg instanceof AppendEntriesRequest) {
            return appendEntriesHandler((AppendEntriesRequest)msg);
        } else if (msg instanceof RequestVoteRequest) {
            return requestVoteHandler((RequestVoteRequest)msg);
        }
        throw new IllegalArgumentException("Invalid message type: " + msg.getType());
    }
}
