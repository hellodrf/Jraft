package com.cervidae.jraft.node;

import com.cervidae.jraft.async.AsyncService;
import com.cervidae.jraft.model.Account;
import com.cervidae.jraft.msg.AppendEntriesRequest;
import com.cervidae.jraft.msg.Message;
import com.cervidae.jraft.msg.RequestVoteReply;
import com.cervidae.jraft.msg.RequestVoteRequest;
import com.cervidae.jraft.restful.RestClientService;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Log4j2
@Data
@Service
@Lazy
public class ClusteredRaftContext implements RaftContext {

    /**
     * Cluster params
     */
    int clusterSize;

    /**
     * Node params
     */
    int id;
    RaftNode node;
    List<String> nodeURLs;
    boolean running;

    /**
     * External Services
     */
    final AsyncService asyncService;
    final RestClientService restClientService;


    List<String> messageLogs = new ArrayList<>(1000);

    public ClusteredRaftContext(AsyncService asyncService, RaftConfiguration config, RestClientService restClientService) {
        ClusteredRaftContext.log.info("ClusteredRaftContext created and starting");
        this.restClientService = restClientService;
        this.id = config.getClusteredId();
        this.asyncService = asyncService;
        this.nodeURLs = config.getClusteredUrls();
        this.clusterSize = config.getClusterSize();
        this.running = false;
        if (this.id != -1) {
            this.node = config.getApplicationContext().getBean(RaftNode.class);
        }
    }

    @Override
    public int newEntry(String entry) {
        return 0;
    }

    @Override
    public void start() {
        if (this.id == -1) return;
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
        if (message instanceof RequestVoteRequest) {
            messageLogs.add("SND->" + target + "  " +message);
            var reply = restClientService.sendRequestVote(nodeURLs.get(target),
                    (RequestVoteRequest)message);
            messageLogs.add(target + "->RCV  " + reply);
            return reply;
        } else if (message instanceof AppendEntriesRequest) {
            messageLogs.add("SND->" + target + "  " +message);
            var reply = restClientService.sendAppendEntries(nodeURLs.get(target),
                    (AppendEntriesRequest)message);
            messageLogs.add(target + "->RCV  " + reply);
            return reply;
        }
        throw new IllegalArgumentException();
    }

    @Override
    public List<RaftNode> getNodes() {
        return new ArrayList<>(Collections.singletonList(node));
    }

}
