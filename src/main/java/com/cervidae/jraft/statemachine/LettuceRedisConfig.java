package com.cervidae.jraft.statemachine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.Serializable;

/**
 * @author AaronDu
 */
@Configuration
public class LettuceRedisConfig {

    /**
     * Redis template for storing Integer
     * This method is registered as bean, please autowire it, do not call explicitly
     * @param connectionFactory autowired
     * @return template
     */
    @Bean
    public RedisTemplate<String, Integer> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Integer> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new RedisSerializer<Integer>() {
            @Override
            public byte[] serialize(Integer value) throws SerializationException {
                if (value==null) return null;
                return new byte[] { (byte) (value >>> 24),
                        (byte) (value >>> 16), (byte) (value >>> 8),
                        (byte) ((int) value) };
            }

            @Override
            public Integer deserialize(byte[] bytes) throws SerializationException {
                if (bytes==null) return null;
                return (bytes[0] << 24)
                        + ((bytes[1] & 0xFF) << 16)
                        + ((bytes[2] & 0xFF) << 8)
                        + (bytes[3] & 0xFF);
            }
        });
        redisTemplate.setConnectionFactory(connectionFactory);
        return redisTemplate;
    }
}
