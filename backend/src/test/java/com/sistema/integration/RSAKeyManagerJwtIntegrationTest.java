package com.sistema.integration;

import com.sistema.config.RSAKeyManager;
import com.sistema.entity.Role;
import com.sistema.entity.User;
import com.sistema.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integração entre RSAKeyManager e JwtService
 * Verifica se as chaves RSA geradas na inicialização do servidor
 * funcionam corretamente com o sistema JWT
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RSAKeyManager e JwtService Integration Tests")
class RSAKeyManagerJwtIntegrationTest {

    private RSAKeyManager rsaKeyManager;
    private JwtService jwtService;
    private User testUser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Configurar RSAKeyManager
        rsaKeyManager = new RSAKeyManager();
        ReflectionTestUtils.setField(rsaKeyManager, "keysDirectory", tempDir.toString());
        
        // Inicializar chaves (simula @PostConstruct na inicialização do servidor)
        rsaKeyManager.initializeKeys();
        
        // Configurar JwtService com RSAKeyManager
        jwtService = new JwtService(rsaKeyManager);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationSeconds", 3600L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationSeconds", 15552000L);
        ReflectionTestUtils.setField(jwtService, "issuer", "sistema-java-test");
        
        // Criar usuário de teste
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPassword("password");
        testUser.setRoles(List.of(Role.USER));
    }

    @Test
    @DisplayName("Deve gerar e validar token JWT usando chaves RSA da inicialização do servidor")
    void shouldGenerateAndValidateJwtTokenUsingRsaKeysFromServerInitialization() {
        // Given - Verificar que as chaves foram inicializadas
        PrivateKey privateKey = rsaKeyManager.getPrivateKey();
        PublicKey publicKey = rsaKeyManager.getPublicKey();
        
        assertThat(privateKey).isNotNull();
        assertThat(publicKey).isNotNull();
        assertThat(rsaKeyManager.validateKeyPair(privateKey, publicKey)).isTrue();

        // When - Gerar token JWT usando as chaves RSA
        String accessToken = jwtService.generateAccessToken(testUser);
        String refreshToken = jwtService.generateRefreshToken(testUser);

        // Then - Verificar que os tokens foram gerados
        assertThat(accessToken).isNotNull().isNotEmpty();
        assertThat(refreshToken).isNotNull().isNotEmpty();
        
        // Verificar que os tokens são válidos
        assertThat(jwtService.isValidAccessToken(accessToken)).isTrue();
        assertThat(jwtService.isValidRefreshToken(refreshToken)).isTrue();
        
        // Verificar que podemos extrair informações dos tokens
        assertThat(jwtService.extractUsername(accessToken)).isEqualTo("testuser");
        assertThat(jwtService.extractUsername(refreshToken)).isEqualTo("testuser");
        assertThat(jwtService.extractTokenType(accessToken)).isEqualTo("access");
        assertThat(jwtService.extractTokenType(refreshToken)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("Deve validar token JWT com usuário usando chaves RSA")
    void shouldValidateJwtTokenWithUserUsingRsaKeys() {
        // Given
        String accessToken = jwtService.generateAccessToken(testUser);
        
        // When & Then
        assertThat(jwtService.isTokenValid(accessToken, testUser)).isTrue();
        assertThat(jwtService.isTokenExpired(accessToken)).isFalse();
    }

    @Test
    @DisplayName("Deve extrair claims do token JWT usando chaves RSA")
    void shouldExtractClaimsFromJwtTokenUsingRsaKeys() {
        // Given
        String accessToken = jwtService.generateAccessToken(testUser);
        
        // When & Then
        assertThat(jwtService.extractUsername(accessToken)).isEqualTo("testuser");
        assertThat(jwtService.extractRoles(accessToken)).contains("ROLE_USER");
        assertThat(jwtService.extractExpiration(accessToken)).isNotNull();
        
        // Verificar informações do token
        var tokenInfo = jwtService.getTokenInfo(accessToken);
        assertThat(tokenInfo).containsKey("username");
        assertThat(tokenInfo).containsKey("tokenType");
        assertThat(tokenInfo).containsKey("issuer");
        assertThat(tokenInfo).containsKey("issuedAt");
        assertThat(tokenInfo).containsKey("expiresAt");
        assertThat(tokenInfo).containsKey("expired");
        assertThat(tokenInfo.get("username")).isEqualTo("testuser");
        assertThat(tokenInfo.get("tokenType")).isEqualTo("access");
        assertThat(tokenInfo.get("expired")).isEqualTo(false);
    }

    @Test
    @DisplayName("Deve funcionar com chaves RSA regeneradas")
    void shouldWorkWithRegeneratedRsaKeys() {
        // Given - Gerar primeiro token
        String firstToken = jwtService.generateAccessToken(testUser);
        assertThat(jwtService.isValidAccessToken(firstToken)).isTrue();
        
        // When - Simular regeneração de chaves (como seria feito na reinicialização)
        rsaKeyManager.forceRegenerateKeys();
        
        // Criar novo JwtService com as novas chaves
        JwtService newJwtService = new JwtService(rsaKeyManager);
        ReflectionTestUtils.setField(newJwtService, "accessTokenExpirationSeconds", 3600L);
        ReflectionTestUtils.setField(newJwtService, "refreshTokenExpirationSeconds", 15552000L);
        ReflectionTestUtils.setField(newJwtService, "issuer", "sistema-java-test");
        
        // Then - Novo token deve funcionar com as novas chaves
        String newToken = newJwtService.generateAccessToken(testUser);
        assertThat(newJwtService.isValidAccessToken(newToken)).isTrue();
        assertThat(newJwtService.extractUsername(newToken)).isEqualTo("testuser");
        
        // Token antigo não deve mais ser válido com as novas chaves
        // Usar try-catch pois a validação pode lançar exceção de assinatura inválida
        try {
            assertThat(newJwtService.isValidAccessToken(firstToken)).isFalse();
        } catch (Exception e) {
            // Esperado: token com chaves antigas deve falhar na validação
            assertThat(e).isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
        }
    }

    @Test
    @DisplayName("Deve verificar compatibilidade das chaves RSA com algoritmo JWT")
    void shouldVerifyRsaKeyCompatibilityWithJwtAlgorithm() {
        // Given
        PrivateKey privateKey = rsaKeyManager.getPrivateKey();
        PublicKey publicKey = rsaKeyManager.getPublicKey();
        
        // When & Then - Verificar propriedades das chaves para JWT
        assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
        assertThat(publicKey.getAlgorithm()).isEqualTo("RSA");
        assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
        assertThat(publicKey.getFormat()).isEqualTo("X.509");
        
        // Verificar que as chaves são válidas para uso com JWT
        assertThat(rsaKeyManager.validateKeyPair(privateKey, publicKey)).isTrue();
        
        // Verificar que o JWT pode ser gerado e validado
        String token = jwtService.generateAccessToken(testUser);
        assertThat(token).isNotNull();
        assertThat(jwtService.isValidAccessToken(token)).isTrue();
    }

    @Test
    @DisplayName("Deve simular ciclo completo de inicialização do servidor com JWT")
    void shouldSimulateCompleteServerInitializationCycleWithJwt() {
        // Given - Simular estado inicial do servidor (sem chaves)
        RSAKeyManager serverRsaKeyManager = new RSAKeyManager();
        ReflectionTestUtils.setField(serverRsaKeyManager, "keysDirectory", tempDir.resolve("server-keys").toString());
        
        // When - Simular inicialização do servidor (@PostConstruct)
        serverRsaKeyManager.initializeKeys();
        
        // Criar serviços que dependem das chaves RSA
        JwtService serverJwtService = new JwtService(serverRsaKeyManager);
        ReflectionTestUtils.setField(serverJwtService, "accessTokenExpirationSeconds", 3600L);
        ReflectionTestUtils.setField(serverJwtService, "refreshTokenExpirationSeconds", 15552000L);
        ReflectionTestUtils.setField(serverJwtService, "issuer", "sistema-java-server");
        
        // Then - Verificar que o sistema está funcionando
        assertThat(serverRsaKeyManager.getPrivateKey()).isNotNull();
        assertThat(serverRsaKeyManager.getPublicKey()).isNotNull();
        
        // Verificar que JWT funciona corretamente
        String accessToken = serverJwtService.generateAccessToken(testUser);
        String refreshToken = serverJwtService.generateRefreshToken(testUser);
        
        assertThat(accessToken).isNotNull();
        assertThat(refreshToken).isNotNull();
        assertThat(serverJwtService.isValidAccessToken(accessToken)).isTrue();
        assertThat(serverJwtService.isValidRefreshToken(refreshToken)).isTrue();
        
        // Verificar que as informações do usuário estão corretas no token
        assertThat(serverJwtService.extractUsername(accessToken)).isEqualTo(testUser.getUsername());
        assertThat(serverJwtService.isTokenValid(accessToken, testUser)).isTrue();
        
        // Verificar que os arquivos de chaves foram criados
        assertThat(tempDir.resolve("server-keys").resolve("private_key.pem")).exists();
        assertThat(tempDir.resolve("server-keys").resolve("public_key.pem")).exists();
    }
}