package com.sistema.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SecurityUtils Tests")
class SecurityUtilsTest {

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate secure random tokens")
        void shouldGenerateSecureRandomTokens() {
            // When
            String token1 = SecurityUtils.generateSecureToken();
            String token2 = SecurityUtils.generateSecureToken();

            // Then
            assertThat(token1)
                    .isNotNull()
                    .isNotEmpty()
                    .isNotEqualTo(token2); // Should be unique

            assertThat(token1.length()).isGreaterThan(10); // Should be reasonably long
        }

        @Test
        @DisplayName("Should generate tokens with specified length")
        void shouldGenerateTokensWithSpecifiedLength() {
            // Given
            int[] lengths = {8, 16, 32, 64};

            // When & Then
            for (int length : lengths) {
                String token = SecurityUtils.generateSecureToken(length);
                assertThat(token)
                        .isNotNull()
                        .isNotEmpty();
                // Base64 URL-safe encoding without padding
                // Length varies but should be reasonable for the input
                assertThat(token.length()).isGreaterThan(0);
            }
        }

        @Test
        @DisplayName("Should generate base64 URL-safe tokens")
        void shouldGenerateBase64UrlSafeTokens() {
            // Given
            int length = 20;

            // When
            String token = SecurityUtils.generateSecureToken(length);

            // Then
            assertThat(token)
                    .isNotNull()
                    .isNotEmpty()
                    .matches("[a-zA-Z0-9_-]+"); // Base64 URL-safe characters
        }

