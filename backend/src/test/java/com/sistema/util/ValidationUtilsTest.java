package com.sistema.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ValidationUtils Tests")
class ValidationUtilsTest {

    @Nested
    @DisplayName("Email Validation Tests")
    class EmailValidationTests {

        @Test
        @DisplayName("Should validate correct email formats")
        void shouldValidateCorrectEmailFormats() {
            // Given
            String[] validEmails = {
                    "test@example.com",
                    "user.name@domain.co.uk",
                    "user+tag@example.org",
                    "user123@test-domain.com",
                    "a@b.co",
                    "test.email.with+symbol@example.com"
            };

            // When & Then
            for (String email : validEmails) {
                assertThat(ValidationUtils.isValidEmail(email))
                        .as("Email %s should be valid", email)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Should reject invalid email formats")
        void shouldRejectInvalidEmailFormats() {
            // Given
            String[] invalidEmails = {
                    "invalid-email",
                    "@example.com",
                    "test@",
                    "test..test@example.com",
                    "test@example",
                    "test@.com",
                    "test@example.",
                    "test space@example.com",
                    "test@exam ple.com"
            };

            // When & Then
            for (String email : invalidEmails) {
                assertThat(ValidationUtils.isValidEmail(email))
                        .as("Email %s should be invalid", email)
                        .isFalse();
            }
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty emails")
        void shouldHandleNullAndEmptyEmails(String email) {
            // When & Then
            assertThat(ValidationUtils.isValidEmail(email)).isFalse();
        }
    }

    @Nested
    @DisplayName("CPF Validation Tests")
    class CpfValidationTests {

        @Test
        @DisplayName("Should validate correct CPF formats")
        void shouldValidateCorrectCpfFormats() {
            // Given
            String[] validCpfs = {
                    "11144477735", // Valid CPF
                    "111.444.777-35", // Valid CPF with formatting
                    "12345678909", // Valid CPF
                    "123.456.789-09" // Valid CPF with formatting
            };

            // When & Then
            for (String cpf : validCpfs) {
                assertThat(ValidationUtils.isValidCpf(cpf))
                        .as("CPF %s should be valid", cpf)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Should reject invalid CPF formats")
        void shouldRejectInvalidCpfFormats() {
            // Given
            String[] invalidCpfs = {
                    "11111111111", // All same digits
                    "12345678901", // Invalid check digits
                    "123.456.789-00", // Invalid check digits
                    "1234567890", // Too short
                    "123456789012", // Too long
                    "abc.def.ghi-jk", // Non-numeric
                    "123.456.78-90" // Wrong format
            };

            // When & Then
            for (String cpf : invalidCpfs) {
                assertThat(ValidationUtils.isValidCpf(cpf))
                        .as("CPF %s should be invalid", cpf)
                        .isFalse();
            }
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty CPFs")
        void shouldHandleNullAndEmptyCpfs(String cpf) {
            // When & Then
            assertThat(ValidationUtils.isValidCpf(cpf)).isFalse();
        }
    }

    @Nested
    @DisplayName("Password Validation Tests")
    class PasswordValidationTests {

        @Test
        @DisplayName("Should validate strong passwords")
        void shouldValidateStrongPasswords() {
            // Given
            String[] strongPasswords = {
                    "Password123!",
                    "MyStr0ng@Pass",
                    "C0mpl3x#P4ssw0rd",
                    "Secure123$",
                    "Valid@Pass1"
            };

            // When & Then
            for (String password : strongPasswords) {
                assertThat(ValidationUtils.isValidPassword(password))
                        .as("Password %s should be valid", password)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Should reject weak passwords")
        void shouldRejectWeakPasswords() {
            // Given
            String[] weakPasswords = {
                    "password", // No uppercase, numbers, or special chars
                    "PASSWORD", // No lowercase, numbers, or special chars
                    "12345678", // No letters or special chars
                    "Pass123", // Too short
                    "password123", // No uppercase or special chars
                    "PASSWORD123", // No lowercase or special chars
                    "Password!", // No numbers
                    "Password123" // No special chars
            };

            // When & Then
            for (String password : weakPasswords) {
                assertThat(ValidationUtils.isValidPassword(password))
                        .as("Password %s should be invalid", password)
                        .isFalse();
            }
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty passwords")
        void shouldHandleNullAndEmptyPasswords(String password) {
            // When & Then
            assertThat(ValidationUtils.isValidPassword(password)).isFalse();
        }
    }

    @Nested
    @DisplayName("Phone Validation Tests")
    class PhoneValidationTests {

        @Test
        @DisplayName("Should validate correct phone formats")
        void shouldValidateCorrectPhoneFormats() {
            // Given
            String[] validPhones = {
                    "11987654321", // Mobile with area code
                    "(11) 98765-4321", // Formatted mobile
                    "1133334444", // Landline with area code
                    "(11) 3333-4444", // Formatted landline
                    "+5511987654321", // International format
                    "11 98765-4321" // Space formatted
            };

            // When & Then
            for (String phone : validPhones) {
                assertThat(ValidationUtils.isValidPhone(phone))
                        .as("Phone %s should be valid", phone)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Should reject invalid phone formats")
        void shouldRejectInvalidPhoneFormats() {
            // Given
            String[] invalidPhones = {
                    "123456789", // Too short
                    "123456789012", // Too long
                    "abcdefghijk", // Non-numeric
                    "1234567890", // Missing area code
                    "(11) 1234-567", // Wrong format
                    "11 1234 5678" // Wrong format
            };

            // When & Then
            for (String phone : invalidPhones) {
                assertThat(ValidationUtils.isValidPhone(phone))
                        .as("Phone %s should be invalid", phone)
                        .isFalse();
            }
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty phones")
        void shouldHandleNullAndEmptyPhones(String phone) {
            // When & Then
            assertThat(ValidationUtils.isValidPhone(phone)).isFalse();
        }
    }

    @Nested
    @DisplayName("String Validation Tests")
    class StringValidationTests {

        @Test
        @DisplayName("Should validate non-blank strings")
        void shouldValidateNonBlankStrings() {
            // Given
            String[] validStrings = {
                    "valid string",
                    "a",
                    "123",
                    "special@chars!",
                    "   text with spaces   "
            };

            // When & Then
            for (String str : validStrings) {
                assertThat(ValidationUtils.isNotBlank(str))
                        .as("String '%s' should not be blank", str)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Should detect blank strings")
        void shouldDetectBlankStrings() {
            // Given
            String[] blankStrings = {
                    "",
                    "   ",
                    "\t",
                    "\n",
                    "\r\n",
                    "   \t\n   "
            };

            // When & Then
            for (String str : blankStrings) {
                assertThat(ValidationUtils.isNotBlank(str))
                        .as("String '%s' should be blank", str)
                        .isFalse();
            }
        }

        @Test
        @DisplayName("Should handle null strings")
        void shouldHandleNullStrings() {
            // When & Then
            assertThat(ValidationUtils.isNotBlank(null)).isFalse();
        }

        @Test
        @DisplayName("Should validate string length ranges")
        void shouldValidateStringLengthRanges() {
            // Given
            String testString = "test string";

            // When & Then
            assertThat(ValidationUtils.isValidLength(testString, 5, 20)).isTrue();
            assertThat(ValidationUtils.isValidLength(testString, 15, 20)).isFalse();
            assertThat(ValidationUtils.isValidLength(testString, 1, 5)).isFalse();
            assertThat(ValidationUtils.isValidLength(null, 1, 10)).isFalse();
        }
    }

    @Nested
    @DisplayName("Numeric Validation Tests")
    class NumericValidationTests {

        @Test
        @DisplayName("Should validate numeric strings")
        void shouldValidateNumericStrings() {
            // Given
            String[] numericStrings = {
                    "123",
                    "0",
                    "999999",
                    "1234567890"
            };

            // When & Then
            for (String str : numericStrings) {
                assertThat(ValidationUtils.isNumeric(str))
                        .as("String '%s' should be numeric", str)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Should reject non-numeric strings")
        void shouldRejectNonNumericStrings() {
            // Given
            String[] nonNumericStrings = {
                    "abc",
                    "123abc",
                    "12.34",
                    "-123",
                    "+123",
                    "1 2 3",
                    "1,234"
            };

            // When & Then
            for (String str : nonNumericStrings) {
                assertThat(ValidationUtils.isNumeric(str))
                        .as("String '%s' should not be numeric", str)
                        .isFalse();
            }
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty numeric strings")
        void shouldHandleNullAndEmptyNumericStrings(String str) {
            // When & Then
            assertThat(ValidationUtils.isNumeric(str)).isFalse();
        }

        @Test
        @DisplayName("Should validate number ranges")
        void shouldValidateNumberRanges() {
            // When & Then
            assertThat(ValidationUtils.isInRange(5, 1, 10)).isTrue();
            assertThat(ValidationUtils.isInRange(1, 1, 10)).isTrue();
            assertThat(ValidationUtils.isInRange(10, 1, 10)).isTrue();
            assertThat(ValidationUtils.isInRange(0, 1, 10)).isFalse();
            assertThat(ValidationUtils.isInRange(11, 1, 10)).isFalse();
        }
    }

    @Nested
    @DisplayName("URL Validation Tests")
    class UrlValidationTests {

        @Test
        @DisplayName("Should validate correct URL formats")
        void shouldValidateCorrectUrlFormats() {
            // Given
            String[] validUrls = {
                    "http://example.com",
                    "https://www.example.com",
                    "https://example.com/path",
                    "https://example.com/path?param=value",
                    "https://subdomain.example.com",
                    "http://localhost:8080",
                    "https://example.com:443/secure"
            };

            // When & Then
            for (String url : validUrls) {
                assertThat(ValidationUtils.isValidUrl(url))
                        .as("URL %s should be valid", url)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Should reject invalid URL formats")
        void shouldRejectInvalidUrlFormats() {
            // Given
            String[] invalidUrls = {
                    "not-a-url",
                    "ftp://example.com", // Wrong protocol
                    "http://",
                    "https://",
                    "example.com", // Missing protocol
                    "http:// example.com", // Space in URL
                    "https://exam ple.com" // Space in domain
            };

            // When & Then
            for (String url : invalidUrls) {
                assertThat(ValidationUtils.isValidUrl(url))
                        .as("URL %s should be invalid", url)
                        .isFalse();
            }
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty URLs")
        void shouldHandleNullAndEmptyUrls(String url) {
            // When & Then
            assertThat(ValidationUtils.isValidUrl(url)).isFalse();
        }
    }

    @Nested
    @DisplayName("Date Validation Tests")
    class DateValidationTests {

        @Test
        @DisplayName("Should validate correct date formats")
        void shouldValidateCorrectDateFormats() {
            // Given
            String[] validDates = {
                    "2023-12-25", // ISO format
                    "25/12/2023", // Brazilian format
                    "12/25/2023", // US format
                    "2023-01-01",
                    "31/01/2023"
            };

            // When & Then
            for (String date : validDates) {
                assertThat(ValidationUtils.isValidDate(date))
                        .as("Date %s should be valid", date)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("Should reject invalid date formats")
        void shouldRejectInvalidDateFormats() {
            // Given
            String[] invalidDates = {
                    "2023-13-01", // Invalid month
                    "2023-02-30", // Invalid day for February
                    "32/01/2023", // Invalid day
                    "01/13/2023", // Invalid month in US format
                    "not-a-date",
                    "2023/12/25", // Wrong separator
                    "25-12-2023" // Wrong separator for Brazilian format
            };

            // When & Then
            for (String date : invalidDates) {
                assertThat(ValidationUtils.isValidDate(date))
                        .as("Date %s should be invalid", date)
                        .isFalse();
            }
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty dates")
        void shouldHandleNullAndEmptyDates(String date) {
            // When & Then
            assertThat(ValidationUtils.isValidDate(date)).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Performance Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long strings")
        void shouldHandleVeryLongStrings() {
            // Given
            StringBuilder longString = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                longString.append("a");
            }
            String veryLongString = longString.toString();

            // When & Then
            assertThatCode(() -> {
                ValidationUtils.isNotBlank(veryLongString);
                ValidationUtils.isValidLength(veryLongString, 1, 20000);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle special characters in validation")
        void shouldHandleSpecialCharactersInValidation() {
            // Given
            String specialChars = "!@#$%^&*()_+-=[]{}|;':,.<>?";

            // When & Then
            assertThat(ValidationUtils.isNotBlank(specialChars)).isTrue();
            assertThat(ValidationUtils.isNumeric(specialChars)).isFalse();
        }

        @Test
        @DisplayName("Should handle unicode characters")
        void shouldHandleUnicodeCharacters() {
            // Given
            String unicodeString = "José da Silva ção";
            String unicodeEmail = "josé@exãmple.com";

            // When & Then
            assertThat(ValidationUtils.isNotBlank(unicodeString)).isTrue();
            assertThat(ValidationUtils.isValidEmail(unicodeEmail)).isFalse(); // Most email validators reject unicode
        }

        @Test
        @DisplayName("Should perform validation efficiently")
        void shouldPerformValidationEfficiently() {
            // Given
            String testEmail = "test@example.com";
            String testCpf = "12345678909";
            String testPassword = "Password123!";

            // When & Then - Should complete quickly
            assertThatCode(() -> {
                for (int i = 0; i < 1000; i++) {
                    ValidationUtils.isValidEmail(testEmail);
                    ValidationUtils.isValidCpf(testCpf);
                    ValidationUtils.isValidPassword(testPassword);
                }
            }).doesNotThrowAnyException();
        }
    }
}