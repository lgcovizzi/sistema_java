package com.example.sistemajava.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JwtBlacklistServiceTest {

    @Test
    void blacklist_and_check() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(redis.hasKey(anyString())).thenReturn(true);

        JwtService jwtService = mock(JwtService.class);
        Claims claims = mock(Claims.class);
        when(jwtService.parseToken("t")).thenReturn(claims);
        when(claims.getExpiration()).thenReturn(new java.util.Date(System.currentTimeMillis() + 10000));

        JwtBlacklistService s = new JwtBlacklistService(redis, jwtService);
        s.blacklist("t");

        assertTrue(s.isBlacklisted("t"));
        verify(redis, times(1)).opsForValue();
    }
}


