package com.example.sistemajava.email;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EmailServiceTest {
    @Test
    void rate_limit_user_allows_first_blocks_second() {
        EmailQueue queue = mock(EmailQueue.class);
        EmailRateLimiter limiter = mock(EmailRateLimiter.class);
        when(limiter.tryAcquirePerMinute("user:user@ex.com")).thenReturn(true, false);
        EmailService svc = new EmailService(queue, limiter);

        assertTrue(svc.sendUserLimited("user@ex.com", "user@ex.com", "s", "t"));
        assertFalse(svc.sendUserLimited("user@ex.com", "user@ex.com", "s", "t"));
        verify(queue, times(1)).send(any());
    }

    @Test
    void rate_limit_anon_allows_first_blocks_second() {
        EmailQueue queue = mock(EmailQueue.class);
        EmailRateLimiter limiter = mock(EmailRateLimiter.class);
        when(limiter.tryAcquirePerMinute("anon:123"))
                .thenReturn(true, false);
        EmailService svc = new EmailService(queue, limiter);

        assertTrue(svc.sendAnonymousLimited("123", "user@ex.com", "s", "t"));
        assertFalse(svc.sendAnonymousLimited("123", "user@ex.com", "s", "t"));
        verify(queue, times(1)).send(any());
    }
}


