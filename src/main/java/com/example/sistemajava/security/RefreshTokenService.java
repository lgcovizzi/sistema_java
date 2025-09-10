package com.example.sistemajava.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final StringRedisTemplate redisTemplate;

    @Value("${app.security.jwt.refreshExpiration:2592000000}") // 30 dias ms
    private long refreshExpirationMs;

    public RefreshTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String issue(String username) {
        String token = UUID.randomUUID().toString();
        String key = key(username, token);
        redisTemplate.opsForValue().set(key, "1", Duration.ofMillis(refreshExpirationMs));
        return token;
    }

    public boolean validate(String username, String token) {
        String key = key(username, token);
        Boolean exists = redisTemplate.hasKey(key);
        return exists != null && exists;
    }

    public void revoke(String username, String token) {
        redisTemplate.delete(key(username, token));
    }

    public void revokeAll(String username) {
        // simple impl: namespace per user not easily deletable without scan; for demo we'll rely on per-token revoke
    }

    private String key(String username, String token) {
        return "refresh:" + username + ":" + token;
    }
}


