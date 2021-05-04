package com.cervidae.jraft.statemachine;

import com.cervidae.jraft.node.LogEntry;
import lombok.Data;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Service
@Scope("prototype")
public class ConcurrentHashMapKVService implements StateMachine {

    Map<String, Integer> storage;

    public ConcurrentHashMapKVService() {
        this.storage = new ConcurrentHashMap<>();
    }

    public String[] processEntry(LogEntry entry) {
        String command = entry.getCommand();
        String[] processedEntry = command.split(":");
        if (processedEntry.length != 3) {
            if(processedEntry[1] != "balance" || processedEntry.length != 2) {
                return null;
            }
        }
        return processedEntry;
    }

    public boolean apply(LogEntry entry) {
        String[] processedEntry = processEntry(entry);
        if (processedEntry == null) {
            return false;
        }
        String accountID, action;
        accountID = processedEntry[0];
        action = processedEntry[1];

        if (action.equals("deposit") || action.equals("withdraw")) {
            if (processedEntry.length != 3) {
                return false;
            }
            Integer amount;
            try {
                amount = Integer.parseInt(processedEntry[2]);
            } catch (NumberFormatException e) {
                return false;
            }

            if (action.equals("deposit")){
                applyDeposit(accountID, amount);
            } else {
                applyWithdraw(accountID, amount);
            }
        } else if (action.equals("checkBalance")) {
            applyCheckBalance(accountID);
        }


        return true;
    }

    public boolean applyDeposit(String accountID, Integer amount) {
        int currentValue = 0;
        if (storage.containsKey(accountID)) {
            currentValue = storage.get(accountID);
        }
        storage.put(accountID, currentValue + amount);
        return true;
    }

    public boolean applyWithdraw(String accountID, Integer amount) {
        if (!storage.containsKey(accountID) || storage.get(accountID) < amount) {
            return false;
        }
        int currentValue = storage.get(accountID);
        storage.put(accountID, currentValue - amount);
        return true;
    }

    public int applyCheckBalance(String accountID) {
        int currentValue = 0;
        if (storage.containsKey(accountID)) {
            currentValue = storage.get(accountID);
        }
        return currentValue;
    }


}
