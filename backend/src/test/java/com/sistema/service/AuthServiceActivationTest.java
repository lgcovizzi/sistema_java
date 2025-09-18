package com.sistema.service;

import com.sistema.entity.RefreshToken;
import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import com.sistema.util.CpfGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários específicos para verificar que apenas contas ativadas podem fazer login.
 * Foca na validação de email verificado durante o processo de autenticação.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Autenticação - Apenas Contas Ativadas")
class AuthServiceActivationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User unverifiedUser;
    private User verifiedUser;
    private User disabledUser;
    private User adminUser;
    private final String testPassword = "password123";
    private final String encodedPassword = "$2a$10$encodedPassword";

    @BeforeEach
    void setUp() {
        // Usuário com email não verificado
        unverifiedUser = new User();
        unverifiedUser.setId(1L);
        unverifiedUser.setEmail("unverified@example.com");
        unverifiedUser.setPassword(encodedPassword);
        unverifiedUser.setFirstName("Unverified");
        unverifiedUser.setLastName("User");
        unverifiedUser.setCpf(CpfGenerator.generateCpf());
        unverifiedUser.setRole(UserRole.USER);
        unverifiedUser.setEnabled(true);
        unverifiedUser.setEmailVerified(false); // Email NÃO verificado
        unverifiedUser.setCreatedAt(LocalDateTime.now());
        unverifiedUser.setUpdatedAt(LocalDateTime.now());

        // Usuário com email verificado
        verifiedUser = new User();
        verifiedUser.setId(2L);
        verifiedUser.setEmail("verified@example.com");
        verifiedUser.setPassword(encodedPassword);
        verifiedUser.setFirstName("Verified");
        verifiedUser.setLastName("User");
        verifiedUser.setCpf(CpfGenerator.generateCpf());
        verifiedUser.setRole(UserRole.USER);
        verifiedUser.setEnabled(true);
        verifiedUser.setEmailVerified(true); // Email verificado
        verifiedUser.setCreatedAt(LocalDateTime.now());
        verifiedUser.setUpdatedAt(LocalDateTime.now());

        // Usuário desabilitado (mesmo com email verificado)
        disabledUser = new User();
        disabledUser.setId(3L);
        disabledUser.setEmail("disabled@example.com");
        disabledUser.setPassword(encodedPassword);
        disabledUser.setFirstName("Disabled");
        disabledUser.setLastName("User");
        disabledUser.setCpf(CpfGenerator.generateCpf());
        disabledUser.setRole(UserRole.USER);
        disabledUser.setEnabled(false); // Usuário desabilitado
        disabledUser.setEmailVerified(true);
        disabledUser.setCreatedAt(LocalDateTime.now());
        disabledUser.setUpdatedAt(LocalDateTime.now());

        // Usuário admin (automaticamente verificado)
        adminUser = new User();
        adminUser.setId(4L);
        adminUser.setEmail("lgcovizzi@gmail.com");
        adminUser.setPassword(encodedPassword);
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setCpf(CpfGenerator.generateCpf());
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setEnabled(true);
        adminUser.setEmailVerified(true); // Admin é automaticamente verificado
        adminUser.setCreatedAt(LocalDateTime.now());
        adminUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve rejeitar login de usuário com email não verificado")
    void shouldRejectLoginForUnverifiedEmail() {
        // Given
        String email = unverifiedUser.getEmail();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(unverifiedUser));

        // When & Then
        assertThatThrownBy(() -> authService.authenticate(email, testPassword))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email não verificado. Verifique sua caixa de entrada e clique no link de verificação.");

        // Verificar que a autenticação não prosseguiu
        verify(userRepository).findByEmail(email);
        verify(authenticationManager, never()).authenticate(any());
        verify(jwtService, never()).generateAccessToken(any(User.class));
    }

    @Test
    @DisplayName("Deve aceitar login de usuário com email verificado")
    void shouldAcceptLoginForVerifiedEmail() {
        // Given
        String email = verifiedUser.getEmail();
        String accessToken = "access-token";
        RefreshToken refreshToken = mock(RefreshToken.class);
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(verifiedUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateAccessToken(verifiedUser)).thenReturn(accessToken);
        when(jwtService.getAccessTokenExpiration()).thenReturn(900L);
        when(refreshTokenService.createRefreshToken(eq(verifiedUser), isNull())).thenReturn(refreshToken);
        when(refreshToken.getToken()).thenReturn("refresh-token");

        // When
        Map<String, Object> result = authService.authenticate(email, testPassword);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("accessToken")).isEqualTo(accessToken);
        assertThat(result.get("refreshToken")).isEqualTo("refresh-token");
        assertThat(result.get("tokenType")).isEqualTo("Bearer");

        verify(userRepository).findByEmail(email);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateAccessToken(verifiedUser);
        verify(refreshTokenService).createRefreshToken(eq(verifiedUser), isNull());
    }

    @Test
    @DisplayName("Deve rejeitar login de usuário desabilitado mesmo com email verificado")
    void shouldRejectLoginForDisabledUserEvenWithVerifiedEmail() {
        // Given
        String email = disabledUser.getEmail();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(disabledUser));

        // When & Then
        assertThatThrownBy(() -> authService.authenticate(email, testPassword))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Usuário desabilitado");

        // Verificar que a autenticação não prosseguiu
        verify(userRepository).findByEmail(email);
        verify(authenticationManager, never()).authenticate(any());
        verify(jwtService, never()).generateAccessToken(any(User.class));
    }

    @Test
    @DisplayName("Deve aceitar login de usuário admin (automaticamente verificado)")
    void shouldAcceptLoginForAdminUser() {
        // Given
        String email = adminUser.getEmail();
        String accessToken = "admin-access-token";
        RefreshToken refreshToken = mock(RefreshToken.class);
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(adminUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateAccessToken(adminUser)).thenReturn(accessToken);
        when(jwtService.getAccessTokenExpiration()).thenReturn(900L);
        when(refreshTokenService.createRefreshToken(eq(adminUser), isNull())).thenReturn(refreshToken);
        when(refreshToken.getToken()).thenReturn("admin-refresh-token");

        // When
        Map<String, Object> result = authService.authenticate(email, testPassword);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("accessToken")).isEqualTo(accessToken);
        assertThat(result.get("refreshToken")).isEqualTo("admin-refresh-token");
        
        // Verificar dados do usuário na resposta
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) result.get("user");
        assertThat(userInfo.get("role")).isEqualTo("ADMIN");

        verify(userRepository).findByEmail(email);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateAccessToken(adminUser);
    }

    @Test
    @DisplayName("Deve rejeitar login com credenciais inválidas")
    void shouldRejectLoginWithInvalidCredentials() {
        // Given
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.authenticate(email, testPassword))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciais inválidas");

        verify(userRepository).findByEmail(email);
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("Deve validar entrada antes de verificar email")
    void shouldValidateInputBeforeCheckingEmail() {
        // When & Then - Email vazio
        assertThatThrownBy(() -> authService.authenticate("", testPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email é obrigatório");

        // When & Then - Senha vazia
        assertThatThrownBy(() -> authService.authenticate("test@example.com", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Senha é obrigatória");

        // When & Then - Email inválido
        assertThatThrownBy(() -> authService.authenticate("invalid-email", testPassword))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email deve ser válido");

        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Deve verificar ordem correta das validações no login")
    void shouldVerifyCorrectOrderOfValidationsInLogin() {
        // Given - Usuário existe mas email não verificado
        String email = unverifiedUser.getEmail();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(unverifiedUser));

        // When & Then
        assertThatThrownBy(() -> authService.authenticate(email, testPassword))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email não verificado. Verifique sua caixa de entrada e clique no link de verificação.");

        // Verificar que as validações seguem a ordem correta:
        // 1. Validação de entrada (email/senha)
        // 2. Busca do usuário
        // 3. Verificação se usuário está habilitado
        // 4. Verificação se email foi verificado
        // 5. Autenticação com Spring Security (não deve chegar aqui)
        
        verify(userRepository).findByEmail(email);
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("Deve permitir login apenas após verificação de email")
    void shouldAllowLoginOnlyAfterEmailVerification() {
        // Given - Usuário inicialmente não verificado
        String email = unverifiedUser.getEmail();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(unverifiedUser));

        // When & Then - Primeiro login deve falhar
        assertThatThrownBy(() -> authService.authenticate(email, testPassword))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email não verificado. Verifique sua caixa de entrada e clique no link de verificação.");

        // Given - Simular verificação de email
        unverifiedUser.setEmailVerified(true);
        String accessToken = "access-token-after-verification";
        RefreshToken refreshToken = mock(RefreshToken.class);
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateAccessToken(unverifiedUser)).thenReturn(accessToken);
        when(jwtService.getAccessTokenExpiration()).thenReturn(900L);
        when(refreshTokenService.createRefreshToken(eq(unverifiedUser), isNull())).thenReturn(refreshToken);
        when(refreshToken.getToken()).thenReturn("refresh-token-after-verification");

        // When - Segundo login deve ser bem-sucedido
        Map<String, Object> result = authService.authenticate(email, testPassword);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("accessToken")).isEqualTo(accessToken);
        
        verify(userRepository, times(2)).findByEmail(email);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Deve verificar que usuário não verificado não pode fazer login mesmo com senha correta")
    void shouldVerifyUnverifiedUserCannotLoginEvenWithCorrectPassword() {
        // Given
        String email = unverifiedUser.getEmail();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(unverifiedUser));
        
        // Simular que a senha estaria correta (mas não deve chegar na verificação de senha)
        when(passwordEncoder.matches(testPassword, encodedPassword)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.authenticate(email, testPassword))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email não verificado. Verifique sua caixa de entrada e clique no link de verificação.");

        // Verificar que a verificação de senha não foi executada
        verify(passwordEncoder, never()).matches(any(), any());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("Deve verificar que status de verificação de email é checado a cada login")
    void shouldVerifyEmailVerificationStatusIsCheckedOnEveryLogin() {
        // Given
        String email = verifiedUser.getEmail();
        
        // Primeiro login - usuário verificado
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(verifiedUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateAccessToken(verifiedUser)).thenReturn("token1");
        when(jwtService.getAccessTokenExpiration()).thenReturn(900L);
        
        RefreshToken refreshToken1 = mock(RefreshToken.class);
        when(refreshToken1.getToken()).thenReturn("refresh1");
        when(refreshTokenService.createRefreshToken(eq(verifiedUser), isNull())).thenReturn(refreshToken1);

        // When - Primeiro login bem-sucedido
        Map<String, Object> result1 = authService.authenticate(email, testPassword);
        assertThat(result1).isNotNull();

        // Given - Simular que o email foi "desverificado" (cenário hipotético)
        verifiedUser.setEmailVerified(false);
        reset(authenticationManager, jwtService, refreshTokenService);

        // When & Then - Segundo login deve falhar
        assertThatThrownBy(() -> authService.authenticate(email, testPassword))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Email não verificado. Verifique sua caixa de entrada e clique no link de verificação.");

        // Verificar que o status foi checado em ambas as tentativas
        verify(userRepository, times(2)).findByEmail(email);
    }
}