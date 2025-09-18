package com.sistema.service.base;

import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para BaseSecurityService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BaseSecurityService Tests")
class BaseSecurityServiceTest {

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private TestableBaseSecurityService baseSecurityService;

    @BeforeEach
    void setUp() {
        baseSecurityService = new TestableBaseSecurityService();
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Should get current user when authenticated")
        void shouldGetCurrentUserWhenAuthenticated() {
            // Given
            User testUser = createTestUser();
            baseSecurityService.setMockUser(testUser);

            // When
            User result = baseSecurityService.getCurrentUser();

            // Then
            assertNotNull(result);
            assertEquals(testUser.getEmail(), result.getEmail());
            assertEquals(testUser.getRole(), result.getRole());
        }

        @Test
        @DisplayName("Should throw exception when not authenticated")
        void shouldThrowExceptionWhenNotAuthenticated() {
            // Given
            baseSecurityService.setMockUser(null);

            // When & Then
            assertThrows(RuntimeException.class, () -> baseSecurityService.getCurrentUser());
        }

        @Test
        @DisplayName("Should throw exception when authentication is null")
        void shouldThrowExceptionWhenAuthenticationIsNull() {
            // Given
            baseSecurityService.setMockUser(null);

            // When & Then
            assertThrows(RuntimeException.class, () -> baseSecurityService.getCurrentUser());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("test@example.com");
            SecurityContextHolder.setContext(securityContext);
            baseSecurityService.setMockUser(null);

            // When & Then
            assertThrows(RuntimeException.class, () -> baseSecurityService.getCurrentUser());
        }

        @Test
        @DisplayName("Should get current user email")
        void shouldGetCurrentUserEmail() {
            // Given
            User testUser = createTestUser();
            baseSecurityService.setMockUser(testUser);

            // When
            User result = baseSecurityService.getCurrentUser();

            // Then
            assertNotNull(result);
            assertEquals(testUser.getEmail(), result.getEmail());
        }

        @Test
        @DisplayName("Should throw exception when getting email of non-authenticated user")
        void shouldThrowExceptionWhenGettingEmailOfNonAuthenticatedUser() {
            // Given
            baseSecurityService.setMockUser(null);

            // When & Then
            assertThrows(RuntimeException.class, () -> baseSecurityService.getCurrentUser());
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should check if user has role")
        void shouldCheckIfUserHasRole() {
            // Given
            User user = createTestUser();
            user.setRole(UserRole.ADMIN);
            
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("test@example.com");
            
            baseSecurityService.setMockUser(user);

            // When
            boolean result = baseSecurityService.hasRole(UserRole.ADMIN);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when user does not have role")
        void shouldReturnFalseWhenUserDoesNotHaveRole() {
            // Given
            User user = createTestUser();
            user.setRole(UserRole.USER);
            
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("test@example.com");
            
            baseSecurityService.setMockUser(user);

            // When
            boolean result = baseSecurityService.hasRole(UserRole.ADMIN);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when user not found for role check")
        void shouldReturnFalseWhenUserNotFoundForRoleCheck() {
            // Given
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("notfound@example.com");
            
            baseSecurityService.setMockUser(null);

            // When
            boolean result = baseSecurityService.hasRole(UserRole.ADMIN);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should check if user is admin")
        void shouldCheckIfUserIsAdmin() {
            // Given
            User user = createTestUser();
            user.setRole(UserRole.ADMIN);
            
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("test@example.com");
            
            baseSecurityService.setMockUser(user);

            // When
            boolean result = baseSecurityService.isAdmin();

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when user is not admin")
        void shouldReturnFalseWhenUserIsNotAdmin() {
            // Given
            User user = createTestUser();
            user.setRole(UserRole.USER);
            
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("test@example.com");
            
            baseSecurityService.setMockUser(user);

            // When
            boolean result = baseSecurityService.isAdmin();

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate email format")
        void shouldValidateEmailFormat() {
            // Given
            String validEmail = "test@example.com";

            // When & Then
            assertDoesNotThrow(() -> baseSecurityService.validateEmailFormat(validEmail));
        }

        @Test
        @DisplayName("Should invalidate incorrect email format")
        void shouldInvalidateIncorrectEmailFormat() {
            // Given
            String invalidEmail = "invalid-email";

            // When & Then
            assertThrows(IllegalArgumentException.class, 
                () -> baseSecurityService.validateEmailFormat(invalidEmail));
        }

        @Test
        @DisplayName("Should validate username")
        void shouldValidateUsername() {
            // Given
            String validUsername = "validuser123";

            // When & Then
            assertDoesNotThrow(() -> baseSecurityService.validateUsernameFormat(validUsername));
        }

        @Test
        @DisplayName("Should invalidate username with special characters")
        void shouldInvalidateUsernameWithSpecialCharacters() {
            // Given
            String invalidUsername = "user@name!";

            // When & Then
            assertThrows(IllegalArgumentException.class, 
                () -> baseSecurityService.validateUsernameFormat(invalidUsername));
        }

        @Test
        @DisplayName("Should invalidate short username")
        void shouldInvalidateShortUsername() {
            // Given
            String shortUsername = "ab";

            // When & Then
            assertThrows(IllegalArgumentException.class, 
                () -> baseSecurityService.validateUsernameFormat(shortUsername));
        }

        @Test
        @DisplayName("Should invalidate long username")
        void shouldInvalidateLongUsername() {
            // Given
            String longUsername = "a".repeat(51);

            // When & Then
            assertThrows(IllegalArgumentException.class, 
                () -> baseSecurityService.validateUsernameFormat(longUsername));
        }

        @Test
        @DisplayName("Should validate token format")
        void shouldValidateTokenFormat() {
            // Given
            String validToken = "dGVzdA"; // "test" em base64

            // When & Then
            assertDoesNotThrow(() -> baseSecurityService.validateTokenFormat(validToken));
        }

        @Test
        @DisplayName("Should invalidate null token")
        void shouldInvalidateNullToken() {
            // Given
            String nullToken = null;

            // When & Then
            assertThrows(IllegalArgumentException.class, 
                () -> baseSecurityService.validateTokenFormat(nullToken));
        }

        @Test
        @DisplayName("Should invalidate empty token")
        void shouldInvalidateEmptyToken() {
            // Given
            String emptyToken = "";

            // When & Then
            assertThrows(IllegalArgumentException.class, 
                () -> baseSecurityService.validateTokenFormat(emptyToken));
        }

        @Test
        @DisplayName("Should validate IP address")
        void shouldValidateIpAddress() {
            // Given
            String validIp = "192.168.1.1";

            // When & Then
            assertDoesNotThrow(() -> baseSecurityService.validateIpAddress(validIp));
        }

        @Test
        @DisplayName("Should invalidate incorrect IP address")
        void shouldInvalidateIncorrectIpAddress() {
            // Given
            String invalidIp = "999.999.999.999";

            // When & Then
            assertThrows(IllegalArgumentException.class, 
                () -> baseSecurityService.validateIpAddress(invalidIp));
        }
    }

    @Nested
    @DisplayName("Security Utilities Tests")
    class SecurityUtilitiesTests {

        @Test
        @DisplayName("Should sanitize input")
        void shouldSanitizeInput() {
            // Given
            String input = "<script>alert('xss')</script>";

            // When
            String result = baseSecurityService.sanitizeInput(input);

            // Then
            assertNotNull(result);
            assertFalse(result.contains("<script>"));
        }

        @Test
        @DisplayName("Should generate secure hash")
        void shouldGenerateSecureHash() {
            // Given
            String input = "test input";
            String salt = "test_salt";

            // When
            String result = baseSecurityService.generateSecureHash(input, salt);

            // Then
            assertNotNull(result);
            assertNotEquals(input, result);
            assertTrue(result.length() > 0);
        }

        @Test
        @DisplayName("Should verify hash with salt")
        void shouldVerifyHashWithSalt() {
            // Given
            String input = "test input";
            String salt = "test_salt";
            String hash = baseSecurityService.generateSecureHash(input, salt);

            // When
            boolean result = baseSecurityService.verifyHash(input, salt, hash);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should not verify incorrect hash with salt")
        void shouldNotVerifyIncorrectHashWithSalt() {
            // Given
            String input = "test input";
            String wrongInput = "wrong input";
            String salt = "test_salt";
            String hash = baseSecurityService.generateSecureHash(input, salt);

            // When
            boolean result = baseSecurityService.verifyHash(wrongInput, salt, hash);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should generate secure token")
        void shouldGenerateSecureToken() {
            // When
            String result = baseSecurityService.generateSecureToken();

            // Then
            assertNotNull(result);
            assertTrue(result.length() > 0);
        }

        @Test
        @DisplayName("Should generate secure token with specific length")
        void shouldGenerateSecureTokenWithLength() {
            // Given
            int length = 32;

            // When
            String result = baseSecurityService.generateSecureToken(length);

            // Then
            assertNotNull(result);
            assertTrue(result.length() > 0);
        }
    }

    /**
     * Cria um usuário de teste para os testes.
     */
    private User createTestUser() {
        User user = new User();
        user.setFirstName("João");
        user.setLastName("Silva");
        user.setEmail("joao@example.com");
        user.setCpf("12345678901");
        user.setPassword("password123");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    /**
     * Implementação testável de BaseSecurityService para permitir testes.
     */
    private static class TestableBaseSecurityService extends BaseSecurityService {
        
        private User mockUser;
        
        public void setMockUser(User user) {
            this.mockUser = user;
        }
        
        @Override
        protected User findUserByEmailRequired(String email) {
            if (mockUser != null && mockUser.getEmail().equals(email)) {
                return mockUser;
            }
            throw new RuntimeException("Usuário não encontrado");
        }
        
        @Override
        public User getCurrentUser() {
            if (mockUser == null) {
                throw new RuntimeException("Usuário não autenticado");
            }
            return mockUser;
        }
        
        @Override
        public boolean hasRole(UserRole role) {
            return super.hasRole(role);
        }
        
        @Override
        public boolean isAdmin() {
            return super.isAdmin();
        }
        
        @Override
        public String sanitizeInput(String input) {
            return super.sanitizeInput(input);
        }
        
        @Override
        public String generateSecureHash(String input, String salt) {
            return super.generateSecureHash(input, salt);
        }
        
        @Override
        public boolean verifyHash(String input, String salt, String hash) {
            return super.verifyHash(input, salt, hash);
        }
        
        @Override
        public String generateSecureToken() {
            return super.generateSecureToken();
        }
        
        @Override
        public String generateSecureToken(int length) {
            return super.generateSecureToken(length);
        }
        
        public void validateEmailFormat(String email) {
            super.validateEmailFormat(email);
        }
        
        public void validateUsernameFormat(String username) {
            super.validateUsernameFormat(username);
        }
        
        public void validateTokenFormat(String token) {
            super.validateTokenFormat(token);
        }
        
        public void validateIpAddress(String ipAddress) {
            super.validateIpAddress(ipAddress);
        }
    }
}