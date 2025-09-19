package com.sistema.e2e.utils;

import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Helper para testes End-to-End (E2E).
 */
@Component
public class E2ETestHelper {
    
    @Autowired(required = false)
    private TestRestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private String baseUrl;
    
    /**
     * Define a URL base da aplicação.
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    /**
     * Cria um usuário de teste personalizado.
     */
    public User createTestUser(String email, String password, UserRole role, 
                              boolean emailVerified, boolean enabled) {
        
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEmailVerified(emailVerified);
        user.setEnabled(enabled);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setCpf("12345678901"); // CPF simples para testes
        
        // Definir nome baseado no email
        String[] emailParts = email.split("@");
        user.setFirstName(emailParts[0].substring(0, 1).toUpperCase() + emailParts[0].substring(1));
        user.setLastName("Teste E2E");
        
        // Se não verificado, gerar token de verificação
        if (!emailVerified) {
            user.setVerificationToken("test-verification-token-" + System.currentTimeMillis());
            user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(1));
        }
        
        return userRepository.save(user);
    }
    
    /**
     * Cria um usuário de teste com configurações padrão.
     */
    public User createTestUser(String email, String password) {
        return createTestUser(email, password, UserRole.USER, true, true);
    }
    
    /**
     * Limpa todos os dados de teste.
     */
    public void cleanupTestData() {
        userRepository.deleteAll();
    }
    
    /**
     * Conta o número de usuários no banco de dados.
     */
    public long countUsers() {
        return userRepository.count();
    }
    
    /**
     * Verifica se um usuário existe no banco de dados.
     */
    public boolean userExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}
