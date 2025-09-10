package com.example.sistemajava.security;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RefreshTokenServiceTest {
    @Test
    void issue_and_validate() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(redis.hasKey(anyString())).thenReturn(true);
        RefreshTokenService s = new RefreshTokenService(redis);
        String token = s.issue("user@ex.com");
        assertNotNull(token);
        assertTrue(s.validate("user@ex.com", token));
    }
}


