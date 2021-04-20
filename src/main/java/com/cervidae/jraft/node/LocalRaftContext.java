package com.cervidae.jraft.node;

import com.cervidae.jraft.async.AsyncService;
import com.cervidae.jraft.msg.Message;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;

@Log4j2
@Data
public class LocalRaftContext implements RaftContext {

    int count;

    List<RaftNode> nodes;

    boolean running;

    AsyncService asyncService;

    StateMachine stateMachine;

    public LocalRaftContext(AsyncService asyncService, int count, StateMachine stateMachine) {
        LocalRaftContext.log.info("LocalRaftContext created, starting cluster nodes");
        this.nodes = new ArrayList<>();
        for (int i=0; i<count; i++) {
            var node = new RaftNode(i, this);
            nodes.add(node);
            node.start();
        }
        this.running = true;
        this.count = count;
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
        return nodes.get(target).dispatchRequest(message);
    }

    @Override
    public Message[] blockingBroadcast(Message message) {
        Message[] messages = new Message[count];
        for (RaftNode node: nodes) {
            messages[node.id] = node.dispatchRequest(message);
        }
        return messages;
    }

    @Override
    public Message[] asyncBroadcast(Message message) {
        Message[] messages = new Message[count];
        for (RaftNode node: nodes) {
            asyncService.go(()-> messages[node.id] = node.dispatchRequest(message));
        }
        return messages;
    }

    public boolean apply(LogEntry entry) {
        return stateMachine.apply(entry);
    }
}
