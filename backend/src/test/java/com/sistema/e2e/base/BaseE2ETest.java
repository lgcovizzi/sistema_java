package com.sistema.e2e.base;

import com.sistema.entity.User;
import com.sistema.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Classe base para todos os testes End-to-End (E2E).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@Transactional
public abstract class BaseE2ETest {
    
    @LocalServerPort
    protected int port;
    
    @Autowired
    protected TestRestTemplate restTemplate;
    
    @Autowired
    protected UserRepository userRepository;
    
    @Autowired
    protected RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Obtém a URL base da aplicação para os testes.
     */
    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }
    
    /**
     * Obtém a URL completa de um endpoint.
     */
    protected String getUrl(String endpoint) {
        return getBaseUrl() + endpoint;
    }
    
    /**
     * Limpa todos os dados de teste.
     */
    protected void cleanupTestData() {
        // Limpar banco de dados
        userRepository.deleteAll();
        
        // Limpar cache Redis
        if (redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
        }
    }
    
    /**
     * Verifica se a aplicação está funcionando corretamente.
     */
    protected boolean isApplicationHealthy() {
        try {
            var response = restTemplate.getForEntity(getUrl("/api/health"), Map.class);
            return response.getStatusCode().is2xxSuccessful() && 
                   "UP".equals(response.getBody().get("status"));
        } catch (Exception e) {
            return false;
        }
    }
}
