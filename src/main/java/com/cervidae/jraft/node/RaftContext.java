package com.cervidae.jraft.node;

import com.cervidae.jraft.bank.BankAccount;
import com.cervidae.jraft.msg.Message;
import com.cervidae.jraft.restful.Response;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

public interface RaftContext {

    void start();

    void shutdown();

    int getMyID(RaftNode node);

    int newEntry(String cmd);

    boolean isRunning();

    Message sendMessage(int target, Message message) throws ResourceAccessException;

    List<RaftNode> getNodes();

    List<String> getMessageLogs();

}
