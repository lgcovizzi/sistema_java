package com.sistema.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para TokenBlacklistService
 * Seguindo práticas de TDD com padrão Given-When-Then
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistService Tests")
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    private String testToken;
    private String testJti;
    private String testUsername;
    private Date futureExpiration;
    private Date pastExpiration;

    @BeforeEach
    void setUp() {
        testToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTYzOTU2NzIwMCwiZXhwIjoxNjM5NTcwODAwfQ.signature";
        testJti = "test-jti-123";
        testUsername = "testuser";
        futureExpiration = new Date(System.currentTimeMillis() + 3600000); // 1 hour from now
        pastExpiration = new Date(System.currentTimeMillis() - 3600000); // 1 hour ago

        // Setup Redis template mocks
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Deve revogar token com sucesso quando token é válido")
    void shouldRevokeTokenSuccessfullyWhenTokenIsValid() {
        // Given
        when(jwtService.extractJti(testToken)).thenReturn(testJti);
        when(jwtService.extractExpiration(testToken)).thenReturn(futureExpiration);

        // When
        boolean result = tokenBlacklistService.revokeToken(testToken);

        // Then
        assertThat(result).isTrue();
        verify(valueOperations).set(
            eq("jwt:blacklist:" + testJti),
            eq("revoked"),
            anyLong(),
            eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("Deve usar hash do token quando JTI não está disponível")
    void shouldUseTokenHashWhenJtiNotAvailable() {
        // Given
        when(jwtService.extractJti(testToken)).thenThrow(new RuntimeException("JTI not available"));
        when(jwtService.extractExpiration(testToken)).thenReturn(futureExpiration);
        String expectedKey = "jwt:blacklist:" + testToken.hashCode();

        // When
        boolean result = tokenBlacklistService.revokeToken(testToken);

        // Then
        assertThat(result).isTrue();
        verify(valueOperations).set(
            eq(expectedKey),
            eq("revoked"),
            anyLong(),
            eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("Deve retornar false quando token já está expirado")
    void shouldReturnFalseWhenTokenAlreadyExpired() {
        // Given
        when(jwtService.extractJti(testToken)).thenReturn(testJti);
        when(jwtService.extractExpiration(testToken)).thenReturn(pastExpiration);

        // When
        boolean result = tokenBlacklistService.revokeToken(testToken);

        // Then
        assertThat(result).isFalse();
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("Deve retornar false quando ocorre erro durante revogação")
    void shouldReturnFalseWhenErrorOccursDuringRevocation() {
        // Given
        when(jwtService.extractJti(testToken)).thenThrow(new RuntimeException("JTI error"));
        when(jwtService.extractExpiration(testToken)).thenThrow(new RuntimeException("Expiration error"));

        // When
        boolean result = tokenBlacklistService.revokeToken(testToken);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve detectar token revogado corretamente")
    void shouldDetectRevokedTokenCorrectly() {
        // Given
        when(jwtService.extractJti(testToken)).thenReturn(testJti);
        when(redisTemplate.hasKey("jwt:blacklist:" + testJti)).thenReturn(true);

        // When
        boolean result = tokenBlacklistService.isTokenRevoked(testToken);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false para token não revogado")
    void shouldReturnFalseForNonRevokedToken() {
        // Given
        when(jwtService.extractJti(testToken)).thenReturn(testJti);
        when(redisTemplate.hasKey("jwt:blacklist:" + testJti)).thenReturn(false);

        // When
        boolean result = tokenBlacklistService.isTokenRevoked(testToken);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false quando Redis retorna null")
    void shouldReturnFalseWhenRedisReturnsNull() {
        // Given
        when(jwtService.extractJti(testToken)).thenReturn(testJti);
        when(redisTemplate.hasKey("jwt:blacklist:" + testJti)).thenReturn(null);

        // When
        boolean result = tokenBlacklistService.isTokenRevoked(testToken);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false quando ocorre erro ao verificar blacklist")
    void shouldReturnFalseWhenErrorOccursCheckingBlacklist() {
        // Given
        when(jwtService.extractJti(testToken)).thenThrow(new RuntimeException("Redis error"));

        // When
        boolean result = tokenBlacklistService.isTokenRevoked(testToken);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve remover token da blacklist com sucesso")
    void shouldRemoveTokenFromBlacklistSuccessfully() {
        // Given
        when(jwtService.extractJti(testToken)).thenReturn(testJti);
        when(redisTemplate.delete("jwt:blacklist:" + testJti)).thenReturn(true);

        // When
        boolean result = tokenBlacklistService.removeFromBlacklist(testToken);

        // Then
        assertThat(result).isTrue();
        verify(redisTemplate).delete("jwt:blacklist:" + testJti);
    }

    @Test
    @DisplayName("Deve retornar false quando token não existe na blacklist para remoção")
    void shouldReturnFalseWhenTokenNotExistsForRemoval() {
        // Given
        when(jwtService.extractJti(testToken)).thenReturn(testJti);
        when(redisTemplate.delete("jwt:blacklist:" + testJti)).thenReturn(false);

        // When
        boolean result = tokenBlacklistService.removeFromBlacklist(testToken);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false quando Redis retorna null na remoção")
    void shouldReturnFalseWhenRedisReturnsNullOnRemoval() {
        // Given
        when(jwtService.extractJti(testToken)).thenReturn(testJti);
        when(redisTemplate.delete("jwt:blacklist:" + testJti)).thenReturn(null);

        // When
        boolean result = tokenBlacklistService.removeFromBlacklist(testToken);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve revogar todos os tokens do usuário com sucesso")
    void shouldRevokeAllUserTokensSuccessfully() {
        // When
        boolean result = tokenBlacklistService.revokeAllUserTokens(testUsername);

        // Then
        assertThat(result).isTrue();
        verify(valueOperations).set(
            eq("jwt:blacklist:user:" + testUsername),
            anyLong(),
            eq(Duration.ofDays(30))
        );
    }

    @Test
    @DisplayName("Deve retornar false quando erro ocorre ao revogar todos os tokens")
    void shouldReturnFalseWhenErrorOccursRevokingAllTokens() {
        // Given
        doThrow(new RuntimeException("Redis error")).when(valueOperations)
            .set(anyString(), anyLong(), any(Duration.class));

        // When
        boolean result = tokenBlacklistService.revokeAllUserTokens(testUsername);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve detectar token globalmente revogado")
    void shouldDetectGloballyRevokedToken() {
        // Given
        long revocationTime = System.currentTimeMillis();
        Date tokenIssuedAt = new Date(revocationTime - 1000); // Token issued before revocation
        
        when(valueOperations.get("jwt:blacklist:user:" + testUsername)).thenReturn(revocationTime);
        when(jwtService.extractIssuedAt(testToken)).thenReturn(tokenIssuedAt);

        // When
        boolean result = tokenBlacklistService.isTokenGloballyRevoked(testToken, testUsername);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false para token emitido após revogação global")
    void shouldReturnFalseForTokenIssuedAfterGlobalRevocation() {
        // Given
        long revocationTime = System.currentTimeMillis();
        Date tokenIssuedAt = new Date(revocationTime + 1000); // Token issued after revocation
        
        when(valueOperations.get("jwt:blacklist:user:" + testUsername)).thenReturn(revocationTime);
        when(jwtService.extractIssuedAt(testToken)).thenReturn(tokenIssuedAt);

        // When
        boolean result = tokenBlacklistService.isTokenGloballyRevoked(testToken, testUsername);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false quando não há revogação global para usuário")
    void shouldReturnFalseWhenNoGlobalRevocationForUser() {
        // Given
        when(valueOperations.get("jwt:blacklist:user:" + testUsername)).thenReturn(null);

        // When
        boolean result = tokenBlacklistService.isTokenGloballyRevoked(testToken, testUsername);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false quando token não tem data de emissão")
    void shouldReturnFalseWhenTokenHasNoIssuedAt() {
        // Given
        long revocationTime = System.currentTimeMillis();
        when(valueOperations.get("jwt:blacklist:user:" + testUsername)).thenReturn(revocationTime);
        when(jwtService.extractIssuedAt(testToken)).thenReturn(null);

        // When
        boolean result = tokenBlacklistService.isTokenGloballyRevoked(testToken, testUsername);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false quando erro ocorre na verificação global")
    void shouldReturnFalseWhenErrorOccursInGlobalCheck() {
        // Given
        when(valueOperations.get("jwt:blacklist:user:" + testUsername))
            .thenThrow(new RuntimeException("Redis error"));

        // When
        boolean result = tokenBlacklistService.isTokenGloballyRevoked(testToken, testUsername);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deve retornar tamanho da blacklist corretamente")
    void shouldReturnBlacklistSizeCorrectly() {
        // Given
        Set<String> keys = Set.of(
            "jwt:blacklist:token1",
            "jwt:blacklist:token2",
            "jwt:blacklist:user:user1"
        );
        when(redisTemplate.keys("jwt:blacklist:*")).thenReturn(keys);

        // When
        long size = tokenBlacklistService.getBlacklistSize();

        // Then
        assertThat(size).isEqualTo(3);
    }

    @Test
    @DisplayName("Deve retornar -1 quando erro ocorre ao obter tamanho da blacklist")
    void shouldReturnMinusOneWhenErrorOccursGettingSize() {
        // Given
        when(redisTemplate.keys("jwt:blacklist:*")).thenThrow(new RuntimeException("Redis error"));

        // When
        long size = tokenBlacklistService.getBlacklistSize();

        // Then
        assertThat(size).isEqualTo(-1);
    }

    @Test
    @DisplayName("Deve calcular TTL corretamente para token com expiração futura")
    void shouldCalculateTTLCorrectlyForFutureExpiration() {
        // Given
        when(jwtService.extractJti(testToken)).thenReturn(testJti);
        when(jwtService.extractExpiration(testToken)).thenReturn(futureExpiration);

        // When
        boolean result = tokenBlacklistService.revokeToken(testToken);

        // Then
        assertThat(result).isTrue();
        verify(valueOperations).set(
            eq("jwt:blacklist:" + testJti),
            eq("revoked"),
            longThat(ttl -> ttl > 0 && ttl <= 3600), // Should be positive and <= 1 hour
            eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("Deve usar TTL padrão quando token não tem expiração")
    void shouldUseDefaultTTLWhenTokenHasNoExpiration() {
        // Given
        when(jwtService.extractJti(testToken)).thenReturn(testJti);
        when(jwtService.extractExpiration(testToken)).thenReturn(null);

        // When
        boolean result = tokenBlacklistService.revokeToken(testToken);

        // Then
        assertThat(result).isTrue();
        verify(valueOperations).set(
            eq("jwt:blacklist:" + testJti),
            eq("revoked"),
            eq(86400L), // 24 hours in seconds
            eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("Deve lidar com token sem JTI usando hash como fallback")
    void shouldHandleTokenWithoutJtiUsingHashFallback() {
        // Given
        when(jwtService.extractJti(testToken)).thenReturn(null);
        when(jwtService.extractExpiration(testToken)).thenReturn(futureExpiration);
        String expectedKey = "jwt:blacklist:" + testToken.hashCode();

        // When
        boolean result = tokenBlacklistService.revokeToken(testToken);

        // Then
        assertThat(result).isTrue();
        verify(valueOperations).set(
            eq(expectedKey),
            eq("revoked"),
            anyLong(),
            eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("Deve lidar com JTI vazio usando hash como fallback")
    void shouldHandleEmptyJtiUsingHashFallback() {
        // Given
        when(jwtService.extractJti(testToken)).thenReturn("");
        when(jwtService.extractExpiration(testToken)).thenReturn(futureExpiration);
        String expectedKey = "jwt:blacklist:" + testToken.hashCode();

        // When
        boolean result = tokenBlacklistService.revokeToken(testToken);

        // Then
        assertThat(result).isTrue();
        verify(valueOperations).set(
            eq(expectedKey),
            eq("revoked"),
            anyLong(),
            eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("Deve verificar token revogado usando hash quando JTI não disponível")
    void shouldCheckRevokedTokenUsingHashWhenJtiNotAvailable() {
        // Given
        when(jwtService.extractJti(testToken)).thenThrow(new RuntimeException("JTI error"));
        String expectedKey = "jwt:blacklist:" + testToken.hashCode();
        when(redisTemplate.hasKey(expectedKey)).thenReturn(true);

        // When
        boolean result = tokenBlacklistService.isTokenRevoked(testToken);

        // Then
        assertThat(result).isTrue();
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("Deve remover token da blacklist usando hash quando JTI não disponível")
    void shouldRemoveTokenFromBlacklistUsingHashWhenJtiNotAvailable() {
        // Given
        when(jwtService.extractJti(testToken)).thenReturn(null);
        String expectedKey = "jwt:blacklist:" + testToken.hashCode();
        when(redisTemplate.delete(expectedKey)).thenReturn(true);

        // When
        boolean result = tokenBlacklistService.removeFromBlacklist(testToken);

        // Then
        assertThat(result).isTrue();
        verify(redisTemplate).delete(expectedKey);
    }
}