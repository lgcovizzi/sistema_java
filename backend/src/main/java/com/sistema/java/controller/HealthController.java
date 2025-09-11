package com.sistema.java.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@CrossOrigin(origins = "*")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("application", "Sistema Java");
        response.put("timestamp", System.currentTimeMillis());
        
        // Test database connection
        try (Connection connection = dataSource.getConnection()) {
            response.put("database", "Connected");
        } catch (Exception e) {
            response.put("database", "Error: " + e.getMessage());
        }
        
        // Test Redis connection
        try {
            redisTemplate.opsForValue().set("health-check", "OK");
            String redisValue = (String) redisTemplate.opsForValue().get("health-check");
            response.put("redis", "Connected - " + redisValue);
        } catch (Exception e) {
            response.put("redis", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "Sistema Java API");
        response.put("version", "1.0.0");
        response.put("description", "API do Sistema Java com Spring Boot");
        response.put("java.version", System.getProperty("java.version"));
        response.put("spring.profiles.active", System.getProperty("spring.profiles.active"));
        
        return ResponseEntity.ok(response);
    }
}