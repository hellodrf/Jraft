package com.cervidae.jraft.node;

import com.cervidae.jraft.async.ArgRunnable;
import com.cervidae.jraft.msg.Message;

import java.util.concurrent.TimeoutException;

public interface RaftContext {

    void start();

    void shutdown();

    int getMyID(RaftNode node);

    int newEntry(LogEntry entry);

    boolean isRunning();

    Message sendMessage(int target, Message message) throws TimeoutException;

    void blockingBroadcast(Message message, ArgRunnable<Message> callback);

    void asyncBroadcast(Message message, ArgRunnable<Message> callback);

}
