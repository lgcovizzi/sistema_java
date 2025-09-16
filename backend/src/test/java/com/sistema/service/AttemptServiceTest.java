package com.sistema.service;

import com.sistema.service.interfaces.AttemptControlOperations;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttemptService Tests")
class AttemptServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AttemptService attemptService;

    private static final String TEST_IP = "192.168.1.1";
    private static final String ATTEMPT_KEY_PREFIX = "attempt:";
    private static final int MAX_ATTEMPTS = 5;
    private static final int ATTEMPT_TTL_MINUTES = 15;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Configurar propriedades via reflection
        ReflectionTestUtils.setField(attemptService, "maxAttempts", MAX_ATTEMPTS);
        ReflectionTestUtils.setField(attemptService, "attemptTtlMinutes", ATTEMPT_TTL_MINUTES);
    }

    @Nested
    @DisplayName("Record Attempt Tests")
    class RecordAttemptTests {

        @Test
        @DisplayName("Should record first attempt for IP")
        void recordAttemptControl_FirstAttempt_Success() {
            // Given
            String attemptKey = ATTEMPT_KEY_PREFIX + TEST_IP;
            when(valueOperations.get(attemptKey)).thenReturn(null);
            when(valueOperations.increment(attemptKey)).thenReturn(1L);

            // When
            attemptService.recordAttemptControl(TEST_IP);

            // Then
            verify(valueOperations).increment(attemptKey);
            verify(redisTemplate).expire(attemptKey, Duration.ofMinutes(ATTEMPT_TTL_MINUTES));
        }

        @Test
        @DisplayName("Should increment existing attempt count")
        void recordAttemptControl_IncrementExisting_Success() {
            // Given
            String attemptKey = ATTEMPT_KEY_PREFIX + TEST_IP;
            when(valueOperations.get(attemptKey)).thenReturn(3);
            when(valueOperations.increment(attemptKey)).thenReturn(4L);

            // When
            attemptService.recordAttemptControl(TEST_IP);

            // Then
            verify(valueOperations).increment(attemptKey);
            verify(redisTemplate).expire(attemptKey, Duration.ofMinutes(ATTEMPT_TTL_MINUTES));
        }

        @Test
        @DisplayName("Should handle null IP address")
        void recordAttemptControl_NullIp_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> attemptService.recordAttemptControl(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("IP address cannot be null or empty");
        }

        @Test
        @DisplayName("Should handle empty IP address")
        void recordAttemptControl_EmptyIp_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> attemptService.recordAttemptControl(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("IP address cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("Captcha Required Tests")
    class CaptchaRequiredTests {

        @Test
        @DisplayName("Should not require captcha for new IP")
        void isCaptchaRequiredControl_NewIp_ReturnsFalse() {
            // Given
            String attemptKey = ATTEMPT_KEY_PREFIX + TEST_IP;
            when(valueOperations.get(attemptKey)).thenReturn(null);

            // When
            boolean result = attemptService.isCaptchaRequiredControl(TEST_IP);

            // Then
            assertThat(result).isFalse();
            verify(valueOperations).get(attemptKey);
        }

        @Test
        @DisplayName("Should not require captcha for attempts below threshold")
        void isCaptchaRequiredControl_BelowThreshold_ReturnsFalse() {
            // Given
            String attemptKey = ATTEMPT_KEY_PREFIX + TEST_IP;
            when(valueOperations.get(attemptKey)).thenReturn(3);

            // When
            boolean result = attemptService.isCaptchaRequiredControl(TEST_IP);

            // Then
            assertThat(result).isFalse();
            verify(valueOperations).get(attemptKey);
        }

        @Test
        @DisplayName("Should require captcha when threshold reached")
        void isCaptchaRequiredControl_ThresholdReached_ReturnsTrue() {
            // Given
            String attemptKey = ATTEMPT_KEY_PREFIX + TEST_IP;
            when(valueOperations.get(attemptKey)).thenReturn(MAX_ATTEMPTS);

            // When
            boolean result = attemptService.isCaptchaRequiredControl(TEST_IP);

            // Then
            assertThat(result).isTrue();
            verify(valueOperations).get(attemptKey);
        }

        @Test
        @DisplayName("Should require captcha when threshold exceeded")
        void isCaptchaRequiredControl_ThresholdExceeded_ReturnsTrue() {
            // Given
            String attemptKey = ATTEMPT_KEY_PREFIX + TEST_IP;
            when(valueOperations.get(attemptKey)).thenReturn(MAX_ATTEMPTS + 2);

            // When
            boolean result = attemptService.isCaptchaRequiredControl(TEST_IP);

            // Then
            assertThat(result).isTrue();
            verify(valueOperations).get(attemptKey);
        }

        @Test
        @DisplayName("Should handle null IP for captcha check")
        void isCaptchaRequiredControl_NullIp_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> attemptService.isCaptchaRequiredControl(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("IP address cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("Clear Attempts Tests")
    class ClearAttemptsTests {

        @Test
        @DisplayName("Should clear attempts for IP successfully")
        void clearAttemptsControl_Success() {
            // Given
            String attemptKey = ATTEMPT_KEY_PREFIX + TEST_IP;
            when(redisTemplate.delete(attemptKey)).thenReturn(true);

            // When
            attemptService.clearAttemptsControl(TEST_IP);

            // Then
            verify(redisTemplate).delete(attemptKey);
        }

        @Test
        @DisplayName("Should handle clearing non-existent attempts")
        void clearAttemptsControl_NonExistent_Success() {
            // Given
            String attemptKey = ATTEMPT_KEY_PREFIX + TEST_IP;
            when(redisTemplate.delete(attemptKey)).thenReturn(false);

            // When
            attemptService.clearAttemptsControl(TEST_IP);

            // Then
            verify(redisTemplate).delete(attemptKey);
        }

        @Test
        @DisplayName("Should handle null IP for clear attempts")
        void clearAttemptsControl_NullIp_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> attemptService.clearAttemptsControl(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("IP address cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("Statistics and Monitoring Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should get attempt count for IP")
        void getAttemptCount_Success() {
            // Given
            String attemptKey = ATTEMPT_KEY_PREFIX + TEST_IP;
            when(valueOperations.get(attemptKey)).thenReturn(3);

            // When
            int result = attemptService.getAttemptCount(TEST_IP);

            // Then
            assertThat(result).isEqualTo(3);
            verify(valueOperations).get(attemptKey);
        }

        @Test
        @DisplayName("Should return zero for new IP")
        void getAttemptCount_NewIp_ReturnsZero() {
            // Given
            String attemptKey = ATTEMPT_KEY_PREFIX + TEST_IP;
            when(valueOperations.get(attemptKey)).thenReturn(null);

            // When
            int result = attemptService.getAttemptCount(TEST_IP);

            // Then
            assertThat(result).isEqualTo(0);
            verify(valueOperations).get(attemptKey);
        }

        @Test
        @DisplayName("Should get remaining attempts")
        void getRemainingAttempts_Success() {
            // Given
            String attemptKey = ATTEMPT_KEY_PREFIX + TEST_IP;
            when(valueOperations.get(attemptKey)).thenReturn(2);

            // When
            int result = attemptService.getRemainingAttempts(TEST_IP);

            // Then
            assertThat(result).isEqualTo(3); // MAX_ATTEMPTS (5) - current (2) = 3
            verify(valueOperations).get(attemptKey);
        }

        @Test
        @DisplayName("Should return zero remaining attempts when threshold exceeded")
        void getRemainingAttempts_ThresholdExceeded_ReturnsZero() {
            // Given
            String attemptKey = ATTEMPT_KEY_PREFIX + TEST_IP;
            when(valueOperations.get(attemptKey)).thenReturn(MAX_ATTEMPTS + 1);

            // When
            int result = attemptService.getRemainingAttempts(TEST_IP);

            // Then
            assertThat(result).isEqualTo(0);
            verify(valueOperations).get(attemptKey);
        }
    }

    @Nested
    @DisplayName("Cleanup and Maintenance Tests")
    class CleanupTests {

        @Test
        @DisplayName("Should cleanup expired attempts")
        void cleanupExpiredAttempts_Success() {
            // Given
            Set<String> attemptKeys = Set.of(
                    ATTEMPT_KEY_PREFIX + "192.168.1.1",
                    ATTEMPT_KEY_PREFIX + "192.168.1.2",
                    ATTEMPT_KEY_PREFIX + "192.168.1.3"
            );
            when(redisTemplate.keys(ATTEMPT_KEY_PREFIX + "*")).thenReturn(attemptKeys);
            when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(-1L); // Expired

            // When
            int result = attemptService.cleanupExpiredAttempts();

            // Then
            assertThat(result).isEqualTo(3);
            verify(redisTemplate).delete(attemptKeys);
        }

        @Test
        @DisplayName("Should not cleanup non-expired attempts")
        void cleanupExpiredAttempts_NonExpired_NoCleanup() {
            // Given
            Set<String> attemptKeys = Set.of(
                    ATTEMPT_KEY_PREFIX + "192.168.1.1",
                    ATTEMPT_KEY_PREFIX + "192.168.1.2"
            );
            when(redisTemplate.keys(ATTEMPT_KEY_PREFIX + "*")).thenReturn(attemptKeys);
            when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(300L); // Not expired

            // When
            int result = attemptService.cleanupExpiredAttempts();

            // Then
            assertThat(result).isEqualTo(0);
            verify(redisTemplate, never()).delete(anyString());
        }
    }

    @Nested
    @DisplayName("Integration with AttemptControlOperations Interface Tests")
    class InterfaceImplementationTests {

        @Test
        @DisplayName("Should implement AttemptControlOperations interface")
        void shouldImplementInterface() {
            // Then
            assertThat(attemptService).isInstanceOf(AttemptControlOperations.class);
        }

        @Test
        @DisplayName("Should provide all interface methods")
        void shouldProvideAllInterfaceMethods() {
            // Given
            AttemptControlOperations operations = attemptService;
            String testIp = "192.168.1.100";

            // When & Then - Should not throw exceptions
            assertThatCode(() -> {
                operations.recordAttemptControl(testIp);
                operations.isCaptchaRequiredControl(testIp);
                operations.clearAttemptsControl(testIp);
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
            String attemptKey = ATTEMPT_KEY_PREFIX + TEST_IP;
            when(valueOperations.get(attemptKey)).thenThrow(new RuntimeException("Redis connection failed"));

            // When & Then
            assertThatThrownBy(() -> attemptService.getAttemptCount(TEST_IP))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Redis connection failed");
        }

        @Test
        @DisplayName("Should handle invalid IP formats")
        void handleInvalidIpFormats() {
            // Given
            String invalidIp = "invalid.ip.format";

            // When & Then - Should still work as we don't validate IP format
            assertThatCode(() -> {
                attemptService.recordAttemptControl(invalidIp);
                attemptService.isCaptchaRequiredControl(invalidIp);
                attemptService.clearAttemptsControl(invalidIp);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle very long IP strings")
        void handleLongIpStrings() {
            // Given
            String longIp = "a".repeat(1000);

            // When & Then
            assertThatCode(() -> {
                attemptService.recordAttemptControl(longIp);
                attemptService.isCaptchaRequiredControl(longIp);
                attemptService.clearAttemptsControl(longIp);
            }).doesNotThrowAnyException();
        }
    }
}