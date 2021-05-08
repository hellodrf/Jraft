package com.cervidae.jraft.statemachine;

import com.cervidae.jraft.node.LogEntry;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Objects;

@Data
@Primary
@Service
public class RedisKVService implements StateMachine {

    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public RedisKVService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().flushDb();
    }

    @Override
    public boolean apply(LogEntry entry) {
        return false;
    }

    @Override
    public int query(String key) {
        var val = redisTemplate.opsForValue().get(key);
        Assert.notNull(val, "value does not exist");
        return Integer.parseInt(val);
    }

    @Override
    public int put(String key, int value) {
        redisTemplate.opsForValue().set(key, Integer.toString(value));
        return query(key);
    }
}
