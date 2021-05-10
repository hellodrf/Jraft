package com.cervidae.jraft.node;

import com.cervidae.jraft.async.AsyncService;
import com.cervidae.jraft.bank.BankAccount;
import com.cervidae.jraft.msg.AppendEntriesRequest;
import com.cervidae.jraft.msg.Message;
import com.cervidae.jraft.msg.RequestVoteRequest;
import com.cervidae.jraft.restful.Response;
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
    String monitorUrl;
    boolean running;

    /**
     * External Services
     */
    final AsyncService asyncService;
    final RestClientService restClientService;


    List<String> messageLogs = new ArrayList<>(1000);

    public ClusteredRaftContext(AsyncService asyncService, RaftConfig config, RestClientService restClientService) {
        ClusteredRaftContext.log.info("ClusteredRaftContext created and starting");
        this.restClientService = restClientService;
        this.id = config.getClusteredId();
        this.asyncService = asyncService;
        this.nodeURLs = config.getClusteredUrls();
        this.clusterSize = config.getClusterSize();
        this.monitorUrl = config.getMonitorUrl();
        this.running = false;
        if (this.id != -1) {
            this.node = config.getApplicationContext().getBean(RaftNode.class);
        }
    }

    @Override
    public int newEntry(String cmd) {
        return node.newEntry(cmd);
    }

    @Override
    public void start() {
        if (this.id == -1) return;
        this.running = true;
        this.node.start();
        this.notifyMonitor("N"+id +" - ONLINE");
    }

    @Override
    public void shutdown() {
        this.running = false;
        if (this.node != null) {
            this.node.shutdown();
        }
        this.notifyMonitor("N"+id +" - OFFLINE");
    }

    @Override
    public int getMyID(RaftNode node) {
        return id;
    }

    @Override
    public Message sendMessage(int target, Message message) {
        if (message instanceof RequestVoteRequest) {
            try {
                var reply = restClientService.sendRequestVote(nodeURLs.get(target),
                        (RequestVoteRequest)message);
                notifyMonitor("N" + message.getSource() + " -> N" + target + " : " +message);
                notifyMonitor("N" + message.getSource() + " <- N" + target + " : " +reply);
                return reply;
            } catch (Exception e) {
                notifyMonitor("N" + message.getSource() + " XX N" + target + " : " +message);
                throw e;
            }
        } else if (message instanceof AppendEntriesRequest) {

            try {
                var reply = restClientService.sendAppendEntries(nodeURLs.get(target),
                        (AppendEntriesRequest)message);
                if (((AppendEntriesRequest) message).getEntries() != null) {
                    notifyMonitor("N" + message.getSource() + " -> N" + target + " : " +message);
                    notifyMonitor("N" + message.getSource() + " <- N" + target + " : " +reply);
                }
                return reply;
            } catch (Exception e) {
                if (((AppendEntriesRequest) message).getEntries() != null) {
                    notifyMonitor("N" + message.getSource() + " XX N" + target + " : " +message);
                }
                throw e;
            }

        }
        throw new IllegalArgumentException();
    }

    protected void notifyMonitor(String msg) {
        try {
            restClientService.post(monitorUrl + "/mon/event", msg);
        } catch (Exception ignored) {}

    }

    @Override
    public List<RaftNode> getNodes() {
        return new ArrayList<>(Collections.singletonList(node));
    }

}
