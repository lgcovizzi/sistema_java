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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

/**
 * Testes unitários para HealthController
 * Seguindo práticas de TDD com padrão Given-When-Then
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthController Tests")
class HealthControllerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private HealthController healthController;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @BeforeEach
    void setUp() {
        // Setup comum para todos os testes
    }

    @Test
    @DisplayName("Deve retornar status UP quando health check é chamado")
    void shouldReturnStatusUpWhenHealthCheckIsCalled() {
        // When
        ResponseEntity<Map<String, Object>> response = healthController.health();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("service")).isEqualTo("Sistema Java Backend");
        assertThat(response.getBody().get("version")).isEqualTo("1.0.0");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Deve retornar informações da aplicação quando info é chamado")
    void shouldReturnApplicationInfoWhenInfoIsCalled() {
        // When
        ResponseEntity<Map<String, Object>> response = healthController.info();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("application")).isEqualTo("Sistema Java");
        assertThat(response.getBody().get("description")).isEqualTo("Sistema Java com Spring Boot, PostgreSQL, Redis e MailHog");
        assertThat(response.getBody()).containsKey("java_version");
        assertThat(response.getBody()).containsKey("spring_profiles");
    }

    @Test
    @DisplayName("Deve retornar sucesso quando Redis está funcionando")
    void shouldReturnSuccessWhenRedisIsWorking() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String testValue = "Redis funcionando em ";
        doNothing().when(valueOperations).set(anyString(), anyString());
        when(valueOperations.get(anyString())).thenReturn(testValue + "2024-01-01T10:00:00");

        // When
        ResponseEntity<Map<String, Object>> response = healthController.testRedis();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("SUCCESS");
        assertThat(response.getBody()).containsKey("key");
        assertThat(response.getBody()).containsKey("value_stored");
        assertThat(response.getBody()).containsKey("value_retrieved");
    }

    @Test
    @DisplayName("Deve retornar erro quando Redis falha")
    void shouldReturnErrorWhenRedisFails() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis connection failed")).when(valueOperations).set(anyString(), anyString());

        // When
        ResponseEntity<Map<String, Object>> response = healthController.testRedis();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("ERROR");
        assertThat(response.getBody().get("error")).isEqualTo("Redis connection failed");
    }
}