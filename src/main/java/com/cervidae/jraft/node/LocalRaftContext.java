package com.cervidae.jraft.node;

import com.cervidae.jraft.async.AsyncService;
import com.cervidae.jraft.msg.Message;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;

@Log4j2
@Data
@Service
@Lazy
public class LocalRaftContext implements RaftContext {

    final int clusterSize;

    final List<RaftNode> nodes;

    boolean running;

    @JsonIgnore
    private final AsyncService asyncService;

    public LocalRaftContext(AsyncService asyncService, RaftConfiguration config) {
        this.running = false;
        this.clusterSize = config.getClusterSize();
        this.asyncService = asyncService;
        this.nodes = new CopyOnWriteArrayList<>();
        for (int i = 0; i < clusterSize; i++) {
            var node = config.getApplicationContext().getBean(RaftNode.class);
            this.nodes.add(node);
        }
    }

    /**
     * Start the nodes
     */
    @Override
    public void start() {
        LocalRaftContext.log.info("LocalRaftContext created, starting cluster nodes N="+clusterSize);
        nodes.forEach(RaftNode::start);
        this.running = true;
    }

    @Override
    public int getMyID(RaftNode node) {
        return nodes.indexOf(node);
    }

    @Override
    public int newEntry(LogEntry entry) {
        for (RaftNode node: nodes) {
            var index = node.newEntry(entry);
            if (index != -1) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public void shutdown() {
        nodes.forEach(RaftNode::shutdown);
    }

    @Override
    public Message sendMessage(int target, Message message) throws ResourceAccessException {
        var node = nodes.get(target);
        if (node.isDEBUG_DISCONNECT() || node.isKilled()) throw new ResourceAccessException("Read timed out");
        return node.dispatchRequest(message);
    }
}
