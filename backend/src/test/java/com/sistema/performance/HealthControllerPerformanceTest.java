package com.sistema.performance;

import com.sistema.controller.HealthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

/**
 * Testes de performance para HealthController
 * Demonstra como implementar testes de performance básicos
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthController Performance Tests")
class HealthControllerPerformanceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private HealthController healthController;

    @BeforeEach
    void setUp() {
        // Setup comum para todos os testes
    }

    @Test
    @DisplayName("Health check deve responder em menos de 100ms")
    void healthCheckShouldRespondInLessThan100ms() {
        // Given
        int iterations = 100;
        long maxResponseTimeMs = 100;

        // When & Then
        for (int i = 0; i < iterations; i++) {
            Instant start = Instant.now();
            ResponseEntity<Map<String, Object>> response = healthController.health();
            Duration duration = Duration.between(start, Instant.now());

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(duration.toMillis())
                .as("Health check iteration %d should respond in less than %dms", i, maxResponseTimeMs)
                .isLessThan(maxResponseTimeMs);
        }
    }

    @Test
    @DisplayName("Info endpoint deve responder em menos de 50ms")
    void infoEndpointShouldRespondInLessThan50ms() {
        // Given
        int iterations = 50;
        long maxResponseTimeMs = 50;

        // When & Then
        for (int i = 0; i < iterations; i++) {
            Instant start = Instant.now();
            ResponseEntity<Map<String, Object>> response = healthController.info();
            Duration duration = Duration.between(start, Instant.now());

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(duration.toMillis())
                .as("Info endpoint iteration %d should respond in less than %dms", i, maxResponseTimeMs)
                .isLessThan(maxResponseTimeMs);
        }
    }

    @Test
    @DisplayName("Redis test deve responder em menos de 200ms")
    void redisTestShouldRespondInLessThan200ms() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), anyString());
        when(valueOperations.get(anyString())).thenReturn("test-value");
        int iterations = 20;
        long maxResponseTimeMs = 200;

        // When & Then
        for (int i = 0; i < iterations; i++) {
            Instant start = Instant.now();
            ResponseEntity<Map<String, Object>> response = healthController.testRedis();
            Duration duration = Duration.between(start, Instant.now());

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(duration.toMillis())
                .as("Redis test iteration %d should respond in less than %dms", i, maxResponseTimeMs)
                .isLessThan(maxResponseTimeMs);
        }
    }

    @Test
    @DisplayName("Teste de carga - múltiplas chamadas simultâneas")
    void loadTest_multipleConcurrentCalls() {
        // Given
        int numberOfThreads = 10;
        int callsPerThread = 10;
        long maxAverageResponseTimeMs = 50;

        // When
        long totalTime = 0;
        int totalCalls = numberOfThreads * callsPerThread;

        for (int thread = 0; thread < numberOfThreads; thread++) {
            for (int call = 0; call < callsPerThread; call++) {
                Instant start = Instant.now();
                ResponseEntity<Map<String, Object>> response = healthController.health();
                Duration duration = Duration.between(start, Instant.now());
                totalTime += duration.toMillis();

                assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            }
        }

        // Then
        long averageResponseTime = totalTime / totalCalls;
        assertThat(averageResponseTime)
            .as("Average response time should be less than %dms", maxAverageResponseTimeMs)
            .isLessThan(maxAverageResponseTimeMs);
    }
}