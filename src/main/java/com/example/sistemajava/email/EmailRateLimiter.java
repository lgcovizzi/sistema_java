package com.example.sistemajava.email;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class EmailRateLimiter {

    private final StringRedisTemplate redisTemplate;

    public EmailRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryAcquirePerMinute(String key) {
        String redisKey = "email:rate:" + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(60));
        }
        return count != null && count <= 1L;
    }
}


