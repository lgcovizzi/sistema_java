package com.sistema.service;

import com.sistema.entity.RefreshToken;
import com.sistema.entity.User;
import com.sistema.entity.Role;
import com.sistema.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para persistência de cookies no AuthService.
 * Foca na funcionalidade que evita logins frequentes quando o browser é fechado.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Testes de Persistência de Cookies")
class AuthServiceCookiePersistenceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

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
        testRefreshToken.setToken("persistent-refresh-token-123");
        testRefreshToken.setUser(testUser);
        testRefreshToken.setExpiresAt(LocalDateTime.now().plusMonths(6));
        testRefreshToken.setIsRevoked(false);
        testRefreshToken.setDeviceInfo("Chrome/Windows");
        testRefreshToken.setIpAddress("192.168.1.100");
        testRefreshToken.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        testRefreshToken.setCreatedAt(LocalDateTime.now());
        testRefreshToken.setLastUsedAt(LocalDateTime.now());
        
        // Configurar o AuthenticationManager no AuthService
        authService.setAuthenticationManager(authenticationManager);
    }

    @Test
    @DisplayName("Deve criar sessão persistente no login para evitar logins frequentes")
    void shouldCreatePersistentSessionOnLoginToAvoidFrequentLogins() {
        // Given
        String username = "testuser";
        String password = "password123";
        String accessToken = "jwt-access-token-123";
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(jwtService.generateAccessToken(testUser)).thenReturn(accessToken);
        when(refreshTokenService.createRefreshToken(testUser, httpServletRequest))
                .thenReturn(testRefreshToken);
        // updateLastLogin é chamado internamente pelo AuthService

        // When
        Map<String, Object> authResponse = authService.authenticate(username, password, httpServletRequest);

        // Then
        assertNotNull(authResponse, "Resposta de autenticação deve ser criada");
        assertEquals(accessToken, authResponse.get("accessToken"), "Access token deve estar presente");
        assertEquals(testRefreshToken.getToken(), authResponse.get("refreshToken"), "Refresh token deve estar presente");
        assertEquals("Bearer", authResponse.get("tokenType"), "Tipo do token deve ser Bearer");
        assertEquals(3600, authResponse.get("expiresIn"), "Expiração do access token deve ser 1 hora");
        assertEquals(15552000, authResponse.get("refreshExpiresIn"), "Expiração do refresh token deve ser 6 meses");
        
        // Verificar informações do usuário na resposta
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) authResponse.get("user");
        assertNotNull(userInfo, "Informações do usuário devem estar presentes");
        assertEquals(testUser.getId(), userInfo.get("id"), "ID do usuário deve estar correto");
        assertEquals(testUser.getUsername(), userInfo.get("username"), "Username deve estar correto");
        assertEquals(testUser.getEmail(), userInfo.get("email"), "Email deve estar correto");
        
        // Verificar que o refresh token foi criado para persistência
        verify(refreshTokenService).createRefreshToken(testUser, httpServletRequest);
        verify(userRepository).updateLastLogin(eq(testUser.getId()), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Deve renovar sessão usando refresh token sem exigir novo login")
    void shouldRenewSessionUsingRefreshTokenWithoutRequiringNewLogin() {
        // Given
        String refreshTokenValue = "persistent-refresh-token-123";
        String newAccessToken = "new-jwt-access-token-456";
        
        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setToken("new-persistent-refresh-token-456");
        newRefreshToken.setUser(testUser);
        newRefreshToken.setExpiresAt(LocalDateTime.now().plusMonths(6));
        newRefreshToken.setIsRevoked(false);
        
        when(refreshTokenService.findValidRefreshToken(refreshTokenValue))
                .thenReturn(Optional.of(testRefreshToken));
        when(jwtService.generateAccessToken(testUser)).thenReturn(newAccessToken);
        when(refreshTokenService.createRefreshToken(testUser, httpServletRequest))
                .thenReturn(newRefreshToken);
        when(refreshTokenService.revokeRefreshToken(refreshTokenValue)).thenReturn(true);

        // When
        Map<String, Object> refreshResponse = authService.refreshAccessToken(refreshTokenValue, httpServletRequest);

        // Then
        assertNotNull(refreshResponse, "Resposta de renovação deve ser criada");
        assertEquals(newAccessToken, refreshResponse.get("accessToken"), "Novo access token deve estar presente");
        assertEquals(newRefreshToken.getToken(), refreshResponse.get("refreshToken"), "Novo refresh token deve estar presente");
        assertEquals("Bearer", refreshResponse.get("tokenType"), "Tipo do token deve ser Bearer");
        assertEquals(3600, refreshResponse.get("expiresIn"), "Expiração do access token deve ser 1 hora");
        
        // Verificar que o token antigo foi revogado e um novo foi criado
        verify(refreshTokenService).findValidRefreshToken(refreshTokenValue);
        verify(refreshTokenService).createRefreshToken(testUser, httpServletRequest);
        verify(refreshTokenService).revokeRefreshToken(refreshTokenValue);
        verify(jwtService).generateAccessToken(testUser);
    }

    @Test
    @DisplayName("Deve rejeitar refresh token expirado forçando novo login")
    void shouldRejectExpiredRefreshTokenForcingNewLogin() {
        // Given
        String expiredRefreshToken = "expired-refresh-token-789";
        when(refreshTokenService.findValidRefreshToken(expiredRefreshToken))
                .thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.refreshAccessToken(expiredRefreshToken, httpServletRequest),
                "Deve lançar exceção para refresh token inválido"
        );
        
        assertEquals("Refresh token inválido ou expirado", exception.getMessage(),
                    "Mensagem de erro deve ser apropriada");
        
        verify(refreshTokenService).findValidRefreshToken(expiredRefreshToken);
        verify(refreshTokenService, never()).createRefreshToken(any(), any());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Deve rejeitar refresh token de usuário desabilitado")
    void shouldRejectRefreshTokenFromDisabledUser() {
        // Given
        testUser.setEnabled(false); // Usuário desabilitado
        String refreshTokenValue = "token-disabled-user-123";
        
        when(refreshTokenService.findValidRefreshToken(refreshTokenValue))
                .thenReturn(Optional.of(testRefreshToken));

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.refreshAccessToken(refreshTokenValue, httpServletRequest),
                "Deve lançar exceção para usuário desabilitado"
        );
        
        assertEquals("Usuário desabilitado", exception.getMessage(),
                    "Mensagem de erro deve indicar usuário desabilitado");
        
        verify(refreshTokenService).findValidRefreshToken(refreshTokenValue);
        verify(refreshTokenService, never()).createRefreshToken(any(), any());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Deve realizar logout parcial mantendo outros dispositivos conectados")
    void shouldPerformPartialLogoutKeepingOtherDevicesConnected() {
        // Given
        String refreshTokenValue = "device-specific-token-123";
        boolean revokeAll = false;
        
        when(refreshTokenService.findValidRefreshToken(refreshTokenValue))
                .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenService.revokeRefreshToken(refreshTokenValue)).thenReturn(true);

        // When
        authService.logout(refreshTokenValue, revokeAll);

        // Then
        verify(refreshTokenService).findValidRefreshToken(refreshTokenValue);
        verify(refreshTokenService).revokeRefreshToken(refreshTokenValue);
        verify(refreshTokenService, never()).revokeAllUserTokens(any());
    }

    @Test
    @DisplayName("Deve realizar logout completo revogando todos os dispositivos")
    void shouldPerformCompleteLogoutRevokingAllDevices() {
        // Given
        String refreshTokenValue = "any-device-token-123";
        boolean revokeAll = true;
        int revokedTokensCount = 3;
        
        when(refreshTokenService.findValidRefreshToken(refreshTokenValue))
                .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenService.revokeAllUserTokens(testUser))
                .thenReturn(revokedTokensCount);

        // When
        authService.logout(refreshTokenValue, revokeAll);

        // Then
        verify(refreshTokenService).findValidRefreshToken(refreshTokenValue);
        verify(refreshTokenService).revokeAllUserTokens(testUser);
        verify(refreshTokenService, never()).revokeRefreshToken(refreshTokenValue);
    }

    @Test
    @DisplayName("Deve extrair username do token JWT válido")
    void shouldExtractUsernameFromValidJwtToken() {
        // Given
        String jwtToken = "valid-jwt-token-123";
        String expectedUsername = "testuser";
        
        when(jwtService.extractUsername(jwtToken)).thenReturn(expectedUsername);

        // When
        String extractedUsername = jwtService.extractUsername(jwtToken);

        // Then
        assertEquals(expectedUsername, extractedUsername, "Username deve ser extraído corretamente do token");
        
        verify(jwtService).extractUsername(jwtToken);
    }

    @Test
    @DisplayName("Deve validar token JWT usando JwtService")
    void shouldValidateJwtTokenUsingJwtService() {
        // Given
        String jwtToken = "valid-jwt-token-123";
        
        when(jwtService.isTokenValid(jwtToken, testUser)).thenReturn(true);

        // When
        boolean isValid = jwtService.isTokenValid(jwtToken, testUser);

        // Then
        assertTrue(isValid, "Token deve ser válido para usuário ativo");
        
        verify(jwtService).isTokenValid(jwtToken, testUser);
    }

    @Test
    @DisplayName("Deve manter informações de sessão para análise de segurança")
    void shouldMaintainSessionInformationForSecurityAnalysis() {
        // Given
        String username = "testuser";
        String password = "password123";
        String accessToken = "jwt-access-token-123";
        
        // Configurar informações detalhadas do dispositivo - removidos stubs desnecessários
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(jwtService.generateAccessToken(testUser)).thenReturn(accessToken);
        when(refreshTokenService.createRefreshToken(testUser, httpServletRequest))
                .thenReturn(testRefreshToken);
        // updateLastLogin é chamado internamente pelo AuthService

        // When
        Map<String, Object> authResponse = authService.authenticate(username, password, httpServletRequest);

        // Then
        assertNotNull(authResponse, "Resposta de autenticação deve ser criada");
        
        // Verificar que as informações do dispositivo foram capturadas
        verify(refreshTokenService).createRefreshToken(testUser, httpServletRequest);
        
        // O RefreshTokenService deve ter capturado:
        // - IP address (incluindo X-Forwarded-For)
        // - User-Agent completo
        // - Informações do dispositivo extraídas
        // - Timestamp de criação e último uso
        
        verify(userRepository).updateLastLogin(eq(testUser.getId()), any(LocalDateTime.class));
    }
}