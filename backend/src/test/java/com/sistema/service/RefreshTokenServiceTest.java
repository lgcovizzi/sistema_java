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
        // Configurar propriedades do serviço
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 15552000L); // 6 meses
        ReflectionTestUtils.setField(refreshTokenService, "maxTokensPerUser", 5);
        ReflectionTestUtils.setField(refreshTokenService, "cleanupEnabled", true);

        // Criar usuário de teste
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setEnabled(true);
        testUser.setRoles(Set.of(Role.USER));
        testUser.setCreatedAt(LocalDateTime.now());

        // Criar refresh token de teste
        testRefreshToken = new RefreshToken();
        testRefreshToken.setId(1L);
        testRefreshToken.setToken("valid-refresh-token-123");
        testRefreshToken.setUser(testUser);
        testRefreshToken.setExpiresAt(LocalDateTime.now().plusMonths(6));
        testRefreshToken.setIsRevoked(false);
        testRefreshToken.setDeviceInfo("Chrome/Windows");
        testRefreshToken.setIpAddress("192.168.1.100");
        testRefreshToken.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        testRefreshToken.setCreatedAt(LocalDateTime.now());

        // Configurar mocks do HttpServletRequest
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.100");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
    }

    @Test
    @DisplayName("Deve criar refresh token para persistência de sessão do cliente")
    void shouldCreateRefreshTokenForClientPersistence() {
        // Given
        when(refreshTokenRepository.countByUserAndIsRevokedFalse(testUser)).thenReturn(2);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // When
        RefreshToken createdToken = refreshTokenService.createRefreshToken(testUser, httpServletRequest);

        // Then
        assertNotNull(createdToken, "Refresh token deve ser criado");
        assertEquals(testUser, createdToken.getUser(), "Token deve estar associado ao usuário correto");
        assertFalse(createdToken.getIsRevoked(), "Token deve estar ativo");
        assertTrue(createdToken.getExpiresAt().isAfter(LocalDateTime.now().plusMonths(5)), 
                  "Token deve ter expiração de longo prazo (6 meses)");
        
        // Verificar informações do dispositivo para identificação única
        assertEquals("192.168.1.100", createdToken.getIpAddress(), "IP deve ser capturado");
        assertNotNull(createdToken.getUserAgent(), "User-Agent deve ser capturado");
        
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Deve validar refresh token existente para evitar novo login")
    void shouldValidateExistingRefreshTokenToAvoidRelogin() {
        // Given
        String tokenValue = "valid-refresh-token-123";
        when(refreshTokenRepository.findByTokenAndIsRevokedFalse(tokenValue))
                .thenReturn(Optional.of(testRefreshToken));

        // When
        Optional<RefreshToken> foundToken = refreshTokenService.findValidRefreshToken(tokenValue);

        // Then
        assertTrue(foundToken.isPresent(), "Token válido deve ser encontrado");
        assertEquals(testRefreshToken, foundToken.get(), "Token retornado deve ser o correto");
        assertFalse(foundToken.get().getIsRevoked(), "Token deve estar ativo");
        assertTrue(foundToken.get().getExpiresAt().isAfter(LocalDateTime.now()), 
                  "Token deve estar dentro do prazo de validade");
        
        verify(refreshTokenRepository).findByTokenAndIsRevokedFalse(tokenValue);
    }

    @Test
    @DisplayName("Deve rejeitar refresh token expirado forçando novo login")
    void shouldRejectExpiredRefreshTokenForcingNewLogin() {
        // Given
        String expiredTokenValue = "expired-refresh-token-456";
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setToken(expiredTokenValue);
        expiredToken.setUser(testUser);
        expiredToken.setExpiresAt(LocalDateTime.now().minusDays(1)); // Expirado
        expiredToken.setIsRevoked(false);
        
        when(refreshTokenRepository.findByTokenAndIsRevokedFalse(expiredTokenValue))
                .thenReturn(Optional.of(expiredToken));

        // When
        Optional<RefreshToken> foundToken = refreshTokenService.findValidRefreshToken(expiredTokenValue);

        // Then
        assertFalse(foundToken.isPresent(), "Token expirado não deve ser considerado válido");
        
        verify(refreshTokenRepository).findByTokenAndIsRevokedFalse(expiredTokenValue);
    }

    @Test
    @DisplayName("Deve gerenciar múltiplos dispositivos com tokens únicos")
    void shouldManageMultipleDevicesWithUniqueTokens() {
        // Given - Simular múltiplos dispositivos
        HttpServletRequest mobileRequest = mock(HttpServletRequest.class);
        when(mobileRequest.getRemoteAddr()).thenReturn("192.168.1.101");
        when(mobileRequest.getHeader("User-Agent")).thenReturn("Mobile Safari/iOS");
        when(mobileRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        
        RefreshToken mobileToken = new RefreshToken();
        mobileToken.setToken("mobile-refresh-token-789");
        mobileToken.setUser(testUser);
        mobileToken.setIpAddress("192.168.1.101");
        mobileToken.setUserAgent("Mobile Safari/iOS");
        mobileToken.setExpiresAt(LocalDateTime.now().plusMonths(6));
        mobileToken.setIsRevoked(false);
        
        when(refreshTokenRepository.countByUserAndIsRevokedFalse(testUser)).thenReturn(1);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(mobileToken);

        // When
        RefreshToken createdMobileToken = refreshTokenService.createRefreshToken(testUser, mobileRequest);

        // Then
        assertNotNull(createdMobileToken, "Token para dispositivo móvel deve ser criado");
        assertEquals("192.168.1.101", createdMobileToken.getIpAddress(), "IP do dispositivo móvel deve ser diferente");
        assertTrue(createdMobileToken.getUserAgent().contains("Mobile"), "User-Agent deve identificar dispositivo móvel");
        
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Deve limitar número máximo de tokens por usuário")
    void shouldLimitMaximumTokensPerUser() {
        // Given - Usuário já tem o máximo de tokens
        when(refreshTokenRepository.countByUserAndIsRevokedFalse(testUser)).thenReturn(5); // Máximo atingido
        when(refreshTokenRepository.findOldestByUserAndIsRevokedFalse(testUser))
                .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // When
        RefreshToken newToken = refreshTokenService.createRefreshToken(testUser, httpServletRequest);

        // Then
        assertNotNull(newToken, "Novo token deve ser criado");
        
        // Verificar que o token mais antigo foi revogado
        verify(refreshTokenRepository).findOldestByUserAndIsRevokedFalse(testUser);
        verify(refreshTokenRepository).save(any(RefreshToken.class)); // Para revogar o antigo e criar o novo
    }

    @Test
    @DisplayName("Deve revogar refresh token no logout mantendo outros dispositivos")
    void shouldRevokeRefreshTokenOnLogoutKeepingOtherDevices() {
        // Given
        String tokenToRevoke = "token-to-revoke-123";
        when(refreshTokenRepository.findByTokenAndIsRevokedFalse(tokenToRevoke))
                .thenReturn(Optional.of(testRefreshToken));

        // When
        refreshTokenService.revokeRefreshToken(tokenToRevoke);

        // Then
        assertTrue(testRefreshToken.getIsRevoked(), "Token deve ser marcado como revogado");
        
        verify(refreshTokenRepository).findByTokenAndIsRevokedFalse(tokenToRevoke);
        verify(refreshTokenRepository).save(testRefreshToken);
    }

    @Test
    @DisplayName("Deve revogar todos os tokens do usuário no logout completo")
    void shouldRevokeAllUserTokensOnCompleteLogout() {
        // Given
        List<RefreshToken> userTokens = List.of(testRefreshToken);
        when(refreshTokenRepository.findAllByUserAndIsRevokedFalse(testUser))
                .thenReturn(userTokens);
        when(refreshTokenRepository.saveAll(userTokens)).thenReturn(userTokens);

        // When
        int revokedCount = refreshTokenService.revokeAllUserTokens(testUser);

        // Then
        assertEquals(1, revokedCount, "Deve retornar o número correto de tokens revogados");
        assertTrue(testRefreshToken.getIsRevoked(), "Token deve ser marcado como revogado");
        
        verify(refreshTokenRepository).findAllByUserAndIsRevokedFalse(testUser);
        verify(refreshTokenRepository).saveAll(userTokens);
    }

    @Test
    @DisplayName("Deve capturar informações do dispositivo para identificação única")
    void shouldCaptureDeviceInformationForUniqueIdentification() {
        // Given
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 192.168.1.100");
        when(refreshTokenRepository.countByUserAndIsRevokedFalse(testUser)).thenReturn(0);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // When
        RefreshToken createdToken = refreshTokenService.createRefreshToken(testUser, httpServletRequest);

        // Then
        assertNotNull(createdToken.getIpAddress(), "IP deve ser capturado");
        assertEquals("203.0.113.1", createdToken.getIpAddress(), "Deve usar o IP real do X-Forwarded-For");
        assertNotNull(createdToken.getUserAgent(), "User-Agent deve ser capturado");
        assertNotNull(createdToken.getDeviceInfo(), "Informações do dispositivo devem ser extraídas");
        
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Deve atualizar último uso do token para monitoramento de atividade")
    void shouldUpdateLastTokenUsageForActivityMonitoring() {
        // Given
        String tokenValue = "active-token-123";
        when(refreshTokenRepository.findByTokenAndIsRevokedFalse(tokenValue))
                .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(testRefreshToken)).thenReturn(testRefreshToken);

        // When
        refreshTokenService.updateLastUsed(tokenValue);

        // Then
        assertNotNull(testRefreshToken.getLastUsedAt(), "Data de último uso deve ser definida");
        assertTrue(testRefreshToken.getLastUsedAt().isAfter(LocalDateTime.now().minusMinutes(1)), 
                  "Data de último uso deve ser recente");
        
        verify(refreshTokenRepository).findByTokenAndIsRevokedFalse(tokenValue);
        verify(refreshTokenRepository).save(testRefreshToken);
    }

    @Test
    @DisplayName("Deve validar token apenas se usuário estiver ativo")
    void shouldValidateTokenOnlyIfUserIsActive() {
        // Given
        testUser.setEnabled(false); // Usuário desabilitado
        String tokenValue = "token-disabled-user-123";
        when(refreshTokenRepository.findByTokenAndIsRevokedFalse(tokenValue))
                .thenReturn(Optional.of(testRefreshToken));

        // When
        Optional<RefreshToken> foundToken = refreshTokenService.findValidRefreshToken(tokenValue);

        // Then
        assertFalse(foundToken.isPresent(), "Token de usuário desabilitado não deve ser válido");
        
        verify(refreshTokenRepository).findByTokenAndIsRevokedFalse(tokenValue);
    }
}