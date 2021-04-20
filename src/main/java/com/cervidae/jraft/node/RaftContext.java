package com.cervidae.jraft.node;

import com.cervidae.jraft.msg.Message;

public interface RaftContext {

    int addEntry(LogEntry entry);

    boolean shutdown();

    Message sendMessage(int target, Message message);

    Message[] blockingBroadcast(Message message);

    Message[] asyncBroadcast(Message message);

    boolean apply(LogEntry entry);

}
