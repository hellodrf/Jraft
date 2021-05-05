package com.cervidae.jraft.node;

import com.cervidae.jraft.msg.Message;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.concurrent.TimeoutException;

public interface RaftContext {

    void start();

    void shutdown();

    int getMyID(RaftNode node);

    int newEntry(LogEntry entry);

    boolean isRunning();

    Message sendMessage(int target, Message message) throws ResourceAccessException;

    List<RaftNode> getNodes();

}
