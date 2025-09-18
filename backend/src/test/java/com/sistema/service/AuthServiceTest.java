package com.sistema.service;

import com.sistema.entity.RefreshToken;
import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import com.sistema.service.base.BaseUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService Tests")
class AuthServiceTest {

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
    private EmailVerificationService emailVerificationService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        // Configurar mocks da classe pai BaseUserService
        authService.setUserRepository(userRepository);
        authService.setPasswordEncoder(passwordEncoder);
        authService.setAuthenticationManager(authenticationManager);
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEnabled(true);
        testUser.setRole(UserRole.USER);
        testUser.setLastLogin(LocalDateTime.now());

        testRefreshToken = new RefreshToken();
        testRefreshToken.setToken("refresh-token-123");
        testRefreshToken.setUser(testUser);
        testRefreshToken.setExpiresAt(LocalDateTime.now().plusDays(30));
        
        // Configurar mock para findById usado pelo updateLastLogin
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
    }

    @Test
    @DisplayName("Should load user by email successfully")
    void loadUserByUsername_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        UserDetails result = authService.loadUserByUsername("test@example.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("test@example.com");
        assertThat(result.isEnabled()).isTrue();
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void loadUserByUsername_UserNotFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@email.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.loadUserByUsername("nonexistent@email.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Usuário não encontrado: nonexistent@email.com");
    }

    @Test
    @DisplayName("Should authenticate user successfully with email")
    void authenticate_WithEmail_Success() {
        // Given
        String validPassword = "Password123!";
        String accessToken = "access-token-123";
        String refreshToken = "refresh-token-123";
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(jwtService.generateAccessToken(testUser)).thenReturn(accessToken);
        when(refreshTokenService.createRefreshToken(testUser, request)).thenReturn(testRefreshToken);

        // When
        Map<String, Object> result = authService.authenticate("test@example.com", validPassword, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("accessToken")).isEqualTo(accessToken);
        assertThat(result.get("refreshToken")).isEqualTo(refreshToken);
        assertThat(result.get("tokenType")).isEqualTo("Bearer");
        assertThat(result.get("expiresIn")).isEqualTo(3600);
        
        Map<String, Object> userInfo = (Map<String, Object>) result.get("user");
        assertThat(userInfo.get("email")).isEqualTo("test@example.com");
        
        // Verificar que o usuário foi salvo para atualizar o lastLoginAt
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when authentication fails")
    void authenticate_BadCredentials() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        assertThatThrownBy(() -> authService.authenticate("testuser", "wrongpassword", request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Credenciais inválidas");
        
        // Verify that updateLastLogin was not called since authentication failed
        verify(userRepository, never()).updateLastLogin(any(), any());
    }

    @Test
    @DisplayName("Should register new user successfully")
    void register_Success() {
        // Given
        String validPassword = "Password123!";
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByCpf("11144477735")).thenReturn(false);
        when(passwordEncoder.encode(validPassword)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = authService.register("new@example.com", validPassword, "New", "User", "11144477735");

        // Then
        assertThat(result).isNotNull();
        verify(userRepository).findByEmail("new@example.com");
        verify(userRepository).existsByCpf("11144477735");
        verify(passwordEncoder).encode(validPassword);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void register_EmailAlreadyExists() {
        // Given
        String validPassword = "Password123!";
        User existingUser = new User();
        existingUser.setId(2L);
        existingUser.setEmail("existing@example.com");
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> authService.register("existing@example.com", validPassword, "New", "User", "12345678901"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email já está em uso");
    }

    @Test
    @DisplayName("Should register and authenticate user successfully")
    void registerAndAuthenticate_Success() {
        // Given
        String validPassword = "Password123!";
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByCpf("11144477735")).thenReturn(false);
        when(passwordEncoder.encode(validPassword)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(testUser, request)).thenReturn(testRefreshToken);

        // When
        Map<String, Object> result = authService.registerAndAuthenticate("new@example.com", validPassword, "New", "User", "11144477735", request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("accessToken")).isEqualTo("access-token");
        verify(userRepository).findByEmail("new@example.com");
        verify(userRepository).existsByCpf("11144477735");
        verify(passwordEncoder).encode(validPassword);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should refresh access token successfully")
    void refreshAccessToken_Success() {
        // Given
        String newAccessToken = "new-access-token";
        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setToken("new-refresh-token");
        newRefreshToken.setExpiresAt(LocalDateTime.now().plusDays(30));
        
        when(refreshTokenService.findValidRefreshToken("refresh-token-123"))
                .thenReturn(Optional.of(testRefreshToken));
        when(jwtService.generateAccessToken(testUser)).thenReturn(newAccessToken);
        when(refreshTokenService.createRefreshToken(testUser, request)).thenReturn(newRefreshToken);

        // When
        Map<String, Object> result = authService.refreshAccessToken("refresh-token-123", request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("accessToken")).isEqualTo(newAccessToken);
        assertThat(result.get("refreshToken")).isEqualTo("new-refresh-token");
        assertThat(result.get("tokenType")).isEqualTo("Bearer");
        
        verify(refreshTokenService).revokeRefreshToken("refresh-token-123");
    }

    @Test
    @DisplayName("Should throw exception when refresh token is invalid")
    void refreshAccessToken_InvalidToken() {
        // Given
        when(refreshTokenService.findValidRefreshToken("invalid-token"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.refreshAccessToken("invalid-token", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refresh token inválido ou expirado");
    }

    @Test
    @DisplayName("Should change password successfully")
    void changePassword_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        authService.changePassword("test@example.com", "currentPassword", "newPassword");

        // Then
        verify(passwordEncoder).matches("currentPassword", "encodedPassword");
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw exception when current password is incorrect")
    void changePassword_IncorrectCurrentPassword() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.changePassword("test@example.com", "wrongPassword", "newPassword"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Senha atual incorreta");
    }

    @Test
    @DisplayName("Should throw exception when user not found for password change")
    void changePassword_UserNotFound() {
        // Given
        String validPassword = "Password123!";
        String newValidPassword = "NewPassword456!";
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.changePassword("nonexistent@example.com", validPassword, newValidPassword))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Usuário não encontrado: nonexistent@example.com");
    }

    @Test
    @DisplayName("Should set user enabled status")
    void setUserEnabled_Success() {
        // When
        authService.setUserEnabled(1L, false);

        // Then
        verify(userRepository).updateUserStatus(1L, false);
    }

    @Test
    @DisplayName("Should find user by ID")
    void findById_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = authService.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should find active users")
    void findActiveUsers_Success() {
        // Given
        List<User> activeUsers = Arrays.asList(testUser);
        when(userRepository.findByEnabledTrue()).thenReturn(activeUsers);

        // When
        List<User> result = authService.findActiveUsers();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("test@example.com");
        verify(userRepository).findByEnabledTrue();
    }

    @Test
    @DisplayName("Should search users by term")
    void searchUsers_Success() {
        // Given
        List<User> searchResults = Arrays.asList(testUser);
        when(userRepository.searchUsers("test")).thenReturn(searchResults);

        // When
        List<User> result = authService.searchUsers("test");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("test@example.com");
        verify(userRepository).searchUsers("test");
    }

    @Test
    @DisplayName("Should validate token for user successfully")
    void isTokenValidForUser_Success() {
        // Given
        String token = "valid-token";
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenValid(token, testUser)).thenReturn(true);

        // When
        boolean result = authService.isTokenValidForUser(token, "test@example.com");

        // Then
        assertThat(result).isTrue();
        verify(jwtService).isTokenValid(token, testUser);
    }

    @Test
    @DisplayName("Should return false when user not found for token validation")
    void isTokenValidForUser_UserNotFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When
        boolean result = authService.isTokenValidForUser("token", "nonexistent@example.com");

        // Then
        assertThat(result).isFalse();
        verify(jwtService, never()).isTokenValid(anyString(), any(User.class));
    }

    @Test
    @DisplayName("Should return false when user is disabled")
    void isTokenValidForUser_UserDisabled() {
        // Given
        testUser.setEnabled(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        boolean result = authService.isTokenValidForUser("token", "test@example.com");

        // Then
        assertThat(result).isFalse();
        verify(jwtService, never()).isTokenValid(anyString(), any(User.class));
    }

    @Test
    @DisplayName("Should handle exception during token validation")
    void isTokenValidForUser_Exception() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenValid(anyString(), any(User.class)))
                .thenThrow(new RuntimeException("Token validation error"));

        // When
        boolean result = authService.isTokenValidForUser("token", "test@example.com");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should logout user with single token revocation")
    void logout_SingleToken() {
        // Given
        when(refreshTokenService.findValidRefreshToken("refresh-token-123"))
                .thenReturn(Optional.of(testRefreshToken));

        // When
        authService.logout("refresh-token-123", false);

        // Then
        verify(refreshTokenService).revokeRefreshToken("refresh-token-123");
        verify(refreshTokenService, never()).revokeAllUserTokens(any(User.class));
    }

    @Test
    @DisplayName("Should logout user with all tokens revocation")
    void logout_AllTokens() {
        // Given
        when(refreshTokenService.findValidRefreshToken("refresh-token-123"))
                .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenService.revokeAllUserTokens(testUser)).thenReturn(3);

        // When
        authService.logout("refresh-token-123", true);

        // Then
        verify(refreshTokenService).revokeAllUserTokens(testUser);
        verify(refreshTokenService, never()).revokeRefreshToken(anyString());
    }

    @Test
    @DisplayName("Should handle logout with null refresh token")
    void logout_NullRefreshToken() {
        // When
        authService.logout(null, false);

        // Then
        verify(refreshTokenService, never()).findValidRefreshToken(anyString());
        verify(refreshTokenService, never()).revokeRefreshToken(anyString());
        verify(refreshTokenService, never()).revokeAllUserTokens(any(User.class));
    }

    @Test
    @DisplayName("Should handle logout with invalid refresh token")
    void logout_InvalidRefreshToken() {
        // Given
        when(refreshTokenService.findValidRefreshToken("invalid-token"))
                .thenReturn(Optional.empty());

        // When
        authService.logout("invalid-token", false);

        // Then
        verify(refreshTokenService).findValidRefreshToken("invalid-token");
        verify(refreshTokenService, never()).revokeRefreshToken(anyString());
        verify(refreshTokenService, never()).revokeAllUserTokens(any(User.class));
    }

    @Test
    @DisplayName("Should get user statistics")
    void getUserStatistics_Success() {
        // Given
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByEnabledTrue()).thenReturn(85L);
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(5L);
        when(userRepository.countByRole(UserRole.USER)).thenReturn(95L);

        // When
        Map<String, Object> result = authService.getUserStatisticsAsMap();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("totalUsers")).isEqualTo(100L);
        assertThat(result.get("activeUsers")).isEqualTo(85L);
        assertThat(result.get("adminUsers")).isEqualTo(5L);
        assertThat(result.get("regularUsers")).isEqualTo(95L);
        
        verify(userRepository).count();
        verify(userRepository).countByEnabledTrue();
        verify(userRepository).countByRole(UserRole.ADMIN);
        verify(userRepository).countByRole(UserRole.USER);
    }

    @Nested
    @DisplayName("CPF Operations Tests")
    class CpfOperationsTests {

        @Test
        @DisplayName("Should find user by CPF successfully")
        void findByCpf_Success() {
            // Given
            String cpf = "12345678909";
            when(userRepository.findByCpf(cpf)).thenReturn(Optional.of(testUser));

            // When
            Optional<User> result = authService.findByCpf(cpf);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(testUser);
            verify(userRepository).findByCpf(cpf);
        }

        @Test
        @DisplayName("Should return empty when CPF not found")
        void findByCpf_NotFound() {
            // Given
            String cpf = "99999999999";
            when(userRepository.findByCpf(cpf)).thenReturn(Optional.empty());

            // When
            Optional<User> result = authService.findByCpf(cpf);

            // Then
            assertThat(result).isEmpty();
            verify(userRepository).findByCpf(cpf);
        }

        @Test
        @DisplayName("Should register user with valid CPF")
        void register_WithValidCpf_Success() {
            // Given
            String email = "new@test.com";
            String password = "Password123!";
            String firstName = "New";
            String lastName = "User";
            String cpf = "12345678909";

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(userRepository.existsByCpf(cpf)).thenReturn(false);
            when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            User result = authService.register(email, password, firstName, lastName, cpf);

            // Then
            assertThat(result).isNotNull();
            verify(userRepository).findByEmail(email);
            verify(userRepository).existsByCpf(cpf);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when CPF already exists")
        void register_CpfExists_ThrowsException() {
            // Given
            String email = "new@test.com";
            String password = "Password123!";
            String firstName = "New";
            String lastName = "User";
            String cpf = "12345678909";

            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(userRepository.existsByCpf(cpf)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> authService.register(email, password, firstName, lastName, cpf))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("CPF já está em uso");
        }
    }

    @Nested
    @DisplayName("User Role Management Tests")
    class UserRoleManagementTests {

        @Test
        @DisplayName("Should update user role successfully")
        void updateUserRole_Success() {
            // Given
            Long userId = 1L;
            UserRole newRole = UserRole.ADMIN;
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            User result = authService.updateUserRole(userId, newRole);

            // Then
            assertThat(result).isNotNull();
            verify(userRepository).findById(userId);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found for role update")
        void updateUserRole_UserNotFound() {
            // Given
            Long userId = 999L;
            UserRole newRole = UserRole.ADMIN;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> authService.updateUserRole(userId, newRole))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuário não encontrado");
        }
    }

    @Nested
    @DisplayName("Search and Filter Tests")
    class SearchAndFilterTests {

        @Test
        @DisplayName("Should search users by term")
        void searchUsers_ByTerm_Success() {
            // Given
            String searchTerm = "test";
            List<User> expectedUsers = Arrays.asList(testUser);
            when(userRepository.searchUsers(searchTerm)).thenReturn(expectedUsers);

            // When
            List<User> result = authService.searchUsers(searchTerm);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).contains(testUser);
        }

        @Test
        @DisplayName("Should return empty list when no users match search")
        void searchUsers_NoMatches_ReturnsEmptyList() {
            // Given
            String searchTerm = "nonexistent";
            when(userRepository.searchUsers(searchTerm)).thenReturn(Collections.emptyList());

            // When
            List<User> result = authService.searchUsers(searchTerm);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should find active users only")
        void findActiveUsers_Success() {
            // Given
            List<User> activeUsers = Arrays.asList(testUser);
            when(userRepository.findByEnabledTrue()).thenReturn(activeUsers);

            // When
            List<User> result = authService.findActiveUsers();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).contains(testUser);
            verify(userRepository).findByEnabledTrue();
        }
    }

    @Nested
    @DisplayName("Authentication Edge Cases Tests")
    class AuthenticationEdgeCasesTests {

        @Test
        @DisplayName("Should handle authentication with disabled user")
        void authenticate_DisabledUser_ThrowsException() {
            // Given
            String email = "test@test.com";
            String validPassword = "Password123!";
            testUser.setEnabled(false);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new DisabledException("User account is disabled"));

            // When & Then
            assertThatThrownBy(() -> authService.authenticate(email, validPassword, request))
                    .isInstanceOf(DisabledException.class)
                    .hasMessageContaining("disabled");
        }

        @Test
        @DisplayName("Should handle null or empty credentials")
        void authenticate_NullCredentials_ThrowsException() {
            // Given
            String validPassword = "Password123!";
            
            // When & Then
            assertThatThrownBy(() -> authService.authenticate(null, validPassword, request))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> authService.authenticate("email", null, request))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> authService.authenticate("", validPassword, request))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> authService.authenticate("email", "", request))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate token for enabled user")
        void isTokenValidForUser_EnabledUser_ReturnsTrue() {
            // Given
            String token = "valid-token";
            String email = "test@example.com";
            testUser.setEnabled(true);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
            when(jwtService.isTokenValid(token, testUser)).thenReturn(true);

            // When
            boolean result = authService.isTokenValidForUser(token, email);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for token with disabled user")
        void isTokenValidForUser_DisabledUser_ReturnsFalse() {
            // Given
            String token = "valid.jwt.token";
            String email = "test@example.com";
            testUser.setEnabled(false);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

            // When
            boolean result = authService.isTokenValidForUser(token, email);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for invalid token")
        void isTokenValidForUser_InvalidToken_ReturnsFalse() {
            // Given
            String token = "invalid.jwt.token";
            String email = "test@example.com";

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
            when(jwtService.isTokenValid(token, testUser)).thenReturn(false);

            // When
            boolean result = authService.isTokenValidForUser(token, email);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Email Activation Tests")
    class EmailActivationTests {

        @Test
        @DisplayName("REGRA CRÍTICA: Deve obrigatoriamente enviar email de ativação após cadastro de usuário comum")
        void shouldMandatorilySendActivationEmailAfterUserRegistration() {
            // Given
            String email = "newuser@example.com";
            String password = "Password123!";
            String firstName = "New";
            String lastName = "User";
            String cpf = "11144477735";
            
            User newUser = new User();
            newUser.setId(2L);
            newUser.setEmail(email);
            newUser.setPassword("encodedPassword");
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setCpf(cpf);
            newUser.setRole(UserRole.USER);
            newUser.setEmailVerified(false);
            
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(userRepository.existsByCpf(cpf)).thenReturn(false);
            when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(newUser);
            when(emailVerificationService.generateVerificationToken(any(User.class))).thenReturn("verification-token-123");

            // When
            User registeredUser = authService.register(email, password, firstName, lastName, cpf);

            // Then
            assertThat(registeredUser).isNotNull();
            assertThat(registeredUser.getRole()).isEqualTo(UserRole.USER);
            assertThat(registeredUser.isEmailVerified()).isFalse();
            
            // REGRA CRÍTICA: Verificar que o email de ativação foi enviado obrigatoriamente
            verify(emailVerificationService, times(1)).generateVerificationToken(any(User.class));
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Deve NÃO enviar email de ativação para usuário ADMIN")
        void shouldNotSendActivationEmailForAdminUser() {
            // Given
            String email = "admin@example.com";
            String password = "AdminPassword123!";
            String firstName = "Admin";
            String lastName = "User";
            String cpf = "52998224725";
            
            User adminUser = new User();
            adminUser.setId(3L);
            adminUser.setEmail(email);
            adminUser.setPassword("encodedPassword");
            adminUser.setFirstName(firstName);
            adminUser.setLastName(lastName);
            adminUser.setCpf(cpf);
            adminUser.setRole(UserRole.ADMIN);
            adminUser.setEmailVerified(true); // Admin já vem verificado
            
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(userRepository.existsByCpf(cpf)).thenReturn(false);
            when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(adminUser);

            // When
            User registeredUser = authService.register(email, password, firstName, lastName, cpf);

            // Then
            assertThat(registeredUser).isNotNull();
            assertThat(registeredUser.getRole()).isEqualTo(UserRole.ADMIN);
            assertThat(registeredUser.isEmailVerified()).isTrue();
            
            // Verificar que o email de ativação NÃO foi enviado para admin
            verify(emailVerificationService, never()).generateVerificationToken(any(User.class));
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("REGRA CRÍTICA: Cadastro deve falhar se envio de email falhar para usuário comum")
        void shouldFailRegistrationIfEmailSendingFailsForRegularUser() {
            // Given
            String email = "failuser@example.com";
            String password = "Password123!";
            String firstName = "Fail";
            String lastName = "User";
            String cpf = "11122233344";
            
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(userRepository.existsByCpf(cpf)).thenReturn(false);
            when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
            
            // Simular falha no envio de email
            when(emailVerificationService.generateVerificationToken(any(User.class)))
                .thenThrow(new RuntimeException("Falha no envio de email"));

            // When & Then
            assertThatThrownBy(() -> authService.register(email, password, firstName, lastName, cpf))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha no envio de email");
            
            // Verificar que tentou enviar o email
            verify(emailVerificationService, times(1)).generateVerificationToken(any(User.class));
        }

        @Test
        @DisplayName("REGRA CRÍTICA: registerAndAuthenticate deve enviar email de ativação mesmo com autenticação automática")
        void shouldSendActivationEmailEvenWithAutoAuthentication() {
            // Given
            String email = "autoauth@example.com";
            String password = "Password123!";
            String firstName = "Auto";
            String lastName = "Auth";
            String cpf = "12345678909";
            
            User newUser = new User();
            newUser.setId(4L);
            newUser.setEmail(email);
            newUser.setPassword("encodedPassword");
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setCpf(cpf);
            newUser.setRole(UserRole.USER);
            newUser.setEmailVerified(false);
            
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(userRepository.existsByCpf(cpf)).thenReturn(false);
            when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(newUser);
            when(userRepository.findById(4L)).thenReturn(Optional.of(newUser)); // Mock para updateLastLogin
            when(emailVerificationService.generateVerificationToken(any(User.class))).thenReturn("verification-token-456");
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(newUser);
            when(jwtService.generateAccessToken(newUser)).thenReturn("access-token");
            when(refreshTokenService.createRefreshToken(newUser, request)).thenReturn(testRefreshToken);

            // When
            Map<String, Object> result = authService.registerAndAuthenticate(email, password, firstName, lastName, cpf, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("accessToken")).isEqualTo("access-token");
            
            // REGRA CRÍTICA: Verificar que o email de ativação foi enviado mesmo com autenticação automática
            verify(emailVerificationService, times(1)).generateVerificationToken(any(User.class));
            verify(userRepository, times(2)).save(any(User.class)); // 1x no register + 1x no updateLastLogin
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should get user statistics as map")
        void getUserStatisticsAsMap_Success() {
            // Given
            when(userRepository.count()).thenReturn(100L);
            when(userRepository.countByEnabledTrue()).thenReturn(85L);
            when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(5L);
            when(userRepository.countByRole(UserRole.USER)).thenReturn(95L);

            // When
            Map<String, Object> result = authService.getUserStatisticsAsMap();

            // Then
            assertThat(result).containsEntry("totalUsers", 100L);
            assertThat(result).containsEntry("activeUsers", 85L);
            assertThat(result).containsEntry("adminUsers", 5L);
            assertThat(result).containsEntry("regularUsers", 95L);
            assertThat(result).containsKey("lastUpdated");
        }
    }
}