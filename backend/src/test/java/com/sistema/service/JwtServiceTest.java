package com.sistema.service;

import com.sistema.config.RSAKeyManager;
import com.sistema.entity.User;
import com.sistema.entity.Role;
import com.sistema.service.interfaces.TokenOperations;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

/**
 * Testes unitários para JwtService
 * Seguindo práticas de TDD com padrão Given-When-Then
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Tests")
class JwtServiceTest {

    @Mock
    private RSAKeyManager rsaKeyManager;

    @InjectMocks
    private JwtService jwtService;

    private User testUser;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        // Generate test RSA key pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();

        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRoles(List.of(Role.USER, Role.ADMIN));
        testUser.setEnabled(true);
        testUser.setCreatedAt(LocalDateTime.now());

        // Mock RSA key manager
        lenient().when(rsaKeyManager.getPrivateKey()).thenReturn(privateKey);
        lenient().when(rsaKeyManager.getPublicKey()).thenReturn(publicKey);

        // Set test values using reflection
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationSeconds", 3600L); // 1 hour
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationSeconds", 86400L); // 24 hours
        ReflectionTestUtils.setField(jwtService, "issuer", "sistema-java");
    }

    @Test
    @DisplayName("Deve gerar access token válido para usuário")
    void shouldGenerateValidAccessTokenForUser() {
        // When
        String accessToken = jwtService.generateAccessToken(testUser);

        // Then
        assertThat(accessToken).isNotNull();
        assertThat(accessToken).isNotEmpty();
        assertThat(accessToken.split("\\.")).hasSize(3); // JWT has 3 parts

        // Verify token content
        String username = jwtService.extractUsername(accessToken);
        assertThat(username).isEqualTo("testuser");

        boolean isValid = jwtService.isValidAccessToken(accessToken);
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Deve gerar refresh token válido para usuário")
    void shouldGenerateValidRefreshTokenForUser() {
        // When
        String refreshToken = jwtService.generateRefreshToken(testUser);

        // Then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        assertThat(refreshToken.split("\\.")).hasSize(3); // JWT has 3 parts

        // Verify token content
        String username = jwtService.extractUsername(refreshToken);
        assertThat(username).isEqualTo("testuser");

        boolean isValid = jwtService.isValidRefreshToken(refreshToken);
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Deve extrair username corretamente do token")
    void shouldExtractUsernameCorrectlyFromToken() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        String extractedUsername = jwtService.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Deve extrair informações corretamente do token")
    void shouldExtractInformationCorrectlyFromToken() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When & Then
        assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
        assertThat(jwtService.extractRoles(token)).containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Deve validar token corretamente quando token é válido")
    void shouldValidateTokenCorrectlyWhenTokenIsValid() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        boolean isValid = jwtService.isValidAccessToken(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar token quando assinatura é inválida")
    void shouldRejectTokenWhenSignatureIsInvalid() {
        // Given
        String token = jwtService.generateAccessToken(testUser);
        String tamperedToken = token.substring(0, token.length() - 10) + "tampered123";

        // When & Then
        assertThatThrownBy(() -> jwtService.isValidAccessToken(tamperedToken))
            .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("Deve rejeitar token malformado")
    void shouldRejectMalformedToken() {
        // Given
        String malformedToken = "invalid.token.format";

        // When & Then
        assertThatThrownBy(() -> jwtService.isValidAccessToken(malformedToken))
            .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("Deve detectar token expirado")
    void shouldDetectExpiredToken() {
        // Given - Set very short expiration time
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationSeconds", 1L); // 1 second
        String token = jwtService.generateAccessToken(testUser);

        // Wait for token to expire
        try {
            Thread.sleep(1100); // Wait 1.1 seconds to ensure token expires
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When & Then
        assertThatThrownBy(() -> jwtService.isValidAccessToken(token))
            .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Deve retornar informações completas do token")
    void shouldReturnCompleteTokenInformation() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        Map<String, Object> tokenInfo = jwtService.getTokenInfo(token);

        // Then
        assertThat(tokenInfo).containsKey("username");
        assertThat(tokenInfo).containsKey("userId");
        assertThat(tokenInfo).containsKey("email");
        assertThat(tokenInfo).containsKey("roles");
        assertThat(tokenInfo).containsKey("tokenType");
        assertThat(tokenInfo).containsKey("issuer");
        assertThat(tokenInfo).containsKey("issuedAt");
        assertThat(tokenInfo).containsKey("expiresAt");

        assertThat(tokenInfo.get("username")).isEqualTo("testuser");
        assertThat(tokenInfo.get("userId")).isEqualTo(1);
        assertThat(tokenInfo.get("email")).isEqualTo("test@example.com");
        assertThat(tokenInfo.get("roles")).isEqualTo(List.of("ROLE_USER", "ROLE_ADMIN"));
        assertThat(tokenInfo.get("tokenType")).isEqualTo("access");
    }

    @Test
    @DisplayName("Deve calcular tempo para expiração corretamente")
    void shouldCalculateTimeToExpirationCorrectly() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        Long timeToExpiration = jwtService.getTimeToExpiration(token);

        // Then
        assertThat(timeToExpiration).isPositive();
        assertThat(timeToExpiration).isLessThanOrEqualTo(3600L); // Should be <= 1 hour
        assertThat(timeToExpiration).isGreaterThan(3590L); // Should be > 59 minutes 50 seconds
    }

    @Test
    @DisplayName("Deve retornar zero para token expirado ao calcular tempo")
    void shouldReturnZeroForExpiredTokenWhenCalculatingTime() {
        // Given - Set very short expiration time
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationSeconds", 1L); // 1 second
        String token = jwtService.generateAccessToken(testUser);

        // Wait for token to expire
        try {
            Thread.sleep(1100); // Wait 1.1 seconds to ensure token expires
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        Long timeToExpiration = jwtService.getTimeToExpiration(token);

        // Then
        assertThat(timeToExpiration).isZero();
    }

    @Test
    @DisplayName("Deve validar refresh token corretamente")
    void shouldValidateRefreshTokenCorrectly() {
        // Given
        String refreshToken = jwtService.generateRefreshToken(testUser);

        // When
        boolean isValid = jwtService.isValidRefreshToken(refreshToken);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar access token quando validado como refresh token")
    void shouldRejectAccessTokenWhenValidatedAsRefreshToken() {
        // Given
        String accessToken = jwtService.generateAccessToken(testUser);

        // When
        boolean isValid = jwtService.isValidRefreshToken(accessToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar refresh token quando validado como access token")
    void shouldRejectRefreshTokenWhenValidatedAsAccessToken() {
        // Given
        String refreshToken = jwtService.generateRefreshToken(testUser);

        // When
        boolean isValid = jwtService.isValidAccessToken(refreshToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Deve gerar tokens diferentes para cada chamada")
    void shouldGenerateDifferentTokensForEachCall() {
        // When
        String token1 = jwtService.generateAccessToken(testUser);
        
        // Add small delay to ensure different timestamps
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String token2 = jwtService.generateAccessToken(testUser);

        // Then
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Deve incluir todas as roles do usuário no token")
    void shouldIncludeAllUserRolesInToken() {
        // Given
        testUser.setRoles(List.of(Role.USER, Role.ADMIN));
        String token = jwtService.generateAccessToken(testUser);

        // When
        List<String> roles = jwtService.extractRoles(token);

        // Then
        assertThat(roles).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Deve lidar com usuário sem roles")
    void shouldHandleUserWithoutRoles() {
        // Given
        testUser.setRoles(List.of());
        
        // When
        String token = jwtService.generateAccessToken(testUser);
        
        // Then
        assertThat(token).isNotNull();
        List<String> roles = jwtService.extractRoles(token);
        assertThat(roles).isEmpty();
    }

    @Test
    @DisplayName("Deve extrair roles do usuário corretamente")
    void shouldExtractUserRolesCorrectly() {
        // Given
        testUser.setRoles(List.of(Role.USER));
        String token = jwtService.generateAccessToken(testUser);

        // When
        List<String> roles = jwtService.extractRoles(token);

        // Then
        assertThat(roles).containsExactly("ROLE_USER");
    }

    @Nested
    @DisplayName("TokenOperations Interface Tests")
    class TokenOperationsInterfaceTests {

        @Test
        @DisplayName("Deve implementar interface TokenOperations")
        void shouldImplementTokenOperationsInterface() {
            // Then
            assertThat(jwtService).isInstanceOf(TokenOperations.class);
        }

        @Test
        @DisplayName("Deve fornecer todos os métodos da interface")
        void shouldProvideAllInterfaceMethods() {
            // Given
            TokenOperations operations = jwtService;
            Map<String, Object> claims = Map.of("username", "testuser");

            // When & Then - Não deve lançar exceções para métodos da interface
            assertThat(operations).isNotNull();
            
            User testUser = new User();
            testUser.setEmail("test@example.com");
            
            String accessToken = operations.generateAccessToken(testUser);
            assertThat(accessToken).isNotNull();
            
            String refreshToken = operations.generateRefreshToken(testUser);
            assertThat(refreshToken).isNotNull();
            
            String subject = operations.extractSubject(accessToken);
            assertThat(subject).isEqualTo("test@example.com");
            
            boolean isValid = operations.isTokenValid(accessToken, testUser);
            assertThat(isValid).isTrue();
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Deve lidar com chave privada nula")
        void shouldHandleNullPrivateKey() {
            // Given
            ReflectionTestUtils.setField(jwtService, "privateKey", null);

            // When & Then
            assertThatThrownBy(() -> jwtService.generateAccessToken(testUser))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Deve lidar com chave pública nula")
        void shouldHandleNullPublicKey() {
            // Given
            String token = jwtService.generateAccessToken(testUser);
            ReflectionTestUtils.setField(jwtService, "publicKey", null);

            // When
            boolean isValid = jwtService.validateToken(token);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Deve lidar com token muito longo")
        void shouldHandleVeryLongToken() {
            // Given
            StringBuilder longString = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                longString.append("a");
            }
            String longToken = longString.toString();

            // When
            boolean isValid = jwtService.validateToken(longToken);
            String subject = jwtService.extractSubject(longToken);

            // Then
            assertThat(isValid).isFalse();
            assertThat(subject).isNull();
        }

        @Test
        @DisplayName("Deve lidar com caracteres especiais no subject")
        void shouldHandleSpecialCharactersInSubject() {
            // Given
            String specialSubject = "user@domain.com!@#$%^&*()";
            Map<String, Object> claims = Map.of("username", "testuser");

            // When
            String token = jwtService.generateToken(specialSubject, claims, 15);
            String extractedSubject = jwtService.extractSubject(token);

            // Then
            assertThat(token).isNotNull();
            assertThat(extractedSubject).isEqualTo(specialSubject);
        }

        @Test
        @DisplayName("Deve lidar com claims vazios")
        void shouldHandleEmptyClaims() {
            // Given
            Map<String, Object> emptyClaims = Map.of();

            // When
            String token = jwtService.generateToken("test@example.com", emptyClaims, 15);

            // Then
            assertThat(token).isNotNull();
            assertThat(jwtService.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("Deve lidar com TTL zero")
        void shouldHandleZeroTTL() {
            // Given
            Map<String, Object> claims = Map.of("username", "testuser");

            // When & Then
            assertThatThrownBy(() -> jwtService.generateToken("test@example.com", claims, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Deve lidar com TTL negativo")
        void shouldHandleNegativeTTL() {
            // Given
            Map<String, Object> claims = Map.of("username", "testuser");

            // When & Then
            assertThatThrownBy(() -> jwtService.generateToken("test@example.com", claims, -1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Deve gerar múltiplos tokens eficientemente")
        void shouldGenerateMultipleTokensEfficiently() {
            // Given
            Map<String, Object> claims = Map.of("username", "testuser");

            // When & Then
            for (int i = 0; i < 100; i++) {
                String token = jwtService.generateToken("test" + i + "@example.com", claims, 15);
                assertThat(token).isNotNull();
            }
        }

        @Test
        @DisplayName("Deve validar múltiplos tokens eficientemente")
        void shouldValidateMultipleTokensEfficiently() {
            // Given
            String token = jwtService.generateAccessToken(testUser);

            // When & Then
            for (int i = 0; i < 100; i++) {
                boolean isValid = jwtService.validateToken(token);
                assertThat(isValid).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Token Security Tests")
    class TokenSecurityTests {

        @Test
        @DisplayName("Deve gerar tokens únicos para cada chamada")
        void shouldGenerateUniqueTokensForEachCall() {
            // Given
            Map<String, Object> claims = Map.of("username", "testuser");

            // When
            String token1 = jwtService.generateToken("test@example.com", claims, 15);
            String token2 = jwtService.generateToken("test@example.com", claims, 15);

            // Then
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("Deve incluir timestamp de criação no token")
        void shouldIncludeCreationTimestampInToken() {
            // Given
            String token = jwtService.generateAccessToken(testUser);

            // When
            Date issuedAt = jwtService.extractIssuedAt(token);
            Date expiration = jwtService.extractExpiration(token);

            // Then
            assertThat(issuedAt).isNotNull();
            assertThat(expiration).isNotNull();
            assertThat(expiration).isAfter(issuedAt);
        }

        @Test
        @DisplayName("Deve rejeitar token com assinatura modificada")
        void shouldRejectTokenWithModifiedSignature() {
            // Given
            String validToken = jwtService.generateAccessToken(testUser);
            String[] tokenParts = validToken.split("\\.");
            String modifiedToken = tokenParts[0] + "." + tokenParts[1] + ".modified_signature";

            // When
            boolean isValid = jwtService.validateToken(modifiedToken);

            // Then
            assertThat(isValid).isFalse();
        }
    }

}