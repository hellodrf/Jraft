package com.cervidae.jraft.statemachine;

import com.cervidae.jraft.node.LogEntry;

public interface StateMachine {

    boolean apply(LogEntry entry);

    int query(String key);

    int put(String key, int value);

}
