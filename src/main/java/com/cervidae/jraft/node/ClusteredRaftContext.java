package com.cervidae.jraft.node;

import com.cervidae.jraft.async.AsyncService;
import com.cervidae.jraft.msg.Message;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Log4j2
@Data
public class ClusteredRaftContext implements RaftContext {

    int count;

    int id;

    RaftNode node;

    String[] nodeIPs;

    boolean running;

    final AsyncService asyncService;

    final StateMachine stateMachine;

    public ClusteredRaftContext(AsyncService asyncService, int count, int id, String[] ips, StateMachine stateMachine) {
        ClusteredRaftContext.log.info("ClusteredRaftContext created and starting");
        this.id = id;
        this.nodeIPs = ips;
        this.count = count;
        this.node = new RaftNode(id, this);
        this.running = true;
        this.node.start();
        this.asyncService = asyncService;
        this.stateMachine = stateMachine;
    }

    @Override
    public int addEntry(LogEntry entry) {
        return 0;
    }

    @Override
    public boolean shutdown() {
        return false;
    }

    @Override
    public Message sendMessage(int target, Message message) {
        return null;
    }

    @Override
    public Message[] blockingBroadcast(Message message) {
        return null;
    }

    @Override
    public Message[] asyncBroadcast(Message message) {
        return null;
    }

    @Override
    public boolean apply(LogEntry entry) {
        return stateMachine.apply(entry);
    }
}
