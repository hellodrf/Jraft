package com.cervidae.jraft.node;

import com.cervidae.jraft.msg.AppendEntriesReply;
import com.cervidae.jraft.msg.Message;
import com.cervidae.jraft.msg.RequestVoteReply;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.io.Serializable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Data
@Log4j2
public class RaftNode implements Serializable {

    public static final long serialVersionUID = 187779204898809752L;

    public enum State {
        FOLLOWER, CANDIDATE, LEADER
    }

    int id;

    private State state;

    @JsonIgnore
    RaftContext context;

    @JsonIgnore
    ReadWriteLock masterMutex = new ReentrantReadWriteLock();

    @JsonIgnore
    ReadWriteLock electionMutex = new ReentrantReadWriteLock();

    @JsonIgnore
    ReadWriteLock logMutex = new ReentrantReadWriteLock();

    @JsonIgnore
    StateMachine stateMachine;

    public RaftNode(int id, RaftContext context) {
        this.id = id;
        this.context = context;
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

    public void start() {

    }

    public void shutdown() {

    }

    public void election() {

    }

    public void campaign() {

    }

    public void coronation() {

    }

    public void abdicate() {

    }

    public void heartbeat() {

    }

    public AppendEntriesReply appendEntriesHandler() {
        return null;
    }

    public RequestVoteReply requestVoteHandler() {
        return null;
    }

    public Message dispatchRequest(Message msg) {
        return null;
    }

}