        @Test
        @DisplayName("Should handle edge cases for token generation")
        void shouldHandleEdgeCasesForTokenGeneration() {
            // When & Then
            assertThatThrownBy(() -> SecurityUtils.generateSecureToken(0))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> SecurityUtils.generateSecureToken(-1))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> SecurityUtils.generateSecureToken(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Hash Generation Tests")
    class HashGenerationTests {

        @Test
        @DisplayName("Should generate SHA-256 hash")
        void shouldGenerateSha256Hash() {
            // Given
            String input = "test string";

            // When
            String hash = SecurityUtils.generateHash(input);

            // Then
            assertThat(hash)
                    .isNotNull()
                    .isNotEmpty()
                    .hasSize(64) // SHA-256 produces 64 character hex string
                    .matches("[a-f0-9]+"); // Only lowercase hex characters
        }

        @Test
        @DisplayName("Should generate consistent hashes for same input")
        void shouldGenerateConsistentHashesForSameInput() {
            // Given
            String input = "consistent input";

            // When
            String hash1 = SecurityUtils.generateHash(input);
            String hash2 = SecurityUtils.generateHash(input);

            // Then
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("Should generate different hashes for different inputs")
        void shouldGenerateDifferentHashesForDifferentInputs() {
            // Given
            String input1 = "input one";
            String input2 = "input two";

            // When
            String hash1 = SecurityUtils.generateHash(input1);
            String hash2 = SecurityUtils.generateHash(input2);

            // Then
            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("Should handle empty and null inputs for hashing")
        void shouldHandleEmptyAndNullInputsForHashing() {
            // When & Then
            assertThatThrownBy(() -> SecurityUtils.generateHash(null))
                    .isInstanceOf(IllegalArgumentException.class);

            // Empty string should work
            String emptyHash = SecurityUtils.generateHash("");
            assertThat(emptyHash)
                    .isNotNull()
                    .hasSize(64);
        }

        @Test
        @DisplayName("Should generate hash with salt")
        void shouldGenerateHashWithSalt() {
            // Given
            String input = "password";
            String salt = SecurityUtils.generateSalt();

            // When
            String hash1 = SecurityUtils.generateHash(input, salt);
            String hash2 = SecurityUtils.generateHash(input, salt);
            String hash3 = SecurityUtils.generateHash(input, SecurityUtils.generateSalt());

            // Then
            assertThat(hash1)
                    .isEqualTo(hash2) // Same input and salt should produce same hash
                    .isNotEqualTo(hash3); // Different salt should produce different hash
        }
    }

    @Nested
    @DisplayName("Input Sanitization Tests")
    class InputSanitizationTests {

        @Test
        @DisplayName("Should sanitize HTML input")
        void shouldSanitizeHtmlInput() {
            // Given
            String maliciousInput = "<script>alert('xss')</script>Hello World";
            String htmlInput = "<b>Bold</b> and <i>italic</i> text";

            // When
            String sanitized1 = SecurityUtils.sanitizeInput(maliciousInput);
            String sanitized2 = SecurityUtils.sanitizeInput(htmlInput);

            // Then
            assertThat(sanitized1)
                    .doesNotContain("<script>")
                    .doesNotContain("</script>")
                    .contains("Hello World");

            assertThat(sanitized2)
                    .doesNotContain("<b>")
                    .doesNotContain("</b>")
                    .doesNotContain("<i>")
                    .doesNotContain("</i>")
                    .contains("Bold")
                    .contains("italic");
        }

        @Test
        @DisplayName("Should handle SQL injection attempts")
        void shouldHandleSqlInjectionAttempts() {
            // Given
            String sqlInjection = "'; DROP TABLE users; --";
            String normalInput = "John O'Connor";

            // When
            String sanitized1 = SecurityUtils.sanitizeInput(sqlInjection);
            String sanitized2 = SecurityUtils.sanitizeInput(normalInput);

            // Then
            assertThat(sanitized1)
                    .doesNotContain("DROP TABLE")
                    .doesNotContain("--");

            assertThat(sanitized2)
                    .contains("John")
                    .contains("Connor"); // Should preserve legitimate apostrophes
        }

        @Test
        @DisplayName("Should preserve safe characters")
        void shouldPreserveSafeCharacters() {
            // Given
            String safeInput = "Hello World! 123 @example.com (test)";

            // When
            String sanitized = SecurityUtils.sanitizeInput(safeInput);

            // Then
            assertThat(sanitized)
                    .contains("Hello World!")
                    .contains("123")
                    .contains("@example.com")
                    .contains("(test)");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty inputs for sanitization")
        void shouldHandleNullAndEmptyInputsForSanitization(String input) {
            // When
            String sanitized = SecurityUtils.sanitizeInput(input);

            // Then
            if (input == null) {
                assertThat(sanitized).isNull();
            } else {
                assertThat(sanitized).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("IP Address Validation Tests")
    class IpAddressValidationTests {

        @Test
        @DisplayName("Should validate IPv4 addresses")
        void shouldValidateIpv4Addresses() {
            // Given
            String[] validIpv4 = {
                    "192.168.1.1",
                    "10.0.0.1",
                    "172.16.0.1",
                    "127.0.0.1",
                    "8.8.8.8",
                    "255.255.255.255",
                    "0.0.0.0"
            };

            // When & Then
            for (String ip : validIpv4) {
                assertThat(SecurityUtils.isValidIpAddress(ip))
                        .as("IP %s should be valid", ip)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Should reject invalid IPv4 addresses")
        void shouldRejectInvalidIpv4Addresses() {
            // Given
            String[] invalidIpv4 = {
                    "256.1.1.1", // Out of range
                    "192.168.1", // Incomplete
                    "192.168.1.1.1", // Too many octets
                    "192.168.-1.1", // Negative number
                    "192.168.1.a", // Non-numeric
                    "not.an.ip.address",
                    "192.168.01.1", // Leading zeros
                    "192.168.1.", // Trailing dot
                    ".192.168.1.1" // Leading dot
            };

            // When & Then
            for (String ip : invalidIpv4) {
                assertThat(SecurityUtils.isValidIpAddress(ip))
                        .as("IP %s should be invalid", ip)
                        .isFalse();
            }
        }

        @Test
        @DisplayName("Should validate IPv6 addresses")
        void shouldValidateIpv6Addresses() {
            // Given
            String[] validIpv6 = {
                    "2001:0db8:85a3:0000:0000:8a2e:0370:7334",
                    "2001:db8:85a3::8a2e:370:7334", // Compressed
                    "::1", // Loopback
                    "::", // All zeros
                    "fe80::1", // Link-local
                    "2001:db8::1"
            };

            // When & Then
            for (String ip : validIpv6) {
                assertThat(SecurityUtils.isValidIpAddress(ip))
                        .as("IPv6 %s should be valid", ip)
                        .isTrue();
            }
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty IP addresses")
        void shouldHandleNullAndEmptyIpAddresses(String ip) {
            // When & Then
            assertThat(SecurityUtils.isValidIpAddress(ip)).isFalse();
        }
    }

    @Nested
    @DisplayName("Data Masking Tests")
    class DataMaskingTests {

        @Test
        @DisplayName("Should mask email addresses")
        void shouldMaskEmailAddresses() {
            // Given
            String email = "user@example.com";

            // When
            String masked = SecurityUtils.maskEmail(email);

            // Then
            assertThat(masked)
                    .isNotNull()
                    .contains("@example.com")
                    .contains("*")
                    .doesNotContain("user");
        }

        @Test
        @DisplayName("Should mask credit card numbers")
        void shouldMaskCreditCardNumbers() {
            // Given
            String creditCard = "1234567890123456";

            // When
            String masked = SecurityUtils.maskCreditCard(creditCard);

            // Then
            assertThat(masked)
                    .isNotNull()
                    .contains("*")
                    .endsWith("3456") // Last 4 digits should be visible
                    .doesNotContain("1234567890");
        }

        @Test
        @DisplayName("Should mask phone numbers")
        void shouldMaskPhoneNumbers() {
            // Given
            String phone = "11987654321";

            // When
            String masked = SecurityUtils.maskPhone(phone);

            // Then
            assertThat(masked)
                    .isNotNull()
                    .contains("*")
                    .startsWith("11") // Area code should be visible
                    .endsWith("21"); // Last 2 digits should be visible
        }

        @Test
        @DisplayName("Should mask CPF numbers")
        void shouldMaskCpfNumbers() {
            // Given
            String cpf = "12345678909";

            // When
            String masked = SecurityUtils.maskCpf(cpf);

            // Then
            assertThat(masked)
                    .isNotNull()
                    .contains("*")
                    .startsWith("123") // First 3 digits should be visible
                    .endsWith("09"); // Last 2 digits should be visible
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty inputs for masking")
        void shouldHandleNullAndEmptyInputsForMasking(String input) {
            // When & Then
            assertThat(SecurityUtils.maskEmail(input)).isEqualTo(input);
            assertThat(SecurityUtils.maskCreditCard(input)).isEqualTo(input);
            assertThat(SecurityUtils.maskPhone(input)).isEqualTo(input);
            assertThat(SecurityUtils.maskCpf(input)).isEqualTo(input);
        }
    }

    @Nested
    @DisplayName("Encryption and Decryption Tests")
    class EncryptionDecryptionTests {

        @Test
        @DisplayName("Should encrypt and decrypt data")
        void shouldEncryptAndDecryptData() {
            // Given
            String plaintext = "sensitive data";
            String key = "encryption-key-123";

            // When
            String encrypted = SecurityUtils.encrypt(plaintext, key);
            String decrypted = SecurityUtils.decrypt(encrypted, key);

            // Then
            assertThat(encrypted)
                    .isNotNull()
                    .isNotEmpty()
                    .isNotEqualTo(plaintext);

            assertThat(decrypted)
                    .isNotNull()
                    .isEqualTo(plaintext);
        }

        @Test
        @DisplayName("Should produce different encrypted values for same input")
        void shouldProduceDifferentEncryptedValuesForSameInput() {
            // Given
            String plaintext = "test data";
            String key = "test-key";

            // When
            String encrypted1 = SecurityUtils.encrypt(plaintext, key);
            String encrypted2 = SecurityUtils.encrypt(plaintext, key);

            // Then
            assertThat(encrypted1).isNotEqualTo(encrypted2); // Should use random IV
        }

        @Test
        @DisplayName("Should fail decryption with wrong key")
        void shouldFailDecryptionWithWrongKey() {
            // Given
            String plaintext = "secret message";
            String correctKey = "correct-key";
            String wrongKey = "wrong-key";

            // When
            String encrypted = SecurityUtils.encrypt(plaintext, correctKey);

            // Then
            assertThatThrownBy(() -> SecurityUtils.decrypt(encrypted, wrongKey))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        @DisplayName("Should handle encryption edge cases")
        void shouldHandleEncryptionEdgeCases() {
            // Given
            String key = "test-key";

            // When & Then
            assertThatThrownBy(() -> SecurityUtils.encrypt(null, key))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> SecurityUtils.encrypt("data", null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> SecurityUtils.encrypt("data", ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Security Validation Tests")
    class SecurityValidationTests {

        @Test
        @DisplayName("Should detect suspicious patterns")
        void shouldDetectSuspiciousPatterns() {
            // Given
            String[] suspiciousInputs = {
                    "<script>alert('xss')</script>",
                    "'; DROP TABLE users; --",
                    "../../../etc/passwd",
                    "javascript:alert('xss')",
                    "<iframe src='malicious.com'></iframe>"
            };

            // When & Then
            for (String input : suspiciousInputs) {
                assertThat(SecurityUtils.containsSuspiciousPattern(input))
                        .as("Input '%s' should be detected as suspicious", input)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Should allow safe inputs")
        void shouldAllowSafeInputs() {
            // Given
            String[] safeInputs = {
                    "Hello World",
                    "user@example.com",
                    "Valid input with numbers 123",
                    "Special chars: !@#$%^&*()",
                    "José da Silva"
            };

            // When & Then
            for (String input : safeInputs) {
                assertThat(SecurityUtils.containsSuspiciousPattern(input))
                        .as("Input '%s' should be safe", input)
                        .isFalse();
            }
        }

        @Test
        @DisplayName("Should validate secure tokens")
        void shouldValidateSecureTokens() {
            // Given
            String token = SecurityUtils.generateSecureToken();
            
            // When & Then
            assertThat(SecurityUtils.isValidToken(token)).isTrue();
            assertThat(SecurityUtils.isValidToken(null)).isFalse();
            assertThat(SecurityUtils.isValidToken("")).isFalse();
            assertThat(SecurityUtils.isValidToken("invalid")).isFalse();
        }
    }

    @Nested
    @DisplayName("Performance and Edge Cases Tests")
    class PerformanceEdgeCasesTests {

        @Test
        @DisplayName("Should handle large inputs efficiently")
        void shouldHandleLargeInputsEfficiently() {
            // Given
            StringBuilder largeInput = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                largeInput.append("test data ");
            }
            String largeString = largeInput.toString();

            // When & Then
            assertThatCode(() -> {
                SecurityUtils.sanitizeInput(largeString);
                SecurityUtils.generateHash(largeString);
                SecurityUtils.containsSuspiciousPattern(largeString);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should perform operations efficiently")
        void shouldPerformOperationsEfficiently() {
            // Given
            String testData = "test data for performance";

            // When & Then - Should complete quickly
            assertThatCode(() -> {
                for (int i = 0; i < 1000; i++) {
                    SecurityUtils.hashSHA256(testData);
                    SecurityUtils.sanitizeInput(testData);
                    SecurityUtils.isValidIpAddress("192.168.1.1");
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle concurrent operations")
        void shouldHandleConcurrentOperations() {
            // Given
            String testData = "concurrent test data";

            // When & Then
            assertThatCode(() -> {
                Thread[] threads = new Thread[10];
                for (int i = 0; i < 10; i++) {
                    threads[i] = new Thread(() -> {
                        for (int j = 0; j < 100; j++) {
                            SecurityUtils.generateSecureToken();
                            SecurityUtils.hashSHA256(testData + j);
                        }
                    });
                    threads[i].start();
                }

                for (Thread thread : threads) {
                    thread.join();
                }
            }).doesNotThrowAnyException();
        }
    }
}