package com.sistema.e2e.scenarios;

import com.sistema.e2e.base.BaseE2ETest;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste E2E simples para verificar se a aplicação está funcionando.
 */
@TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
class SimpleE2ETest extends BaseE2ETest {
    
    @BeforeEach
    void setUp() {
        cleanupTestData();
    }
    
    @AfterEach
    void tearDown() {
        cleanupTestData();
    }
    
    @Test
    @Order(1)
    @DisplayName("E2E: Deve retornar health check com status UP")
    void shouldReturnHealthCheckWithStatusUp() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
            getUrl("/api/health"), Map.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("status");
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }
    
    @Test
    @Order(2)
    @DisplayName("E2E: Deve carregar página inicial")
    void shouldLoadHomePage() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            getUrl("/"), String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Sistema Java");
    }
}
