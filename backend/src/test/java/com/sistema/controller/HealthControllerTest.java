package com.sistema.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HealthController Tests")
class HealthControllerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private HealthController healthController;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should return health status successfully")
    void health_ShouldReturnHealthStatus() {
        // Given
        LocalDateTime beforeCall = LocalDateTime.now();

        // When
        ResponseEntity<Map<String, Object>> response = healthController.health();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        Map<String, Object> body = response.getBody();
        assertThat(body.get("status")).isEqualTo("UP");
        assertThat(body.get("service")).isEqualTo("Sistema Java Backend");
        assertThat(body.get("version")).isEqualTo("1.0.0");
        assertThat(body.get("timestamp")).isInstanceOf(LocalDateTime.class);
        
        LocalDateTime timestamp = (LocalDateTime) body.get("timestamp");
        assertThat(timestamp).isAfterOrEqualTo(beforeCall);
    }

    @Test
    @DisplayName("Should test Redis connectivity successfully")
    void testRedis_Success() {
        // Given - Mock to capture and return the stored value
        Map<String, String> redisStorage = new HashMap<>();
        
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            redisStorage.put(key, value);
            return null;
        }).when(valueOperations).set(anyString(), anyString());
        
        when(valueOperations.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return redisStorage.get(key);
        });

        // When
        ResponseEntity<Map<String, Object>> response = healthController.testRedis();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        Map<String, Object> body = response.getBody();
        assertThat(body.get("status")).isEqualTo("SUCCESS");
        assertThat(body.get("key")).isNotNull();
        assertThat(body.get("value_stored")).isNotNull();
        assertThat(body.get("value_retrieved")).isNotNull();
        assertThat(body.get("redis_working")).isEqualTo(true);
        
        verify(valueOperations).set(anyString(), anyString());
        verify(valueOperations).get(anyString());
    }

    @Test
    @DisplayName("Should handle Redis error gracefully")
    void testRedis_Error() {
        // Given
        doThrow(new RuntimeException("Redis connection failed"))
                .when(valueOperations).set(anyString(), anyString());

        // When
        ResponseEntity<Map<String, Object>> response = healthController.testRedis();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        Map<String, Object> body = response.getBody();
        assertThat(body.get("status")).isEqualTo("ERROR");
        assertThat(body.get("error")).isEqualTo("Redis connection failed");
    }

    @Test
    @DisplayName("Should return application info successfully")
    void info_ShouldReturnApplicationInfo() {
        // When
        ResponseEntity<Map<String, Object>> response = healthController.info();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        Map<String, Object> body = response.getBody();
        assertThat(body.get("application")).isEqualTo("Sistema Java");
        assertThat(body.get("description")).isEqualTo("Sistema Java com Spring Boot, PostgreSQL, Redis e MailHog");
        assertThat(body.get("java_version")).isNotNull();
        assertThat(body.get("spring_profiles")).isNotNull();
    }

    @Test
    @DisplayName("Should handle Redis value mismatch")
    void testRedis_ValueMismatch() {
        // Given
        String storedValue = "Original value";
        String retrievedValue = "Different value";
        when(valueOperations.get(anyString())).thenReturn(retrievedValue);

        // When
        ResponseEntity<Map<String, Object>> response = healthController.testRedis();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        Map<String, Object> body = response.getBody();
        assertThat(body.get("status")).isEqualTo("SUCCESS");
        assertThat(body.get("redis_working")).isEqualTo(false);
    }

    @Test
    @DisplayName("Should handle null value from Redis")
    void testRedis_NullValue() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        ResponseEntity<Map<String, Object>> response = healthController.testRedis();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        Map<String, Object> body = response.getBody();
        assertThat(body.get("status")).isEqualTo("SUCCESS");
        assertThat(body.get("value_retrieved")).isNull();
        assertThat(body.get("redis_working")).isEqualTo(false);
    }

    @Test
    @DisplayName("Should verify Redis operations are called correctly")
    void testRedis_VerifyOperations() {
        // Given
        String testValue = "Test value";
        when(valueOperations.get(anyString())).thenReturn(testValue);

        // When
        healthController.testRedis();

        // Then
        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOperations).set(startsWith("test:"), contains("Redis funcionando em"));
        verify(valueOperations).get(startsWith("test:"));
    }
}