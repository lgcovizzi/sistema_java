package com.sistema.integration;

import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import com.sistema.repository.RefreshTokenRepository;
import com.sistema.service.AuthService;
import com.sistema.service.JwtService;
import com.sistema.util.ClientStorageSimulator;
import com.sistema.util.CpfGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Teste de integração completo para o fluxo de login com JWT.
 * Testa todo o processo: login → armazenamento → recuperação → renovação automática
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("JWT Login Flow Integration Tests")
class JwtLoginFlowIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    // Simulador de armazenamento do cliente
    private ClientStorageSimulator clientStorage;

    // Dados estáticos para o teste
    private static final String testEmail = "jwt.login.test@example.com";
    private static final String testPassword = "JwtTestPassword123!";
    private static final String testCpf = CpfGenerator.generateCpf();
    private static final String testFirstName = "Maria";
    private static final String testLastName = "Santos";
    
    private User testUser;
    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        // Inicializar simulador de armazenamento do cliente
        clientStorage = new ClientStorageSimulator();
        
        // Configurar mock request
        mockRequest = new MockHttpServletRequest();
        mockRequest.setRemoteAddr("127.0.0.1");
        mockRequest.addHeader("User-Agent", "Test-Browser/1.0");
        
        // Buscar ou criar usuário de teste
        Optional<User> existingUser = userRepository.findByEmail(testEmail);
        if (existingUser.isPresent()) {
            testUser = existingUser.get();
        } else {
            // Criar usuário se não existir
            try {
                testUser = authService.register(testEmail, testPassword, testFirstName, testLastName, testCpf);
            } catch (Exception e) {
                // Se usuário já existe mas não foi encontrado, tentar buscar novamente
                testUser = userRepository.findByEmail(testEmail).orElse(null);
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Deve criar usuário de teste para fluxo de login")
    void shouldCreateTestUserForLoginFlow() {
        // Given - Verificar se usuário já existe
        assertThat(testUser).isNotNull();

        // Then - Verificar que usuário está configurado corretamente
        assertThat(testUser.getEmail()).isEqualTo(testEmail);
        assertThat(testUser.isEnabled()).isTrue();
        assertThat(testUser.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @Order(2)
    @DisplayName("2. Deve fazer login inicial e armazenar JWT no cliente")
    void shouldPerformInitialLoginAndStoreJwtOnClient() {
        // Given - Usuário criado e cliente sem tokens
        assertThat(testUser).isNotNull();
        assertThat(clientStorage.hasAuthTokens()).isFalse();

        // Primeiro, vamos testar se o JwtService funciona diretamente
        String directAccessToken = jwtService.generateAccessToken(testUser);
        String directRefreshToken = jwtService.generateRefreshToken(testUser);
        
        // Verificar se os tokens gerados diretamente são válidos
        assertThat(directAccessToken).isNotNull().isNotEmpty();
        assertThat(directRefreshToken).isNotNull().isNotEmpty();
        assertThat(jwtService.isValidAccessToken(directAccessToken)).isTrue();
        assertThat(jwtService.isValidRefreshToken(directRefreshToken)).isTrue();

        // When - Fazer login
        Map<String, Object> authResponse = authService.authenticate(testEmail, testPassword, mockRequest);

        // Then - Verificar resposta de autenticação
        assertThat(authResponse).isNotNull();
        assertThat(authResponse.get("accessToken")).isNotNull();
        assertThat(authResponse.get("refreshToken")).isNotNull();
        assertThat(authResponse.get("tokenType")).isEqualTo("Bearer");
        assertThat(authResponse.get("expiresIn")).isEqualTo(3600);

        // Simular armazenamento no cliente
        String accessToken = (String) authResponse.get("accessToken");
        String refreshToken = (String) authResponse.get("refreshToken");
        
        // Verificar que os tokens foram gerados
        assertThat(accessToken).isNotNull().isNotEmpty();
        assertThat(refreshToken).isNotNull().isNotEmpty();
        
        clientStorage.storeAuthTokens(accessToken, refreshToken);

        // Verificar armazenamento
        assertThat(clientStorage.hasAuthTokens()).isTrue();
        assertThat(clientStorage.getAccessToken()).isPresent();
        assertThat(clientStorage.getRefreshToken()).isPresent();
        
        // Verificar que podemos extrair informações dos tokens
        assertThat(jwtService.extractUsername(accessToken)).isEqualTo(testEmail);
        
        // Verificar que o token é válido para o usuário
        assertThat(jwtService.isTokenValid(accessToken, testUser)).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("3. Deve recuperar JWT armazenado em acesso subsequente")
    void shouldRecoverStoredJwtOnSubsequentAccess() {
        // Given - Cliente com tokens armazenados
        assertThat(clientStorage.hasAuthTokens()).isTrue();
        
        Optional<String> storedAccessToken = clientStorage.getAccessToken();
        Optional<String> storedRefreshToken = clientStorage.getRefreshToken();
        
        assertThat(storedAccessToken).isPresent();
        assertThat(storedRefreshToken).isPresent();

        // When - Simular acesso subsequente (recuperar tokens)
        String accessToken = storedAccessToken.get();
        String refreshToken = storedRefreshToken.get();

        // Then - Verificar que tokens ainda são válidos
        assertThat(jwtService.isValidAccessToken(accessToken)).isTrue();
        assertThat(jwtService.isValidRefreshToken(refreshToken)).isTrue();
        
        // Verificar que podemos extrair informações do usuário
        assertThat(jwtService.extractUsername(accessToken)).isEqualTo(testEmail);
        assertThat(jwtService.extractTokenType(accessToken)).isEqualTo("access");
        assertThat(jwtService.extractTokenType(refreshToken)).isEqualTo("refresh");
        
        // Verificar que não é necessário novo login
        assertThat(jwtService.isTokenExpired(accessToken)).isFalse();
    }

    @Test
    @Order(4)
    @DisplayName("4. Deve renovar JWT automaticamente quando expirado")
    void shouldAutomaticallyRenewExpiredJwt() {
        // Given - Simular token expirado modificando a expiração
        assertThat(clientStorage.hasAuthTokens()).isTrue();
        
        Optional<String> currentRefreshToken = clientStorage.getRefreshToken();
        assertThat(currentRefreshToken).isPresent();

        // Criar um access token com expiração muito curta para simular expiração
        // Modificar temporariamente a configuração de expiração
        long originalExpiration = (Long) ReflectionTestUtils.getField(jwtService, "accessTokenExpirationSeconds");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationSeconds", 1L); // 1 segundo
        
        try {
            // Gerar novo access token que expirará rapidamente
            String shortLivedToken = jwtService.generateAccessToken(testUser);
            clientStorage.storeToken("accessToken", shortLivedToken, "access");
            
            // Aguardar expiração
            Thread.sleep(2000); // 2 segundos para garantir expiração
            
            // Verificar que o token expirou
            assertThat(jwtService.isTokenExpired(shortLivedToken)).isTrue();
            
            // When - Simular renovação automática usando refresh token
            String refreshTokenValue = currentRefreshToken.get();
            Map<String, Object> renewalResponse = authService.refreshAccessToken(refreshTokenValue, mockRequest);
            
            // Then - Verificar renovação bem-sucedida
            assertThat(renewalResponse).isNotNull();
            assertThat(renewalResponse.get("accessToken")).isNotNull();
            assertThat(renewalResponse.get("refreshToken")).isNotNull();
            
            // Simular substituição no cliente (deletar antigo, armazenar novo)
            clientStorage.clearAuthTokens();
            String newAccessToken = (String) renewalResponse.get("accessToken");
            String newRefreshToken = (String) renewalResponse.get("refreshToken");
            clientStorage.storeAuthTokens(newAccessToken, newRefreshToken);
            
            // Verificar que novos tokens são válidos
            assertThat(jwtService.isValidAccessToken(newAccessToken)).isTrue();
            assertThat(jwtService.isValidRefreshToken(newRefreshToken)).isTrue();
            assertThat(jwtService.isTokenExpired(newAccessToken)).isFalse();
            
            // Verificar que o token antigo não é mais válido no sistema
            assertThat(jwtService.isTokenExpired(shortLivedToken)).isTrue();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Teste interrompido: " + e.getMessage());
        } finally {
            // Restaurar configuração original
            ReflectionTestUtils.setField(jwtService, "accessTokenExpirationSeconds", originalExpiration);
        }
    }

    @Test
    @Order(5)
    @DisplayName("5. Deve validar fluxo completo de persistência e renovação")
    void shouldValidateCompleteFlowOfPersistenceAndRenewal() {
        // Given - Estado final após todos os testes
        assertThat(clientStorage.hasAuthTokens()).isTrue();
        
        Optional<String> finalAccessToken = clientStorage.getAccessToken();
        Optional<String> finalRefreshToken = clientStorage.getRefreshToken();
        
        assertThat(finalAccessToken).isPresent();
        assertThat(finalRefreshToken).isPresent();

        // When - Verificar estado final dos tokens
        String accessToken = finalAccessToken.get();
        String refreshToken = finalRefreshToken.get();

        // Then - Validar que o fluxo completo funcionou
        assertThat(jwtService.isValidAccessToken(accessToken)).isTrue();
        assertThat(jwtService.isValidRefreshToken(refreshToken)).isTrue();
        assertThat(jwtService.extractUsername(accessToken)).isEqualTo(testEmail);
        
        // Verificar que o usuário pode ser autenticado com os tokens finais
        assertThat(jwtService.isTokenValid(accessToken, testUser)).isTrue();
        
        // Verificar metadados do armazenamento
        Optional<ClientStorageSimulator.StoredToken> storedAccessToken = clientStorage.getToken("accessToken");
        Optional<ClientStorageSimulator.StoredToken> storedRefreshToken = clientStorage.getToken("refreshToken");
        
        assertThat(storedAccessToken).isPresent();
        assertThat(storedRefreshToken).isPresent();
        assertThat(storedAccessToken.get().getTokenType()).isEqualTo("access");
        assertThat(storedRefreshToken.get().getTokenType()).isEqualTo("refresh");
        assertThat(storedAccessToken.get().getStoredAt()).isNotNull();
        assertThat(storedRefreshToken.get().getStoredAt()).isNotNull();
    }

    @Test
    @Order(6)
    @DisplayName("6. Deve limpar dados de teste após validação completa")
    void shouldCleanupTestDataAfterCompleteValidation() {
        // Given - Dados de teste existentes
        assertThat(testUser).isNotNull();
        assertThat(clientStorage.hasAuthTokens()).isTrue();

        // When - Limpar armazenamento do cliente
        clientStorage.clear();

        // Then - Verificar limpeza
        assertThat(clientStorage.hasAuthTokens()).isFalse();
        assertThat(clientStorage.size()).isEqualTo(0);

        // Limpar dados do banco (refresh tokens associados ao usuário)
        if (testUser != null) {
            refreshTokenRepository.deleteAll(refreshTokenRepository.findByUser(testUser));
            userRepository.delete(testUser);
        }

        // Verificar que usuário foi removido
        Optional<User> deletedUser = userRepository.findByEmail(testEmail);
        assertThat(deletedUser).isEmpty();
    }
}