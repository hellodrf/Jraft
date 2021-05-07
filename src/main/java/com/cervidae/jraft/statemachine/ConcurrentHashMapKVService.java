package com.cervidae.jraft.statemachine;

import com.cervidae.jraft.node.LogEntry;
import lombok.Data;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

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

    public boolean apply(LogEntry entry) {
        return true;
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
}
