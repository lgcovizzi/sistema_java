package com.sistema.service;

import com.sistema.entity.RefreshToken;
import com.sistema.entity.User;
import com.sistema.entity.UserRole;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para persistência de cookies pelo lado do cliente.
 * Foca em evitar logins frequentes quando o browser é fechado.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Persistência de Cookies")
class CookiePersistenceTest {

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
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setEnabled(true);
        testUser.setRole(UserRole.USER);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setLastLogin(LocalDateTime.now().minusDays(1));

        // Configurar refresh token de teste
        testRefreshToken = new RefreshToken();
        testRefreshToken.setId(1L);
        testRefreshToken.setToken("persistent-cookie-token-123");
        testRefreshToken.setUser(testUser);
        testRefreshToken.setExpiresAt(LocalDateTime.now().plusMonths(6)); // 6 meses para persistência
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
    @DisplayName("Deve criar token de longa duração para persistência de cookies")
    void shouldCreateLongLivedTokenForCookiePersistence() {
        // Given
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.100");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);
        when(refreshTokenRepository.findValidByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // When
        RefreshToken createdToken = refreshTokenService.createRefreshToken(testUser, httpServletRequest);

        // Then
        assertNotNull(createdToken, "Token de persistência deve ser criado");
        assertEquals(testUser, createdToken.getUser(), "Token deve estar associado ao usuário");
        assertFalse(createdToken.getIsRevoked(), "Token deve estar ativo para persistência");
        assertTrue(createdToken.getExpiresAt().isAfter(LocalDateTime.now().plusMonths(5)), 
                  "Token deve ter expiração de longo prazo (6 meses) para persistência");
        
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Deve encontrar token válido para evitar novo login após fechamento do browser")
    void shouldFindValidTokenToAvoidReloginAfterBrowserClose() {
        // Given
        String persistentToken = "persistent-cookie-token-123";
        when(refreshTokenRepository.findValidByToken(eq(persistentToken), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // When
        Optional<RefreshToken> foundToken = refreshTokenService.findValidRefreshToken(persistentToken);

        // Then
        assertTrue(foundToken.isPresent(), "Token persistente deve ser encontrado");
        assertEquals(testRefreshToken, foundToken.get(), "Token retornado deve ser o correto");
        assertFalse(foundToken.get().getIsRevoked(), "Token deve estar ativo");
        assertTrue(foundToken.get().getExpiresAt().isAfter(LocalDateTime.now()), 
                  "Token deve estar dentro do prazo de validade");
        
        verify(refreshTokenRepository).findValidByToken(eq(persistentToken), any(LocalDateTime.class));
        verify(refreshTokenRepository).save(testRefreshToken); // Atualiza lastUsedAt
    }

    @Test
    @DisplayName("Deve rejeitar token expirado forçando novo login")
    void shouldRejectExpiredTokenForcingNewLogin() {
        // Given
        String expiredToken = "expired-cookie-token-456";
        when(refreshTokenRepository.findValidByToken(eq(expiredToken), any(LocalDateTime.class)))
                .thenReturn(Optional.empty()); // Token expirado não é retornado pela query

        // When
        Optional<RefreshToken> foundToken = refreshTokenService.findValidRefreshToken(expiredToken);

        // Then
        assertFalse(foundToken.isPresent(), "Token expirado não deve ser considerado válido");
        
        verify(refreshTokenRepository).findValidByToken(eq(expiredToken), any(LocalDateTime.class));
        verify(refreshTokenRepository, never()).save(any()); // Não deve atualizar token inválido
    }

    @Test
    @DisplayName("Deve gerenciar múltiplos dispositivos com tokens únicos")
    void shouldManageMultipleDevicesWithUniqueTokens() {
        // Given - Simular dispositivo móvel
        HttpServletRequest mobileRequest = mock(HttpServletRequest.class);
        when(mobileRequest.getRemoteAddr()).thenReturn("192.168.1.101");
        when(mobileRequest.getHeader("User-Agent")).thenReturn("Mobile Safari/iOS");
        
        RefreshToken mobileToken = new RefreshToken();
        mobileToken.setToken("mobile-persistent-token-789");
        mobileToken.setUser(testUser);
        mobileToken.setIpAddress("192.168.1.101");
        mobileToken.setUserAgent("Mobile Safari/iOS");
        mobileToken.setExpiresAt(LocalDateTime.now().plusMonths(6));
        mobileToken.setIsRevoked(false);
        
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(mobileToken);
        when(refreshTokenRepository.findValidByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // When
        RefreshToken createdMobileToken = refreshTokenService.createRefreshToken(testUser, mobileRequest);

        // Then
        assertNotNull(createdMobileToken, "Token para dispositivo móvel deve ser criado");
        assertEquals(testUser, createdMobileToken.getUser(), "Token deve estar associado ao usuário");
        
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Deve revogar todos os tokens do usuário no logout completo")
    void shouldRevokeAllUserTokensOnCompleteLogout() {
        // Given
        int expectedRevokedCount = 3;
        when(refreshTokenRepository.revokeAllByUser(testUser)).thenReturn(expectedRevokedCount);

        // When
        int revokedCount = refreshTokenService.revokeAllUserTokens(testUser);

        // Then
        assertEquals(expectedRevokedCount, revokedCount, "Deve revogar todos os tokens do usuário");
        
        verify(refreshTokenRepository).revokeAllByUser(testUser);
    }

    @Test
    @DisplayName("Deve verificar se usuário atingiu limite de tokens persistentes")
    void shouldCheckIfUserReachedPersistentTokenLimit() {
        // Given
        when(refreshTokenRepository.countValidByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(5L); // Máximo de 5 tokens

        // When
        boolean hasReachedLimit = refreshTokenService.hasReachedTokenLimit(testUser);

        // Then
        assertTrue(hasReachedLimit, "Usuário deve ter atingido o limite de tokens persistentes");
        
        verify(refreshTokenRepository).countValidByUser(eq(testUser), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Deve capturar informações do dispositivo para identificação única")
    void shouldCaptureDeviceInformationForUniqueIdentification() {
        // Given
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);
        when(refreshTokenRepository.findValidByUser(eq(testUser), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // When
        RefreshToken createdToken = refreshTokenService.createRefreshToken(testUser, httpServletRequest);

        // Then
        assertNotNull(createdToken, "Token com informações do dispositivo deve ser criado");
        
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(httpServletRequest, atLeastOnce()).getHeader("User-Agent");
        verify(httpServletRequest).getHeader("X-Forwarded-For");
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
        
        // Verificar que não houve chamadas desnecessárias ao repository
        verify(refreshTokenRepository, never()).findValidByToken(any(), any());
    }
}