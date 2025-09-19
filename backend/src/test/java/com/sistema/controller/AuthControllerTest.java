package com.sistema.controller;

import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.service.AuthService;
import com.sistema.service.JwtService;
import com.sistema.service.TokenBlacklistService;
import com.sistema.service.AttemptService;
import com.sistema.service.UserService;
import com.sistema.service.CaptchaService;
import com.sistema.service.EmailVerificationService;
import com.sistema.service.PasswordResetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

/**
 * Testes unitários para AuthController
 * Seguindo práticas de TDD com padrão Given-When-Then
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private AttemptService attemptService;

    @Mock
    private UserService userService;

    @Mock
    private CaptchaService captchaService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private Map<String, Object> authResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice() // Add global exception handling
                .build();
        objectMapper = new ObjectMapper();
        
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.USER);
        testUser.setEnabled(true);
        testUser.setEmailVerified(true);
        testUser.setCreatedAt(LocalDateTime.now());
        
        // Setup auth response
        authResponse = new HashMap<>();
        authResponse.put("accessToken", "access-token-123");
        authResponse.put("refreshToken", "refresh-token-123");
        authResponse.put("tokenType", "Bearer");
        authResponse.put("expiresIn", 3600);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", 1L);
        userInfo.put("username", "testuser");
        userInfo.put("email", "test@example.com");
        userInfo.put("emailVerified", true);
        userInfo.put("roles", List.of("USER"));
        authResponse.put("user", userInfo);
    }

    @Test
    @DisplayName("Deve realizar login com sucesso quando credenciais são válidas")
    void shouldLoginSuccessfullyWhenCredentialsAreValid() throws Exception {
        // Given
        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");
        
        when(attemptService.isCaptchaRequiredForLogin(anyString())).thenReturn(false);
        
        // Configurar mock do authService.authenticate
        Map<String, Object> authResponse = new HashMap<>();
        authResponse.put("accessToken", "access-token-123");
        authResponse.put("refreshToken", "refresh-token-123");
        authResponse.put("tokenType", "Bearer");
        authResponse.put("expiresIn", 900L);
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", 1L);
        userInfo.put("email", "test@example.com");
        userInfo.put("username", "testuser");
        userInfo.put("emailVerified", true);
        authResponse.put("user", userInfo);
        
        when(authService.authenticate(eq("test@example.com"), eq("password123")))
                .thenReturn(authResponse);
        
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-123"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.emailVerified").value(true));
        
        verify(authService).authenticate(eq("test@example.com"), eq("password123"));
    }

    @Test
    @DisplayName("Deve retornar erro 401 quando credenciais são inválidas")
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        // Given
        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
        loginRequest.setEmail("testuser@example.com");
        loginRequest.setPassword("wrongpassword");
        
        when(authService.authenticate(anyString(), anyString(), any(HttpServletRequest.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));
        
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(true))
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("Deve registrar usuário com sucesso quando dados são válidos")
    void shouldRegisterUserSuccessfullyWhenDataIsValid() throws Exception {
        // Given
        AuthController.RegisterRequest registerRequest = new AuthController.RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");
        registerRequest.setCpf("11144477735");
        
        when(authService.registerAndAuthenticate(anyString(), anyString(), anyString(), anyString(), anyString(), any(HttpServletRequest.class)))
            .thenReturn(authResponse);
        
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-123"))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
        
        verify(authService).registerAndAuthenticate(eq("newuser@example.com"), eq("password123"), eq("New"), eq("User"), eq("11144477735"), any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando dados de registro são inválidos")
    void shouldReturn400WhenRegistrationDataIsInvalid() throws Exception {
        // Given
        AuthController.RegisterRequest registerRequest = new AuthController.RegisterRequest();
        registerRequest.setEmail("invalid-email"); // Invalid email
        registerRequest.setPassword("123"); // Too short password
        registerRequest.setFirstName(""); // Invalid firstName
        registerRequest.setLastName(""); // Invalid lastName
        registerRequest.setCpf("invalid-cpf"); // Invalid CPF
        
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve renovar token com sucesso quando refresh token é válido")
    void shouldRefreshTokenSuccessfullyWhenRefreshTokenIsValid() throws Exception {
        // Given
        AuthController.RefreshTokenRequest refreshRequest = new AuthController.RefreshTokenRequest();
        refreshRequest.setRefreshToken("valid-refresh-token");
        
        Map<String, Object> refreshResponse = new HashMap<>();
        refreshResponse.put("accessToken", "new-access-token");
        refreshResponse.put("refreshToken", "new-refresh-token");
        refreshResponse.put("tokenType", "Bearer");
        refreshResponse.put("expiresIn", 3600);
        
        when(authService.refreshAccessToken(eq("valid-refresh-token"), any(HttpServletRequest.class)))
            .thenReturn(refreshResponse);
        
        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
        
        verify(authService).refreshAccessToken(eq("valid-refresh-token"), any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("Deve retornar erro 401 quando refresh token é inválido")
    void shouldReturn401WhenRefreshTokenIsInvalid() throws Exception {
        // Given
        AuthController.RefreshTokenRequest refreshRequest = new AuthController.RefreshTokenRequest();
        refreshRequest.setRefreshToken("invalid-refresh-token");
        
        when(authService.refreshAccessToken(eq("invalid-refresh-token"), any(HttpServletRequest.class)))
            .thenThrow(new IllegalArgumentException("Invalid refresh token"));
        
        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(true))
                .andExpect(jsonPath("$.message").value("Refresh token inválido"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("Deve validar token com sucesso quando token é válido")
    void shouldValidateTokenSuccessfullyWhenTokenIsValid() throws Exception {
        // Given
        AuthController.ValidateTokenRequest validateRequest = new AuthController.ValidateTokenRequest();
        validateRequest.setToken("valid-token");
        
        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put("username", "testuser");
        tokenInfo.put("roles", List.of("USER"));
        tokenInfo.put("exp", System.currentTimeMillis() / 1000 + 3600);
        
        when(jwtService.getTokenInfo("valid-token")).thenReturn(tokenInfo);
        when(jwtService.isValidAccessToken("valid-token")).thenReturn(true);
        when(jwtService.getTimeToExpiration("valid-token")).thenReturn(3600L);
        
        // When & Then
        mockMvc.perform(post("/api/auth/validate-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.tokenInfo.username").value("testuser"))
                .andExpect(jsonPath("$.timeToExpiration").value(3600));
    }

    @Test
    @DisplayName("Deve retornar token inválido quando token é expirado")
    void shouldReturnInvalidWhenTokenIsExpired() throws Exception {
        // Given
        AuthController.ValidateTokenRequest validateRequest = new AuthController.ValidateTokenRequest();
        validateRequest.setToken("expired-token");
        
        when(jwtService.getTokenInfo("expired-token"))
            .thenThrow(new RuntimeException("Token expired"));
        
        // When & Then
        mockMvc.perform(post("/api/auth/validate-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.error").value("Token expired"));
    }

    @Test
    @DisplayName("Deve realizar logout com sucesso")
    void shouldLogoutSuccessfully() throws Exception {
        // Given
        AuthController.LogoutRequest logoutRequest = new AuthController.LogoutRequest();
        logoutRequest.setRefreshToken("refresh-token-123");
        logoutRequest.setRevokeAll(false);
        
        doNothing().when(authService).logout("refresh-token-123", false);
        
        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout realizado com sucesso"));
        
        verify(authService).logout("refresh-token-123", false);
    }

    @Test
    @DisplayName("Deve alterar senha com sucesso quando senha atual é válida")
    void shouldChangePasswordSuccessfullyWhenCurrentPasswordIsValid() throws Exception {
        // Given
        AuthController.ChangePasswordRequest changePasswordRequest = new AuthController.ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("currentPassword");
        changePasswordRequest.setNewPassword("newPassword123");
        
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        
        doNothing().when(authService).changePassword("testuser", "currentPassword", "newPassword123");
        
        // When & Then
        mockMvc.perform(post("/api/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Senha alterada com sucesso"));
        
        verify(authService).changePassword("testuser", "currentPassword", "newPassword123");
    }

    @Test
    @DisplayName("Deve retornar erro 400 quando senha atual é incorreta")
    void shouldReturn400WhenCurrentPasswordIsIncorrect() throws Exception {
        // Given
        AuthController.ChangePasswordRequest changePasswordRequest = new AuthController.ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("wrongPassword");
        changePasswordRequest.setNewPassword("newPassword123");
        
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        
        doThrow(new IllegalArgumentException("Senha atual incorreta"))
            .when(authService).changePassword("testuser", "wrongPassword", "newPassword123");
        
        // When & Then
        mockMvc.perform(post("/api/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(true))
                .andExpect(jsonPath("$.message").value("Senha atual incorreta"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_PASSWORD"));
    }

    @Test
    @DisplayName("Deve retornar perfil do usuário com sucesso")
    void shouldReturnUserProfileSuccessfully() throws Exception {
        // Given
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        
        // When & Then
        mockMvc.perform(get("/api/auth/me")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("test@example.com"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

}