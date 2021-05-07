package com.cervidae.jraft.statemachine;

import com.cervidae.jraft.bank.BankAccount;
import com.cervidae.jraft.node.LogEntry;
import com.cervidae.jraft.restful.Response;

public interface StateMachine {

    Response<BankAccount> apply(LogEntry entry);

}
