package com.example.sistemajava.email;

import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final EmailQueue emailQueue;
    private final EmailRateLimiter rateLimiter;

    public EmailService(EmailQueue emailQueue, EmailRateLimiter rateLimiter) {
        this.emailQueue = emailQueue;
        this.rateLimiter = rateLimiter;
    }

    public boolean sendUserLimited(String userKey, String to, String subject, String text) {
        if (!rateLimiter.tryAcquirePerMinute("user:" + userKey)) {
            return false;
        }
        emailQueue.send(new EmailQueue.Job(to, subject, text));
        return true;
    }

    public boolean sendAnonymousLimited(String anonKey, String to, String subject, String text) {
        if (!rateLimiter.tryAcquirePerMinute("anon:" + anonKey)) {
            return false;
        }
        emailQueue.send(new EmailQueue.Job(to, subject, text));
        return true;
    }
}


