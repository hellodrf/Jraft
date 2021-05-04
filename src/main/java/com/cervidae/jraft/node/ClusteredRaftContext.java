package com.cervidae.jraft.node;

import com.cervidae.jraft.async.AsyncService;
import com.cervidae.jraft.msg.Message;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Log4j2
@Data
@Service
@Lazy
public class ClusteredRaftContext implements RaftContext {

    int clusterSize;

    int id;

    RaftNode node;

    String nodeURLs;

    boolean running;

    final AsyncService asyncService;

    public ClusteredRaftContext(AsyncService asyncService, RaftConfiguration config) {
        ClusteredRaftContext.log.info("ClusteredRaftContext created and starting");
        this.id = config.getClusteredId();
        this.asyncService = asyncService;
        this.nodeURLs = config.getClusteredUrls();
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
        this.running = false;
        this.node.shutdown();
    }

    @Override
    public int getMyID(RaftNode node) {
        return id;
    }

    @Override
    public Message sendMessage(int target, Message message) {
        return null;
    }

    @Override
    public List<RaftNode> getNodes() {
        return new ArrayList<>(Collections.singletonList(node));
    }
}
