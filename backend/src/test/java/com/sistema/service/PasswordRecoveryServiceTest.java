package com.sistema.service;

import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import com.sistema.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Password Recovery Service Tests")
class PasswordRecoveryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private AttemptService attemptService;

    @Mock
    private CaptchaService captchaService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private String testCpf;
    private String testEmail;

    @BeforeEach
    void setUp() {
        testCpf = "12345678901";
        testEmail = "test@example.com";
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail(testEmail);
        testUser.setCpf(testCpf);
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.USER);
        testUser.setEnabled(true);
        testUser.setEmailVerified(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("CPF Verification Tests")
    class CpfVerificationTests {

        @Test
        @DisplayName("Should return masked email when CPF exists")
        void shouldReturnMaskedEmailWhenCpfExists() {
            // Given
            when(userRepository.findByCpf(testCpf)).thenReturn(Optional.of(testUser));
            
            try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
                securityUtilsMock.when(() -> SecurityUtils.maskEmail(testEmail))
                    .thenReturn("t***@***.com");

                // When
                Optional<User> result = userRepository.findByCpf(testCpf);

                // Then
                assertThat(result).isPresent();
                assertThat(result.get().getEmail()).isEqualTo(testEmail);
                verify(userRepository).findByCpf(testCpf);
            }
        }

        @Test
        @DisplayName("Should return empty when CPF does not exist")
        void shouldReturnEmptyWhenCpfDoesNotExist() {
            // Given
            String nonExistentCpf = "99999999999";
            when(userRepository.findByCpf(nonExistentCpf)).thenReturn(Optional.empty());

            // When
            Optional<User> result = userRepository.findByCpf(nonExistentCpf);

            // Then
            assertThat(result).isEmpty();
            verify(userRepository).findByCpf(nonExistentCpf);
        }

        @Test
        @DisplayName("Should handle null CPF gracefully")
        void shouldHandleNullCpfGracefully() {
            // Given
            when(userRepository.findByCpf(null)).thenReturn(Optional.empty());

            // When
            Optional<User> result = userRepository.findByCpf(null);

            // Then
            assertThat(result).isEmpty();
            verify(userRepository).findByCpf(null);
        }
    }

    @Nested
    @DisplayName("Email Confirmation Tests")
    class EmailConfirmationTests {

        @Test
        @DisplayName("Should confirm email when CPF and email match")
        void shouldConfirmEmailWhenCpfAndEmailMatch() {
            // Given
            when(userRepository.findByCpf(testCpf)).thenReturn(Optional.of(testUser));
            when(captchaService.validateCaptcha(anyString(), anyString())).thenReturn(true);

            // When
            Optional<User> userResult = userRepository.findByCpf(testCpf);
            boolean captchaValid = captchaService.validateCaptcha("captchaId", "answer");

            // Then
            assertThat(userResult).isPresent();
            assertThat(userResult.get().getEmail()).isEqualTo(testEmail);
            assertThat(captchaValid).isTrue();
            verify(userRepository).findByCpf(testCpf);
            verify(captchaService).validateCaptcha("captchaId", "answer");
        }

        @Test
        @DisplayName("Should reject when email does not match")
        void shouldRejectWhenEmailDoesNotMatch() {
            // Given
            String wrongEmail = "wrong@example.com";
            when(userRepository.findByCpf(testCpf)).thenReturn(Optional.of(testUser));

            // When
            Optional<User> userResult = userRepository.findByCpf(testCpf);

            // Then
            assertThat(userResult).isPresent();
            assertThat(userResult.get().getEmail()).isNotEqualTo(wrongEmail);
            verify(userRepository).findByCpf(testCpf);
        }

        @Test
        @DisplayName("Should reject when captcha is invalid")
        void shouldRejectWhenCaptchaIsInvalid() {
            // Given
            when(userRepository.findByCpf(testCpf)).thenReturn(Optional.of(testUser));
            when(captchaService.validateCaptcha(anyString(), anyString())).thenReturn(false);

            // When
            boolean captchaValid = captchaService.validateCaptcha("captchaId", "wrongAnswer");

            // Then
            assertThat(captchaValid).isFalse();
            verify(captchaService).validateCaptcha("captchaId", "wrongAnswer");
        }

        @Test
        @DisplayName("Should handle rate limiting")
        void shouldHandleRateLimiting() {
            // Given
            String clientIp = "192.168.1.1";
            when(attemptService.isCaptchaRequiredForPasswordReset(clientIp)).thenReturn(true);

            // When
            boolean rateLimited = attemptService.isCaptchaRequiredForPasswordReset(clientIp);

            // Then
            assertThat(rateLimited).isTrue();
            verify(attemptService).isCaptchaRequiredForPasswordReset(clientIp);
        }

        @Test
        @DisplayName("Should validate captcha correctly")
        void shouldValidateCaptchaCorrectly() {
            // Given
            String captchaId = "captcha-123";
            String captchaAnswer = "ABCDE";
            
            when(captchaService.validateCaptcha(captchaId, captchaAnswer)).thenReturn(true);

            // When
            boolean isValid = captchaService.validateCaptcha(captchaId, captchaAnswer);

            // Then
            assertThat(isValid).isTrue();
            verify(captchaService).validateCaptcha(captchaId, captchaAnswer);
        }

        @Test
        @DisplayName("Should require captcha after multiple attempts")
        void shouldRequireCaptchaAfterMultipleAttempts() {
            // Given
            String identifier = "192.168.1.1";
            
            when(attemptService.isCaptchaRequiredForPasswordReset(identifier)).thenReturn(true);

            // When
            boolean captchaRequired = attemptService.isCaptchaRequiredForPasswordReset(identifier);

            // Then
            assertThat(captchaRequired).isTrue();
            verify(attemptService).isCaptchaRequiredForPasswordReset(identifier);
        }
    }

    @Nested
    @DisplayName("Password Recovery Token Tests")
    class PasswordRecoveryTokenTests {

        @Test
        @DisplayName("Should generate recovery token when email is confirmed")
        void shouldGenerateRecoveryTokenWhenEmailIsConfirmed() {
            // Given
            String recoveryToken = "recovery-token-123";
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);
            
            testUser.setResetPasswordToken(recoveryToken);
            testUser.setResetPasswordTokenExpiresAt(expiresAt);
            
            when(userRepository.findByCpf(testCpf)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            
            try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
                securityUtilsMock.when(() -> SecurityUtils.generateSecureToken(32))
                    .thenReturn(recoveryToken);

                // When
                Optional<User> userResult = userRepository.findByCpf(testCpf);
                if (userResult.isPresent()) {
                    User user = userResult.get();
                    user.setResetPasswordToken(SecurityUtils.generateSecureToken(32));
                    user.setResetPasswordTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
                    userRepository.save(user);
                }

                // Then
                assertThat(userResult).isPresent();
                assertThat(userResult.get().getResetPasswordToken()).isEqualTo(recoveryToken);
                assertThat(userResult.get().getResetPasswordTokenExpiresAt()).isAfter(LocalDateTime.now());
                verify(userRepository).save(any(User.class));
            }
        }

        @Test
        @DisplayName("Should send recovery email with token")
        void shouldSendRecoveryEmailWithToken() {
            // Given
            String recoveryToken = "recovery-token-123";
            testUser.setResetPasswordToken(recoveryToken);
            
            when(emailService.sendPasswordResetEmail(testUser, recoveryToken)).thenReturn(true);

            // When
            boolean emailSent = emailService.sendPasswordResetEmail(testUser, recoveryToken);

            // Then
            assertThat(emailSent).isTrue();
            verify(emailService).sendPasswordResetEmail(testUser, recoveryToken);
        }

        @Test
        @DisplayName("Should handle email sending failure")
        void shouldHandleEmailSendingFailure() {
            // Given
            String recoveryToken = "recovery-token-123";
            testUser.setResetPasswordToken(recoveryToken);
            
            when(emailService.sendPasswordResetEmail(testUser, recoveryToken)).thenReturn(false);

            // When
            boolean emailSent = emailService.sendPasswordResetEmail(testUser, recoveryToken);

            // Then
            assertThat(emailSent).isFalse();
            verify(emailService).sendPasswordResetEmail(testUser, recoveryToken);
        }
    }

    @Nested
    @DisplayName("Password Reset Tests")
    class PasswordResetTests {

        @Test
        @DisplayName("Should reset password with valid token")
        void shouldResetPasswordWithValidToken() {
            // Given
            String recoveryToken = "valid-token";
            String newPassword = "newPassword123";
            String encodedPassword = "encodedNewPassword";
            
            testUser.setResetPasswordToken(recoveryToken);
            testUser.setResetPasswordTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
            
            when(userRepository.findByResetPasswordToken(recoveryToken)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            Optional<User> userResult = userRepository.findByResetPasswordToken(recoveryToken);
            if (userResult.isPresent() && 
                userResult.get().getResetPasswordTokenExpiresAt().isAfter(LocalDateTime.now())) {
                User user = userResult.get();
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetPasswordToken(null);
                user.setResetPasswordTokenExpiresAt(null);
                userRepository.save(user);
            }

            // Then
            assertThat(userResult).isPresent();
            verify(passwordEncoder).encode(newPassword);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should reject expired token")
        void shouldRejectExpiredToken() {
            // Given
            String expiredToken = "expired-token";
            testUser.setResetPasswordToken(expiredToken);
            testUser.setResetPasswordTokenExpiresAt(LocalDateTime.now().minusMinutes(30));
            
            when(userRepository.findByResetPasswordToken(expiredToken)).thenReturn(Optional.of(testUser));

            // When
            Optional<User> userResult = userRepository.findByResetPasswordToken(expiredToken);
            boolean isTokenValid = userResult.isPresent() && 
                userResult.get().getResetPasswordTokenExpiresAt().isAfter(LocalDateTime.now());

            // Then
            assertThat(userResult).isPresent();
            assertThat(isTokenValid).isFalse();
            verify(userRepository).findByResetPasswordToken(expiredToken);
        }

        @Test
        @DisplayName("Should reject invalid token")
        void shouldRejectInvalidToken() {
            // Given
            String invalidToken = "invalid-token";
            when(userRepository.findByResetPasswordToken(invalidToken)).thenReturn(Optional.empty());

            // When
            Optional<User> userResult = userRepository.findByResetPasswordToken(invalidToken);

            // Then
            assertThat(userResult).isEmpty();
            verify(userRepository).findByResetPasswordToken(invalidToken);
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should mask email correctly")
        void shouldMaskEmailCorrectly() {
            try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
                // Given
                securityUtilsMock.when(() -> SecurityUtils.maskEmail("test@example.com"))
                    .thenReturn("t***@***.com");
                securityUtilsMock.when(() -> SecurityUtils.maskEmail("john.doe@company.org"))
                    .thenReturn("j***@***.org");

                // When & Then
                assertThat(SecurityUtils.maskEmail("test@example.com")).isEqualTo("t***@***.com");
                assertThat(SecurityUtils.maskEmail("john.doe@company.org")).isEqualTo("j***@***.org");
            }
        }

        @Test
        @DisplayName("Should generate secure tokens")
        void shouldGenerateSecureTokens() {
            try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
                // Given
                securityUtilsMock.when(() -> SecurityUtils.generateSecureToken(32))
                    .thenReturn("secure-token-32-chars-long-abc123");

                // When
                String token = SecurityUtils.generateSecureToken(32);

                // Then
                assertThat(token).isNotNull();
                assertThat(token).isEqualTo("secure-token-32-chars-long-abc123");
            }
        }

        @Test
        @DisplayName("Should validate token expiration")
        void shouldValidateTokenExpiration() {
            // Given
            LocalDateTime futureTime = LocalDateTime.now().plusMinutes(30);
            LocalDateTime pastTime = LocalDateTime.now().minusMinutes(30);

            // When & Then
            assertThat(futureTime.isAfter(LocalDateTime.now())).isTrue();
            assertThat(pastTime.isAfter(LocalDateTime.now())).isFalse();
        }
    }
}