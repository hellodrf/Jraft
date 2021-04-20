package com.cervidae.jraft.node;

import lombok.Data;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Service
@Scope("prototype")
public class KVDatabase implements StateMachine {

    Map<String, String> storage;

    public KVDatabase() {
        this.storage = new ConcurrentHashMap<>();
    }

    @Override
    public boolean apply(LogEntry entry) {
        return false;
    }
}
