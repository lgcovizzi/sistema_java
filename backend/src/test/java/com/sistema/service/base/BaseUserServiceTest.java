package com.sistema.service.base;

import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para BaseUserService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BaseUserService Tests")
class BaseUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private TestableBaseUserService baseUserService;

    @BeforeEach
    void setUp() {
        baseUserService = new TestableBaseUserService();
        baseUserService.setUserRepository(userRepository);
        baseUserService.setPasswordEncoder(passwordEncoder);
    }

    @Nested
    @DisplayName("User Retrieval Tests")
    class UserRetrievalTests {

        @Test
        @DisplayName("Should find user by ID required successfully")
        void shouldFindUserByIdRequiredSuccessfully() {
            // Given
            User user = createTestUser();
            user.setId(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // When
            User result = baseUserService.findUserByIdRequired(1L);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.getId());
            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when user not found by ID required")
        void shouldThrowExceptionWhenUserNotFoundByIdRequired() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                baseUserService.findUserByIdRequired(1L)
            );

            assertTrue(exception.getMessage().contains("Usuário não encontrado"));
            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("Should find user by email required successfully")
        void shouldFindUserByEmailRequiredSuccessfully() {
            // Given
            User user = createTestUser();
            when(userRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(user));

            // When
            User result = baseUserService.findUserByEmailRequired("joao@example.com");

            // Then
            assertNotNull(result);
            assertEquals("joao@example.com", result.getEmail());
            verify(userRepository).findByEmail("joao@example.com");
        }

        @Test
        @DisplayName("Should throw exception when user not found by email required")
        void shouldThrowExceptionWhenUserNotFoundByEmailRequired() {
            // Given
            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                baseUserService.findUserByEmailRequired("notfound@example.com")
            );

            assertTrue(exception.getMessage().contains("Usuário não encontrado"));
            verify(userRepository).findByEmail("notfound@example.com");
        }

        @Test
        @DisplayName("Should find user by email")
        void shouldFindUserByEmail() {
            // Given
            User user = createTestUser();
            when(userRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(user));

            // When
            Optional<User> result = baseUserService.findUserByEmail("joao@example.com");

            // Then
            assertTrue(result.isPresent());
            assertEquals("joao@example.com", result.get().getEmail());
            verify(userRepository).findByEmail("joao@example.com");
        }

        @Test
        @DisplayName("Should return empty when user not found by email")
        void shouldReturnEmptyWhenUserNotFoundByEmail() {
            // Given
            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            // When
            Optional<User> result = baseUserService.findUserByEmail("notfound@example.com");

            // Then
            assertFalse(result.isPresent());
            verify(userRepository).findByEmail("notfound@example.com");
        }

        @Test
        @DisplayName("Should throw exception when email is null for search")
        void shouldThrowExceptionWhenEmailIsNullForSearch() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                baseUserService.findUserByEmail(null)
            );

            assertTrue(exception.getMessage().contains("email"));
        }
    }

    @Nested
    @DisplayName("Email Validation Tests")
    class EmailValidationTests {

        @Test
        @DisplayName("Should validate email not in use when email is available")
        void shouldValidateEmailNotInUseWhenEmailIsAvailable() {
            // Given
            when(userRepository.findByEmail("available@example.com")).thenReturn(Optional.empty());

            // When & Then
            assertDoesNotThrow(() -> 
                baseUserService.validateEmailNotInUse("available@example.com", null)
            );
            verify(userRepository).findByEmail("available@example.com");
        }

        @Test
        @DisplayName("Should throw exception when email is already in use")
        void shouldThrowExceptionWhenEmailIsAlreadyInUse() {
            // Given
            User existingUser = createTestUser();
            existingUser.setId(1L);
            when(userRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(existingUser));

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                baseUserService.validateEmailNotInUse("joao@example.com", 2L)
            );

            assertTrue(exception.getMessage().contains("Email já está em uso"));
            verify(userRepository).findByEmail("joao@example.com");
        }

        @Test
        @DisplayName("Should allow email for same user during update")
        void shouldAllowEmailForSameUserDuringUpdate() {
            // Given
            User existingUser = createTestUser();
            existingUser.setId(1L);
            when(userRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(existingUser));

            // When & Then
            assertDoesNotThrow(() -> 
                baseUserService.validateEmailNotInUse("joao@example.com", 1L)
            );
            verify(userRepository).findByEmail("joao@example.com");
        }
    }

    @Nested
    @DisplayName("Password Validation Tests")
    class PasswordValidationTests {

        @Test
        @DisplayName("Should validate strong password")
        void shouldValidateStrongPassword() {
            // Given
            String strongPassword = "StrongPass123!";

            // When & Then
            assertDoesNotThrow(() -> 
                baseUserService.validatePasswordStrength(strongPassword)
            );
        }

        @Test
        @DisplayName("Should throw exception for short password")
        void shouldThrowExceptionForShortPassword() {
            // Given
            String shortPassword = "Short1!";

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                baseUserService.validatePasswordStrength(shortPassword)
            );

            assertTrue(exception.getMessage().contains("pelo menos 8 caracteres"));
        }

        @Test
        @DisplayName("Should throw exception for password without uppercase")
        void shouldThrowExceptionForPasswordWithoutUppercase() {
            // Given
            String password = "lowercase123!";

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                baseUserService.validatePasswordStrength(password)
            );

            assertTrue(exception.getMessage().contains("letra maiúscula"));
        }

        @Test
        @DisplayName("Should throw exception for password without lowercase")
        void shouldThrowExceptionForPasswordWithoutLowercase() {
            // Given
            String password = "UPPERCASE123!";

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                baseUserService.validatePasswordStrength(password)
            );

            assertTrue(exception.getMessage().contains("letra minúscula"));
        }

        @Test
        @DisplayName("Should throw exception for password without number")
        void shouldThrowExceptionForPasswordWithoutNumber() {
            // Given
            String password = "NoNumberPass!";

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                baseUserService.validatePasswordStrength(password)
            );

            assertTrue(exception.getMessage().contains("número"));
        }

        @Test
        @DisplayName("Should throw exception for password without special character")
        void shouldThrowExceptionForPasswordWithoutSpecialCharacter() {
            // Given
            String password = "NoSpecialChar123";

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                baseUserService.validatePasswordStrength(password)
            );

            assertTrue(exception.getMessage().contains("caractere especial"));
        }
    }

    @Nested
    @DisplayName("Password Encoding Tests")
    class PasswordEncodingTests {

        @Test
        @DisplayName("Should encode password")
        void shouldEncodePassword() {
            // Given
            String rawPassword = "password123";
            String encodedPassword = "encoded_password";
            when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

            // When
            String result = baseUserService.encodePassword(rawPassword);

            // Then
            assertEquals(encodedPassword, result);
            verify(passwordEncoder).encode(rawPassword);
        }

        @Test
        @DisplayName("Should match password correctly")
        void shouldMatchPasswordCorrectly() {
            // Given
            String rawPassword = "password123";
            String encodedPassword = "encoded_password";
            when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

            // When
            boolean result = baseUserService.matchesPassword(rawPassword, encodedPassword);

            // Then
            assertTrue(result);
            verify(passwordEncoder).matches(rawPassword, encodedPassword);
        }

        @Test
        @DisplayName("Should not match incorrect password")
        void shouldNotMatchIncorrectPassword() {
            // Given
            String rawPassword = "wrongpassword";
            String encodedPassword = "encoded_password";
            when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

            // When
            boolean result = baseUserService.matchesPassword(rawPassword, encodedPassword);

            // Then
            assertFalse(result);
            verify(passwordEncoder).matches(rawPassword, encodedPassword);
        }

        @Test
        @DisplayName("Should throw exception when encoding null password")
        void shouldThrowExceptionWhenEncodingNullPassword() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                baseUserService.encodePassword(null)
            );

            assertTrue(exception.getMessage().contains("rawPassword"));
        }
    }

    @Nested
    @DisplayName("User Update Tests")
    class UserUpdateTests {

        @Test
        @DisplayName("Should update last login")
        void shouldUpdateLastLogin() {
            // Given
            User user = createTestUser();
            user.setId(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            baseUserService.updateLastLogin(1L);

            // Then
            verify(userRepository).findById(1L);
            verify(userRepository).save(user);
            assertNotNull(user.getLastLogin());
        }

        @Test
        @DisplayName("Should throw exception when user not found for last login update")
        void shouldThrowExceptionWhenUserNotFoundForLastLoginUpdate() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                baseUserService.updateLastLogin(1L)
            );

            assertTrue(exception.getMessage().contains("Usuário não encontrado"));
            verify(userRepository).findById(1L);
        }
    }

    @Nested
    @DisplayName("User Validation Tests")
    class UserValidationTests {

        @Test
        @DisplayName("Should validate active user")
        void shouldValidateActiveUser() {
            // Given
            User user = createTestUser();
            user.setEnabled(true);

            // When & Then
            assertDoesNotThrow(() -> 
                baseUserService.validateUserActive(user)
            );
        }

        @Test
        @DisplayName("Should throw exception for inactive user")
        void shouldThrowExceptionForInactiveUser() {
            // Given
            User user = createTestUser();
            user.setEnabled(false);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                baseUserService.validateUserActive(user)
            );

            assertTrue(exception.getMessage().contains("inativo"));
        }

        @Test
        @DisplayName("Should validate user role")
        void shouldValidateUserRole() {
            // Given
            User user = createTestUser();
            user.setRole(UserRole.ADMIN);

            // When & Then
            assertDoesNotThrow(() -> 
                baseUserService.validateUserRole(user, UserRole.ADMIN)
            );
        }

        @Test
        @DisplayName("Should throw exception for incorrect user role")
        void shouldThrowExceptionForIncorrectUserRole() {
            // Given
            User user = createTestUser();
            user.setRole(UserRole.USER);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                baseUserService.validateUserRole(user, UserRole.ADMIN)
            );

            assertTrue(exception.getMessage().contains("permissão"));
        }
    }

    @Nested
    @DisplayName("User Statistics Tests")
    class UserStatisticsTests {

        @Test
        @DisplayName("Should get user statistics")
        void shouldGetUserStatistics() {
            // Given
            when(userRepository.count()).thenReturn(100L);
            when(userRepository.countByEnabledTrue()).thenReturn(80L);
            when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(5L);
            when(userRepository.countByRole(UserRole.USER)).thenReturn(95L);

            // When
            BaseUserService.UserStatistics stats = baseUserService.getUserStatistics();

            // Then
            assertNotNull(stats);
            assertEquals(100L, stats.getTotalUsers());
            assertEquals(80L, stats.getActiveUsers());
            assertEquals(5L, stats.getAdminUsers());
            assertEquals(95L, stats.getRegularUsers());
            assertEquals(20L, stats.getInactiveUsers());
            assertEquals(80.0, stats.getActiveUserPercentage());
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
     * Implementação testável de BaseUserService para permitir testes.
     */
    private static class TestableBaseUserService extends BaseUserService {
        // Expõe métodos protected para teste
        
        @Override
        public User findUserByIdRequired(Long id) {
            return super.findUserByIdRequired(id);
        }
        
        @Override
        public User findUserByEmailRequired(String email) {
            return super.findUserByEmailRequired(email);
        }
        
        @Override
        public Optional<User> findUserByEmail(String email) {
            return super.findUserByEmail(email);
        }
        
        @Override
        public void validateEmailNotInUse(String email, Long excludeUserId) {
            super.validateEmailNotInUse(email, excludeUserId);
        }
        
        @Override
        public void validatePasswordStrength(String password) {
            super.validatePasswordStrength(password);
        }
        
        @Override
        public String encodePassword(String rawPassword) {
            return super.encodePassword(rawPassword);
        }
        
        @Override
        public boolean matchesPassword(String rawPassword, String encodedPassword) {
            return super.matchesPassword(rawPassword, encodedPassword);
        }
        
        @Override
        public void updateLastLogin(Long userId) {
            super.updateLastLogin(userId);
        }
        
        @Override
        public void validateUserActive(User user) {
            super.validateUserActive(user);
        }
        
        @Override
        public void validateUserRole(User user, UserRole requiredRole) {
            super.validateUserRole(user, requiredRole);
        }
        
        @Override
        public UserStatistics getUserStatistics() {
            return super.getUserStatistics();
        }
    }
}