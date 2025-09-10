package com.example.sistemajava.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailQueue {
    private static final Logger log = LoggerFactory.getLogger(EmailQueue.class);

    private final JavaMailSender mailSender;

    public EmailQueue(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public record Job(String to, String subject, String text) {}

    @Async("avatarExecutor")
    public void send(Job job) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(job.to());
            message.setSubject(job.subject());
            message.setText(job.text());
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Falha ao enviar email para {}: {}", job.to(), e.getMessage());
        }
    }
}


