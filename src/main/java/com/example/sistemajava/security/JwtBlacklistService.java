package com.example.sistemajava.security;

import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class JwtBlacklistService {
    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    public JwtBlacklistService(StringRedisTemplate redisTemplate, JwtService jwtService) {
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
    }

    public void blacklist(String token) {
        Claims claims = jwtService.parseToken(token);
        long ttlMs = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (ttlMs <= 0) ttlMs = 1_000;
        redisTemplate.opsForValue().set(blacklistKey(token), "1", Duration.ofMillis(ttlMs));
    }

    public boolean isBlacklisted(String token) {
        Boolean exists = redisTemplate.hasKey(blacklistKey(token));
        return exists != null && exists;
    }

    private String blacklistKey(String token) {
        return "jwt:blacklist:" + token;
    }
}


