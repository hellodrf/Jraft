package com.cervidae.jraft.statemachine;

import com.cervidae.jraft.restful.BankAccount;
import com.cervidae.jraft.node.LogEntry;
import com.cervidae.jraft.restful.Response;

public interface StateMachine {

    Response<BankAccount> apply(LogEntry entry);

    int query(String key);

    int put(String key, int value);

}
