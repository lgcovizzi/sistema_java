package com.sistema.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Transactional
@DisplayName("Application Integration Tests")
class ApplicationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("Should start application with containers")
    void shouldStartApplicationWithContainers() {
        // Then
        assertThat(postgres.isRunning()).isTrue();
        assertThat(redis.isRunning()).isTrue();
        assertThat(port).isGreaterThan(0);
        
        // Verify Redis connection
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