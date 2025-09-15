package com.sistema.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para HealthController
 * Usando Testcontainers para PostgreSQL e Redis
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "logging.level.org.springframework.web=DEBUG"
})
@DisplayName("HealthController Integration Tests")
class HealthControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_pass");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Redis configuration
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        
        // Disable mail for tests
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "25");
    }

    @Test
    @DisplayName("Deve retornar health check com status 200 e dados corretos")
    void shouldReturnHealthCheckWithStatus200AndCorrectData() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/health", Map.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("service")).isEqualTo("Sistema Java Backend");
        assertThat(response.getBody().get("version")).isEqualTo("1.0.0");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    @DisplayName("Deve retornar informações da aplicação com status 200")
    void shouldReturnApplicationInfoWithStatus200() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/info", Map.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("application")).isEqualTo("Sistema Java");
        assertThat(response.getBody().get("description")).isEqualTo("Sistema Java com Spring Boot, PostgreSQL, Redis e MailHog");
        assertThat(response.getBody()).containsKey("java_version");
        assertThat(response.getBody()).containsKey("spring_profiles");
    }

    @Test
    @DisplayName("Deve testar conectividade Redis com sucesso")
    void shouldTestRedisConnectivitySuccessfully() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/redis-test", Map.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("SUCCESS");
        assertThat(response.getBody()).containsKey("key");
        assertThat(response.getBody()).containsKey("value_stored");
        assertThat(response.getBody()).containsKey("value_retrieved");
        assertThat(response.getBody().get("redis_working")).isEqualTo(true);
    }

    @Test
    @DisplayName("Deve acessar página inicial e retornar template Thymeleaf")
    void shouldAccessHomePageAndReturnThymeleafTemplate() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/", String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("Sistema Java");
        assertThat(response.getBody()).contains("<!DOCTYPE html>");
    }

    @Test
    @DisplayName("Deve redirecionar api-simple para página inicial")
    void shouldRedirectApiSimpleToHomePage() {
        // When - Testando o redirecionamento sem seguir automaticamente
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/api-simple", 
            org.springframework.http.HttpMethod.GET, 
            null, 
            String.class);

        // Then - Deve retornar 302 (redirecionamento)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation().getPath()).isEqualTo("/");
    }
}