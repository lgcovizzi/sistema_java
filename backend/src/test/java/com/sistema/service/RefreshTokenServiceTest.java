package com.sistema.service;

import com.sistema.entity.RefreshToken;
import com.sistema.entity.User;
import com.sistema.entity.Role;
import com.sistema.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para RefreshTokenService.
 * Foca na persistência de cookies pelo lado do cliente para evitar logins frequentes.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService - Testes de Persistência de Cookies")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        // Configurar usuário de teste
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setEnabled(true);
        testUser.setRoles(List.of(Role.USER));
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setLastLogin(LocalDateTime.now().minusDays(1));

        // Configurar refresh token de teste
        testRefreshToken = new RefreshToken();
        testRefreshToken.setId(1L);
        testRefreshToken.setToken("test-refresh-token-123");
        testRefreshToken.setUser(testUser);
        testRefreshToken.setExpiresAt(LocalDateTime.now().plusMonths(6));
        testRefreshToken.setIsRevoked(false);
        testRefreshToken.setDeviceInfo("Chrome/Windows");
        testRefreshToken.setIpAddress("192.168.1.100");
        testRefreshToken.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        testRefreshToken.setCreatedAt(LocalDateTime.now());
        testRefreshToken.setLastUsedAt(LocalDateTime.now());

        // Configurar valores de configuração
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 15552000L); // 6 meses
        ReflectionTestUtils.setField(refreshTokenService, "maxTokensPerUser", 5);
        ReflectionTestUtils.setField(refreshTokenService, "cleanupEnabled", true);
    }

    @Test
    @DisplayName("Deve criar refresh token para persistência de sessão do cliente")
    void shouldCreateRefreshTokenForClientPersistence() {
        // Given
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);
        when(refreshTokenRepository.findValidByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // When
        RefreshToken createdToken = refreshTokenService.createRefreshToken(testUser, httpServletRequest);

        // Then
        assertNotNull(createdToken, "Refresh token deve ser criado");
        assertEquals(testRefreshToken.getToken(), createdToken.getToken(), "Token deve ser o mesmo");
        assertEquals(testUser, createdToken.getUser(), "Usuário deve ser o mesmo");
        assertNotNull(createdToken.getExpiresAt(), "Data de expiração deve ser definida");
        assertFalse(createdToken.getIsRevoked(), "Token não deve estar revogado");
        
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Deve encontrar token válido usando método real")
    void shouldFindValidTokenUsingRealMethod() {
        // Given
        String tokenValue = "valid-token-123";
        testRefreshToken.setToken(tokenValue);
        testRefreshToken.setExpiresAt(LocalDateTime.now().plusDays(30));
        testRefreshToken.setIsRevoked(false);
        
        when(refreshTokenRepository.findValidByToken(eq(tokenValue), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(testRefreshToken);

        // When
        Optional<RefreshToken> result = refreshTokenService.findValidRefreshToken(tokenValue);

        // Then
        assertTrue(result.isPresent(), "Token válido deve ser encontrado");
        assertEquals(testRefreshToken, result.get(), "Token retornado deve ser o mesmo");
        
        verify(refreshTokenRepository).findValidByToken(eq(tokenValue), any(LocalDateTime.class));
        verify(refreshTokenRepository).save(testRefreshToken); // Para atualizar lastUsedAt
    }

    @Test
    @DisplayName("Deve retornar vazio para token inválido")
    void shouldReturnEmptyForInvalidToken() {
        // Given
        String invalidToken = "invalid-token-456";
        
        when(refreshTokenRepository.findValidByToken(eq(invalidToken), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // When
        Optional<RefreshToken> result = refreshTokenService.findValidRefreshToken(invalidToken);

        // Then
        assertFalse(result.isPresent(), "Token inválido não deve ser encontrado");
        
        verify(refreshTokenRepository).findValidByToken(eq(invalidToken), any(LocalDateTime.class));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar vazio para token nulo ou vazio")
    void shouldReturnEmptyForNullOrEmptyToken() {
        // When & Then
        assertFalse(refreshTokenService.findValidRefreshToken(null).isPresent(), 
                   "Token nulo deve retornar vazio");
        assertFalse(refreshTokenService.findValidRefreshToken("").isPresent(), 
                   "Token vazio deve retornar vazio");
        assertFalse(refreshTokenService.findValidRefreshToken("   ").isPresent(), 
                   "Token com espaços deve retornar vazio");
        
        verify(refreshTokenRepository, never()).findValidByToken(any(), any());
    }

    @Test
    @DisplayName("Deve revogar refresh token específico")
    void shouldRevokeSpecificRefreshToken() {
        // Given
        String tokenValue = "token-to-revoke-123";
        when(refreshTokenRepository.updateLastUsed(eq(tokenValue), any(LocalDateTime.class))).thenReturn(1);

        // When
        int updated = refreshTokenRepository.updateLastUsed(tokenValue, LocalDateTime.now());

        // Then
        assertEquals(1, updated, "Token deve ser atualizado com sucesso");
        
        verify(refreshTokenRepository).updateLastUsed(eq(tokenValue), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Deve revogar todos os tokens do usuário")
    void shouldRevokeAllUserTokens() {
        // Given
        int expectedRevokedCount = 3;
        when(refreshTokenRepository.revokeAllByUser(testUser)).thenReturn(expectedRevokedCount);

        // When
        int revokedCount = refreshTokenService.revokeAllUserTokens(testUser);

        // Then
        assertEquals(expectedRevokedCount, revokedCount, "Número de tokens revogados deve ser correto");
        
        verify(refreshTokenRepository).revokeAllByUser(testUser);
    }

    @Test
    @DisplayName("Deve capturar informações do dispositivo para identificação única")
    void shouldCaptureDeviceInformationForUniqueIdentification() {
        // Given
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpServletRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.100");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);
        when(refreshTokenRepository.findValidByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // When
        RefreshToken createdToken = refreshTokenService.createRefreshToken(testUser, httpServletRequest);

        // Then
        assertNotNull(createdToken, "Token deve ser criado");
        
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(httpServletRequest).getHeader("X-Forwarded-For");
        verify(httpServletRequest).getHeader("X-Real-IP");
        verify(httpServletRequest, times(2)).getHeader("User-Agent");
        verify(httpServletRequest).getRemoteAddr();
    }

    @Test
    @DisplayName("Deve verificar se usuário atingiu limite de tokens")
    void shouldCheckIfUserReachedTokenLimit() {
        // Given
        when(refreshTokenRepository.countValidByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(5L);

        // When
        boolean hasReachedLimit = refreshTokenService.hasReachedTokenLimit(testUser);

        // Then
        assertTrue(hasReachedLimit, "Usuário deve ter atingido o limite de tokens");
        
        verify(refreshTokenRepository).countValidByUser(eq(testUser), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Deve obter estatísticas de tokens do usuário")
    void shouldGetUserTokenStatistics() {
        // Given
        Object[] mockStats = {3L, 2L, 0L, 1L}; // total, válidos, expirados, revogados
        when(refreshTokenRepository.getTokenStatsByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(mockStats);

        // When
        Object[] stats = refreshTokenRepository.getTokenStatsByUser(testUser, LocalDateTime.now());

        // Then
        assertArrayEquals(mockStats, stats, "Estatísticas devem estar corretas");
        
        verify(refreshTokenRepository).getTokenStatsByUser(eq(testUser), any(LocalDateTime.class));
    }
}