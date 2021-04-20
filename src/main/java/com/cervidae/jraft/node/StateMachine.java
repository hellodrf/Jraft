package com.cervidae.jraft.node;

public interface StateMachine {

    boolean apply(LogEntry entry);

}
