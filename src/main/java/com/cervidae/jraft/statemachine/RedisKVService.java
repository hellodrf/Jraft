package com.cervidae.jraft.statemachine;

import com.cervidae.jraft.node.LogEntry;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Data
@Primary
@Service
public class RedisKVService implements StateMachine {

    private final RedisTemplate<String, Integer> redisTemplate;

    @Autowired
    public RedisKVService(RedisTemplate<String, Integer> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean apply(LogEntry entry) {
        return false;
    }
}