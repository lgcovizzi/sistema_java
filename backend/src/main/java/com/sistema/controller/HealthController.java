package com.sistema.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HealthController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "Sistema Java Backend");
        response.put("version", "1.0.0");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/redis-test")
    public ResponseEntity<Map<String, Object>> testRedis() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String key = "test:" + System.currentTimeMillis();
            String value = "Redis funcionando em " + LocalDateTime.now();
            
            // Salvar no Redis
            redisTemplate.opsForValue().set(key, value);
            
            // Recuperar do Redis
            String retrievedValue = (String) redisTemplate.opsForValue().get(key);
            
            response.put("status", "SUCCESS");
            response.put("key", key);
            response.put("value_stored", value);
            response.put("value_retrieved", retrievedValue);
            response.put("redis_working", value.equals(retrievedValue));
            
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", "Sistema Java");
        response.put("description", "Sistema Java com Spring Boot, PostgreSQL, Redis e MailHog");
        response.put("java_version", System.getProperty("java.version"));
        response.put("spring_profiles", System.getProperty("spring.profiles.active", "default"));
        
        return ResponseEntity.ok(response);
    }

}