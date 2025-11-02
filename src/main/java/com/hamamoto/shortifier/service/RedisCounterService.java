package com.hamamoto.shortifier.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisCounterService {
    private static final String COUNTER_KEY = "shortifier:counter";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisCounterService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long getNextId() {
        var nextId = redisTemplate.opsForValue().increment(COUNTER_KEY);
        return nextId != null ? nextId : 0L;
    }
}