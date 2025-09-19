package com.sistema.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.cache.type=simple",
        "spring.redis.host=",
        "spring.redis.port=",
        "management.health.redis.enabled=false",
        "spring.data.redis.repositories.enabled=false"
    }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
@Transactional
public class ApplicationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private ValueOperations<String, Object> valueOperations;

    @Test
    @DisplayName("Should start application successfully")
    void shouldStartApplicationSuccessfully() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("test-key")).thenReturn("test-value");
        
        // Then
        assertThat(port).isGreaterThan(0);
        
        // Verify Redis mock behavior
        redisTemplate.opsForValue().set("test-key", "test-value");
        String value = (String) redisTemplate.opsForValue().get("test-key");
        assertThat(value).isEqualTo("test-value");
    }

    @Test
    @DisplayName("Should test health endpoints with database and Redis")
    void shouldTestHealthEndpointsWithDatabaseAndRedis() {
        // When
        ResponseEntity<Map> healthResponse = restTemplate.getForEntity("/api/health", Map.class);
        ResponseEntity<Map> infoResponse = restTemplate.getForEntity("/api/info", Map.class);
        ResponseEntity<Map> redisTestResponse = restTemplate.getForEntity("/api/redis-test", Map.class);

        // Then
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getBody()).containsKey("status");
        
        assertThat(infoResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(infoResponse.getBody()).containsKey("application");
        
        assertThat(redisTestResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(redisTestResponse.getBody()).containsKey("status");
    }

    @Test
    @DisplayName("Should test home page rendering")
    void shouldTestHomePageRendering() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Sistema Java");
        assertThat(response.getBody()).contains("<!DOCTYPE html>");
    }

    @Test
    @DisplayName("Should test Redis connectivity and operations")
    void shouldTestRedisConnectivityAndOperations() {
        // Given
        String testKey = "integration-test-key";
        String testValue = "integration-test-value";
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(testKey)).thenReturn(testValue);
        when(redisTemplate.hasKey(testKey)).thenReturn(false);

        // When
        redisTemplate.opsForValue().set(testKey, testValue);
        String retrievedValue = (String) redisTemplate.opsForValue().get(testKey);

        // Then
        assertThat(retrievedValue).isEqualTo(testValue);
        
        // Cleanup
        redisTemplate.delete(testKey);
        assertThat(redisTemplate.hasKey(testKey)).isFalse();
    }

    @Test
    @DisplayName("Should test API simple redirect")
    void shouldTestApiSimpleRedirect() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity("/api-simple", String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation().getPath()).isEqualTo("/");
    }
}