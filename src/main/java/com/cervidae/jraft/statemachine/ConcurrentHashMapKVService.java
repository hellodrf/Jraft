package com.cervidae.jraft.statemachine;

import com.cervidae.jraft.bank.BankAccount;
import com.cervidae.jraft.node.LogEntry;
import com.cervidae.jraft.restful.Response;
import lombok.Data;
import lombok.extern.java.Log;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Service
@Primary
@Scope("prototype")
public class ConcurrentHashMapKVService implements StateMachine {

    Map<String, Integer> storage;

    public ConcurrentHashMapKVService() {
        this.storage = new ConcurrentHashMap<>();
    }

    public void applyAll(List<LogEntry> entries) {
        for (LogEntry entry : entries) {
            this.apply(entry);
        }
    }

    public Response<BankAccount> apply(LogEntry entry) {
        String[] processedEntry = processEntry(entry);
        if (processedEntry == null) {
            return Response.fail();
        }
        String accountID, action;
        action = processedEntry[0];
        accountID = processedEntry[1];

        if (action.equals("DEPOSIT") || action.equals("WITHDRAW")) {
            if (processedEntry.length != 3) {
                return Response.fail();
            }
            Integer amount;
            try {
                amount = Integer.parseInt(processedEntry[2]);
            } catch (NumberFormatException e) {
                return Response.fail();
            }

            if (action.equals("DEPOSIT")){
                return applyDeposit(accountID, amount);
            } else {
                return applyWithdraw(accountID, amount);
            }
        } else if (action.equals("BALANCE")) {
            return applyCheckBalance(accountID);
        } else if (action.equals("CREATE")) {
            return applyCreateAccount(accountID);
        }
        return Response.fail();
    }

    private String[] processEntry(LogEntry entry) {
        String command = entry.getCommand();
        String[] processedEntry = command.split(";");

        // Need to test node crashes too. Stop it - do request before and after stopping node. (Both followers and leaders). Restart as well to see if it gets all logs.
        /*if (processedEntry.length != 3) {
            if((!processedEntry[1].equals("BALANCE") && !processedEntry[1].equals("CREATE")) || processedEntry.length != 2) {
                return null;
            }
        }*/
        return processedEntry;
    }

    public Response<BankAccount> applyCreateAccount(String accountID) {

        if(storage.containsKey(accountID)) {
            return Response.fail();
        }

        storage.put(accountID, 0);

        return Response.success(new BankAccount(accountID, storage.get(accountID)));
    }

    public Response<BankAccount> applyDeposit(String accountID, Integer amount) {
        int currentValue = 0;
        if (storage.containsKey(accountID)) {
            currentValue = storage.get(accountID);
        }
        storage.put(accountID, currentValue + amount);

        return Response.success(new BankAccount(accountID, storage.get(accountID)));
    }

    public Response<BankAccount> applyWithdraw(String accountID, Integer amount) {
        if (!storage.containsKey(accountID) || storage.get(accountID) < amount) {
            return Response.fail();
        }
        int currentValue = storage.get(accountID);
        storage.put(accountID, currentValue - amount);

        return Response.success(new BankAccount(accountID, storage.get(accountID)));
    }

    /**
     * CAN BE REMOVED.
     * @param accountID
     * @return
     */
    public Response<BankAccount> applyCheckBalance(String accountID) {

        if (!storage.containsKey(accountID)) {
            return Response.fail();
        }

        return Response.success(new BankAccount(accountID, storage.get(accountID)));
    }

    @Override
    public int query(String key) {
        if (!storage.containsKey(key)) {
            throw new IllegalArgumentException();
        }
        var val = storage.get(key);

        if (val != null) {
            return val;
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public int put(String key, int value) {
        storage.put(key, value);
        return storage.getOrDefault(key, 0);
    }
}
