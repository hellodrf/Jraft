package com.cervidae.jraft.node;

import com.cervidae.jraft.async.ArgRunnable;
import com.cervidae.jraft.async.AsyncService;
import com.cervidae.jraft.msg.Message;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.concurrent.Callable;

@Log4j2
@Data
@Service
@Lazy
public class ClusteredRaftContext implements RaftContext {

    int clusterSize;

    int id;

    RaftNode node;

    String[] nodeIPs;

    boolean running;

    final AsyncService asyncService;

    public ClusteredRaftContext(AsyncService asyncService, RaftConfiguration config) {
        ClusteredRaftContext.log.info("ClusteredRaftContext created and starting");
        this.id = config.getClusteredId();
        this.asyncService = asyncService;
        this.nodeIPs = config.getClusteredIPs();
        this.clusterSize = config.getClusterSize();
        this.running = false;
        this.node = config.getApplicationContext().getBean(RaftNode.class);
    }

    @Override
    public int newEntry(LogEntry entry) {
        return 0;
    }

    @Override
    public void start() {
        this.running = true;
        this.node.start();
    }

    @Override
    public void shutdown() {

    }

    @Override
    public int getMyID(RaftNode node) {
        return 0;
    }

    @Override
    public Message sendMessage(int target, Message message) {
        return null;
    }

    @Override
    public void blockingBroadcast(Message message, ArgRunnable<Message> callback) {

    }

    @Override
    public void asyncBroadcast(Message message, ArgRunnable<Message> callback) {

    }

}
