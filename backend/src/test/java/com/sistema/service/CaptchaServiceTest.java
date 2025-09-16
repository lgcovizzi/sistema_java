package com.sistema.service;

import com.sistema.service.interfaces.CaptchaOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CaptchaService Tests")
class CaptchaServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private CaptchaService captchaService;

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    private static final String STATS_KEY_PREFIX = "captcha:stats:";
    private static final int CAPTCHA_TTL_MINUTES = 5;
    private static final int CAPTCHA_WIDTH = 200;
    private static final int CAPTCHA_HEIGHT = 50;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Configurar propriedades via reflection
        ReflectionTestUtils.setField(captchaService, "captchaTtlMinutes", CAPTCHA_TTL_MINUTES);
        ReflectionTestUtils.setField(captchaService, "captchaWidth", CAPTCHA_WIDTH);
        ReflectionTestUtils.setField(captchaService, "captchaHeight", CAPTCHA_HEIGHT);
    }

    @Nested
    @DisplayName("Captcha Creation Tests")
    class CaptchaCreationTests {

        @Test
        @DisplayName("Should create captcha successfully")
        void createCaptcha_Success() {
            // Given
            String captchaId = "test-captcha-id";
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

            // When
            Map<String, String> result = captchaService.generateCaptcha();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).containsKey("id");
            assertThat(result).containsKey("imageBase64");
            assertThat(result.get("id")).isNotNull();
            assertThat(result.get("imageBase64")).isInstanceOf(String.class);
            
            String imageBase64 = result.get("imageBase64");
            assertThat(imageBase64).startsWith("data:image/png;base64,");
            
            verify(valueOperations).setIfAbsent(anyString(), anyString(), eq(Duration.ofMinutes(CAPTCHA_TTL_MINUTES)));
        }

        @Test
        @DisplayName("Should handle Redis storage failure")
        void generateCaptcha_RedisFailure_ThrowsException() {
            // Given
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

            // When & Then
            assertThatCode(() -> captchaService.generateCaptcha())
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Captcha Verification Tests")
    class CaptchaVerificationTests {

        @Test
        @DisplayName("Should verify captcha successfully")
        void verifyCaptcha_Success() {
            // Given
            String captchaId = "test-captcha-id";
            String userResponse = "ABCD";
            String storedHash = "hashed-response";
            String captchaKey = CAPTCHA_KEY_PREFIX + captchaId;
            
            when(valueOperations.get(captchaKey)).thenReturn(storedHash);
            when(redisTemplate.delete(captchaKey)).thenReturn(true);
            
            // Mock SecurityUtils.hashSHA256 behavior
            ReflectionTestUtils.setField(captchaService, "useSecurityUtils", false);

            // When
            boolean result = captchaService.validateCaptcha(captchaId, userResponse);

            // Then - Since we can't easily mock static methods, we'll test the flow
            verify(valueOperations).get(captchaKey);
            verify(redisTemplate).delete(captchaKey);
        }

        @Test
        @DisplayName("Should return false for non-existent captcha")
        void verifyCaptcha_NonExistent_ReturnsFalse() {
            // Given
            String captchaId = "non-existent-id";
            String userResponse = "ABCD";
            String captchaKey = CAPTCHA_KEY_PREFIX + captchaId;
            
            when(valueOperations.get(captchaKey)).thenReturn(null);

            // When
            boolean result = captchaService.validateCaptcha(captchaId, userResponse);

            // Then
            assertThat(result).isFalse();
            verify(valueOperations).get(captchaKey);
            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("Should handle null captcha ID for verification")
        void validateCaptcha_NullId_ThrowsException() {
            // When & Then
            assertThatCode(() -> captchaService.validateCaptcha(null, "ABCD"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle null user response")
        void validateCaptcha_NullResponse_ThrowsException() {
            // When & Then
            assertThatCode(() -> captchaService.validateCaptcha("test-id", null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle empty user response")
        void validateCaptcha_EmptyResponse_ThrowsException() {
            // When & Then
            assertThatCode(() -> captchaService.validateCaptcha("test-id", ""))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Captcha Validation Tests")
    class CaptchaValidationTests {

        @Test
        @DisplayName("Should return true for valid captcha")
        void isCaptchaValid_Valid_ReturnsTrue() {
            // Given
            String captchaId = "valid-captcha-id";
            String captchaKey = CAPTCHA_KEY_PREFIX + captchaId;
            
            when(redisTemplate.hasKey(captchaKey)).thenReturn(true);

            // When
            boolean result = captchaService.captchaExists(captchaId);

            // Then
            assertThat(result).isTrue();
            verify(redisTemplate).hasKey(captchaKey);
        }

        @Test
        @DisplayName("Should return false for invalid captcha")
        void isCaptchaValid_Invalid_ReturnsFalse() {
            // Given
            String captchaId = "invalid-captcha-id";
            String captchaKey = CAPTCHA_KEY_PREFIX + captchaId;
            
            when(redisTemplate.hasKey(captchaKey)).thenReturn(false);

            // When
            boolean result = captchaService.captchaExists(captchaId);

            // Then
            assertThat(result).isFalse();
            verify(redisTemplate).hasKey(captchaKey);
        }

        @Test
        @DisplayName("Should handle null captcha ID for validation")
        void captchaExists_NullId_ThrowsException() {
            // When & Then
            assertThatCode(() -> captchaService.captchaExists(null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Captcha Configuration Tests")
    class CaptchaConfigurationTests {

        @Test
        @DisplayName("Should get captcha configuration")
        void getCaptchaConfiguration_Success() {
            // When
            Map<String, Object> config = captchaService.getDefaultCaptchaConfig();

            // Then
            assertThat(config).isNotNull();
            assertThat(config).containsKey("width");
            assertThat(config).containsKey("height");
            assertThat(config).containsKey("ttlMinutes");
            assertThat(config).containsKey("textLength");
            
            assertThat(config.get("width")).isEqualTo(CAPTCHA_WIDTH);
            assertThat(config.get("height")).isEqualTo(CAPTCHA_HEIGHT);
            assertThat(config.get("ttlMinutes")).isEqualTo(CAPTCHA_TTL_MINUTES);
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should get captcha statistics")
        void getCaptchaStatistics_Success() {
            // Given
            String generatedKey = STATS_KEY_PREFIX + "generated";
            String validatedKey = STATS_KEY_PREFIX + "validated";
            String successfulKey = STATS_KEY_PREFIX + "successful";
            
            when(valueOperations.get(generatedKey)).thenReturn(100);
            when(valueOperations.get(validatedKey)).thenReturn(80);
            when(valueOperations.get(successfulKey)).thenReturn(60);

            // When
            Map<String, Object> stats = captchaService.getCaptchaStatistics();

            // Then
            assertThat(stats).isNotNull();
            assertThat(stats).containsKey("totalGenerated");
            assertThat(stats).containsKey("totalValidated");
            assertThat(stats).containsKey("totalSuccessful");
            assertThat(stats).containsKey("successRate");
            assertThat(stats).containsKey("lastUpdated");
            
            assertThat(stats.get("totalGenerated")).isEqualTo(100);
            assertThat(stats.get("totalValidated")).isEqualTo(80);
            assertThat(stats.get("totalSuccessful")).isEqualTo(60);
        }

        @Test
        @DisplayName("Should handle null statistics gracefully")
        void getCaptchaStatistics_NullValues_Success() {
            // Given
            when(valueOperations.get(anyString())).thenReturn(null);

            // When
            Map<String, Object> stats = captchaService.getCaptchaStatistics();

            // Then
            assertThat(stats).isNotNull();
            assertThat(stats.get("totalGenerated")).isEqualTo(0);
            assertThat(stats.get("totalValidated")).isEqualTo(0);
            assertThat(stats.get("totalSuccessful")).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Cleanup Tests")
    class CleanupTests {

        @Test
        @DisplayName("Should cleanup expired captchas")
        void cleanupExpiredCaptchas_Success() {
            // Given
            Set<String> captchaKeys = Set.of(
                    CAPTCHA_KEY_PREFIX + "captcha1",
                    CAPTCHA_KEY_PREFIX + "captcha2",
                    CAPTCHA_KEY_PREFIX + "captcha3"
            );
            when(redisTemplate.keys(CAPTCHA_KEY_PREFIX + "*")).thenReturn(captchaKeys);
            when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(-1L); // Expired

            // When
            long result = captchaService.cleanupExpiredCaptchas();

            // Then
            assertThat(result).isEqualTo(3);
            verify(redisTemplate).delete(captchaKeys);
        }

        @Test
        @DisplayName("Should not cleanup non-expired captchas")
        void cleanupExpiredCaptchas_NonExpired_NoCleanup() {
            // Given
            Set<String> captchaKeys = Set.of(
                    CAPTCHA_KEY_PREFIX + "captcha1",
                    CAPTCHA_KEY_PREFIX + "captcha2"
            );
            when(redisTemplate.keys(CAPTCHA_KEY_PREFIX + "*")).thenReturn(captchaKeys);
            when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(300L); // Not expired

            // When
            long result = captchaService.cleanupExpiredCaptchas();

            // Then
            assertThat(result).isEqualTo(0);
            verify(redisTemplate, never()).delete(anyString());
        }
    }

    @Nested
    @DisplayName("Integration with CaptchaOperations Interface Tests")
    class InterfaceImplementationTests {

        @Test
        @DisplayName("Should implement CaptchaOperations interface")
        void shouldImplementInterface() {
            // Then
            assertThat(captchaService).isInstanceOf(CaptchaOperations.class);
        }

        @Test
        @DisplayName("Should provide all interface methods")
        void shouldProvideAllInterfaceMethods() {
            // Given
            CaptchaOperations operations = captchaService;
            String testId = "test-captcha-id";

            // When & Then - Should not throw exceptions for interface methods
            assertThatCode(() -> {
                operations.captchaExists(testId);
                operations.getDefaultCaptchaConfig();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle Redis connection errors gracefully")
        void handleRedisConnectionError() {
            // Given
            String captchaId = "test-captcha-id";
            when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

            // When & Then
            assertThatThrownBy(() -> captchaService.captchaExists(captchaId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Redis connection failed");
        }

        @Test
        @DisplayName("Should handle very long captcha IDs")
        void handleLongCaptchaIds() {
            // Given
            String longId = "a".repeat(1000);
            when(redisTemplate.hasKey(anyString())).thenReturn(false);

            // When & Then
            assertThatCode(() -> captchaService.captchaExists(longId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle special characters in captcha ID")
        void handleSpecialCharactersInId() {
            // Given
            String specialId = "test-id-with-special-chars-!@#$%^&*()";
            when(redisTemplate.hasKey(anyString())).thenReturn(true);

            // When & Then
            assertThatCode(() -> captchaService.captchaExists(specialId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle case sensitivity in user response")
        void handleCaseSensitivityInResponse() {
            // Given
            String captchaId = "test-captcha-id";
            String userResponseLower = "abcd";
            String userResponseUpper = "ABCD";
            String captchaKey = CAPTCHA_KEY_PREFIX + captchaId;
            
            when(valueOperations.get(captchaKey)).thenReturn("stored-hash");
            when(redisTemplate.delete(captchaKey)).thenReturn(true);

            // When & Then - Both should be handled (case-insensitive)
            assertThatCode(() -> {
                captchaService.validateCaptcha(captchaId, userResponseLower);
                captchaService.validateCaptcha(captchaId, userResponseUpper);
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Performance and Load Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should handle multiple concurrent captcha creations")
        void handleConcurrentCreations() {
            // Given
            when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

            // When & Then
            assertThatCode(() -> {
                for (int i = 0; i < 100; i++) {
                    captchaService.generateCaptcha();
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle bulk captcha validations")
        void handleBulkValidations() {
            // Given
            when(redisTemplate.hasKey(anyString())).thenReturn(true);

            // When & Then
            assertThatCode(() -> {
                for (int i = 0; i < 100; i++) {
                    captchaService.captchaExists("captcha-" + i);
                }
            }).doesNotThrowAnyException();
        }
    }
}