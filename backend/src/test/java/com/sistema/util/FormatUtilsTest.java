package com.sistema.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FormatUtils Tests")
class FormatUtilsTest {

    @Nested
    @DisplayName("CPF Formatting Tests")
    class CpfFormattingTests {

        @Test
        @DisplayName("Should format CPF with dots and dash")
        void shouldFormatCpfWithDotsAndDash() {
            // Given
            String unformattedCpf = "12345678909";

            // When
            String formatted = FormatUtils.formatCpf(unformattedCpf);

            // Then
            assertThat(formatted).isEqualTo("123.456.789-09");
        }

        @Test
        @DisplayName("Should handle already formatted CPF")
        void shouldHandleAlreadyFormattedCpf() {
            // Given
            String formattedCpf = "123.456.789-09";

            // When
            String result = FormatUtils.formatCpf(formattedCpf);

            // Then
            assertThat(result).isEqualTo("123.456.789-09");
        }

        @Test
        @DisplayName("Should remove CPF formatting")
        void shouldRemoveCpfFormatting() {
            // Given
            String formattedCpf = "123.456.789-09";

            // When
            String unformatted = FormatUtils.removeCpfFormatting(formattedCpf);

            // Then
            assertThat(unformatted).isEqualTo("12345678909");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty CPF")
        void shouldHandleNullAndEmptyCpf(String cpf) {
            // When & Then
            assertThat(FormatUtils.formatCpf(cpf)).isEqualTo(cpf);
            assertThat(FormatUtils.removeCpfFormatting(cpf)).isEqualTo(cpf);
        }

        @Test
        @DisplayName("Should handle invalid CPF length")
        void shouldHandleInvalidCpfLength() {
            // Given
            String shortCpf = "123456789";
            String longCpf = "123456789012";

            // When & Then
            assertThat(FormatUtils.formatCpf(shortCpf)).isEqualTo(shortCpf);
            assertThat(FormatUtils.formatCpf(longCpf)).isEqualTo(longCpf);
        }
    }

    @Nested
    @DisplayName("CNPJ Formatting Tests")
    class CnpjFormattingTests {

        @Test
        @DisplayName("Should format CNPJ with dots, slash and dash")
        void shouldFormatCnpjWithDotsSlashAndDash() {
            // Given
            String unformattedCnpj = "12345678000195";

            // When
            String formatted = FormatUtils.formatCnpj(unformattedCnpj);

            // Then
            assertThat(formatted).isEqualTo("12.345.678/0001-95");
        }

        @Test
        @DisplayName("Should handle already formatted CNPJ")
        void shouldHandleAlreadyFormattedCnpj() {
            // Given
            String formattedCnpj = "12.345.678/0001-95";

            // When
            String result = FormatUtils.formatCnpj(formattedCnpj);

            // Then
            assertThat(result).isEqualTo("12.345.678/0001-95");
        }

        @Test
        @DisplayName("Should remove CNPJ formatting")
        void shouldRemoveCnpjFormatting() {
            // Given
            String formattedCnpj = "12.345.678/0001-95";

            // When
            String unformatted = FormatUtils.removeCnpjFormatting(formattedCnpj);

            // Then
            assertThat(unformatted).isEqualTo("12345678000195");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty CNPJ")
        void shouldHandleNullAndEmptyCnpj(String cnpj) {
            // When & Then
            assertThat(FormatUtils.formatCnpj(cnpj)).isEqualTo(cnpj);
            assertThat(FormatUtils.removeCnpjFormatting(cnpj)).isEqualTo(cnpj);
        }
    }

    @Nested
    @DisplayName("Phone Formatting Tests")
    class PhoneFormattingTests {

        @Test
        @DisplayName("Should format mobile phone with area code")
        void shouldFormatMobilePhoneWithAreaCode() {
            // Given
            String unformattedPhone = "11987654321";

            // When
            String formatted = FormatUtils.formatPhone(unformattedPhone);

            // Then
            assertThat(formatted).isEqualTo("(11) 98765-4321");
        }

        @Test
        @DisplayName("Should format landline phone with area code")
        void shouldFormatLandlinePhoneWithAreaCode() {
            // Given
            String unformattedPhone = "1133334444";

            // When
            String formatted = FormatUtils.formatPhone(unformattedPhone);

            // Then
            assertThat(formatted).isEqualTo("(11) 3333-4444");
        }

        @Test
        @DisplayName("Should handle already formatted phone")
        void shouldHandleAlreadyFormattedPhone() {
            // Given
            String formattedPhone = "(11) 98765-4321";

            // When
            String result = FormatUtils.formatPhone(formattedPhone);

            // Then
            assertThat(result).isEqualTo("(11) 98765-4321");
        }

        @Test
        @DisplayName("Should remove phone formatting")
        void shouldRemovePhoneFormatting() {
            // Given
            String formattedPhone = "(11) 98765-4321";

            // When
            String unformatted = FormatUtils.removePhoneFormatting(formattedPhone);

            // Then
            assertThat(unformatted).isEqualTo("11987654321");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty phone")
        void shouldHandleNullAndEmptyPhone(String phone) {
            // When & Then
            assertThat(FormatUtils.formatPhone(phone)).isEqualTo(phone);
            assertThat(FormatUtils.removePhoneFormatting(phone)).isEqualTo(phone);
        }
    }

    @Nested
    @DisplayName("Currency Formatting Tests")
    class CurrencyFormattingTests {

        @Test
        @DisplayName("Should format currency in Brazilian Real")
        void shouldFormatCurrencyInBrazilianReal() {
            // Given
            BigDecimal amount = new BigDecimal("1234.56");

            // When
            String formatted = FormatUtils.formatCurrency(amount);

            // Then
            assertThat(formatted).isEqualTo("R$ 1.234,56");
        }

        @Test
        @DisplayName("Should format zero currency")
        void shouldFormatZeroCurrency() {
            // Given
            BigDecimal zero = BigDecimal.ZERO;

            // When
            String formatted = FormatUtils.formatCurrency(zero);

            // Then
            assertThat(formatted).isEqualTo("R$ 0,00");
        }

        @Test
        @DisplayName("Should format large currency amounts")
        void shouldFormatLargeCurrencyAmounts() {
            // Given
            BigDecimal largeAmount = new BigDecimal("1234567.89");

            // When
            String formatted = FormatUtils.formatCurrency(largeAmount);

            // Then
            assertThat(formatted).isEqualTo("R$ 1.234.567,89");
        }

        @Test
        @DisplayName("Should format negative currency")
        void shouldFormatNegativeCurrency() {
            // Given
            BigDecimal negativeAmount = new BigDecimal("-123.45");

            // When
            String formatted = FormatUtils.formatCurrency(negativeAmount);

            // Then
            assertThat(formatted).contains("-").contains("123,45");
        }

        @Test
        @DisplayName("Should handle null currency")
        void shouldHandleNullCurrency() {
            // When & Then
            assertThat(FormatUtils.formatCurrency(null)).isEqualTo("R$ 0,00");
        }

        @Test
        @DisplayName("Should format currency with custom symbol")
        void shouldFormatCurrencyWithCustomSymbol() {
            // Given
            BigDecimal amount = new BigDecimal("100.50");
            String symbol = "US$";

            // When
            String formatted = FormatUtils.formatCurrency(amount, symbol);

            // Then
            assertThat(formatted).isEqualTo("US$ 100,50");
        }
    }

    @Nested
    @DisplayName("Date Formatting Tests")
    class DateFormattingTests {

        @Test
        @DisplayName("Should format date in Brazilian format")
        void shouldFormatDateInBrazilianFormat() {
            // Given
            LocalDate date = LocalDate.of(2023, 12, 25);

            // When
            String formatted = FormatUtils.formatDate(date);

            // Then
            assertThat(formatted).isEqualTo("25/12/2023");
        }

        @Test
        @DisplayName("Should format datetime in Brazilian format")
        void shouldFormatDatetimeInBrazilianFormat() {
            // Given
            LocalDateTime datetime = LocalDateTime.of(2023, 12, 25, 14, 30, 45);

            // When
            String formatted = FormatUtils.formatDateTime(datetime);

            // Then
            assertThat(formatted).isEqualTo("25/12/2023 14:30:45");
        }

        @Test
        @DisplayName("Should format date with custom pattern")
        void shouldFormatDateWithCustomPattern() {
            // Given
            LocalDate date = LocalDate.of(2023, 12, 25);
            String pattern = "yyyy-MM-dd";

            // When
            String formatted = FormatUtils.formatDate(date, pattern);

            // Then
            assertThat(formatted).isEqualTo("2023-12-25");
        }

        @Test
        @DisplayName("Should format datetime with custom pattern")
        void shouldFormatDatetimeWithCustomPattern() {
            // Given
            LocalDateTime datetime = LocalDateTime.of(2023, 12, 25, 14, 30, 45);
            String pattern = "yyyy-MM-dd HH:mm";

            // When
            String formatted = FormatUtils.formatDateTime(datetime, pattern);

            // Then
            assertThat(formatted).isEqualTo("2023-12-25 14:30");
        }

        @Test
        @DisplayName("Should handle null dates")
        void shouldHandleNullDates() {
            // When & Then
            assertThat(FormatUtils.formatDate((LocalDate) null)).isEmpty();
            assertThat(FormatUtils.formatDateTime(null)).isEmpty();
            assertThat(FormatUtils.formatDate((LocalDate) null, "yyyy-MM-dd")).isEmpty();
        }

        @Test
        @DisplayName("Should parse date from Brazilian format")
        void shouldParseDateFromBrazilianFormat() {
            // Given
            String dateString = "25/12/2023";

            // When
            LocalDate parsed = FormatUtils.parseDate(dateString);

            // Then
            assertThat(parsed).isEqualTo(LocalDate.of(2023, 12, 25));
        }

        @Test
        @DisplayName("Should parse datetime from Brazilian format")
        void shouldParseDatetimeFromBrazilianFormat() {
            // Given
            String datetimeString = "25/12/2023 14:30:45";

            // When
            LocalDateTime parsed = FormatUtils.parseDateTime(datetimeString);

            // Then
            assertThat(parsed).isEqualTo(LocalDateTime.of(2023, 12, 25, 14, 30, 45));
        }
    }

    @Nested
    @DisplayName("String Formatting Tests")
    class StringFormattingTests {

        @Test
        @DisplayName("Should capitalize first letter")
        void shouldCapitalizeFirstLetter() {
            // Given
            String input = "hello world";

            // When
            String capitalized = FormatUtils.capitalizeFirst(input);

            // Then
            assertThat(capitalized).isEqualTo("Hello world");
        }

        @Test
        @DisplayName("Should capitalize each word")
        void shouldCapitalizeEachWord() {
            // Given
            String input = "hello world test";

            // When
            String capitalized = FormatUtils.capitalizeWords(input);

            // Then
            assertThat(capitalized).isEqualTo("Hello World Test");
        }

        @Test
        @DisplayName("Should convert to title case")
        void shouldConvertToTitleCase() {
            // Given
            String input = "HELLO WORLD";

            // When
            String titleCase = FormatUtils.toTitleCase(input);

            // Then
            assertThat(titleCase).isEqualTo("Hello World");
        }

        @Test
        @DisplayName("Should remove accents from text")
        void shouldRemoveAccentsFromText() {
            // Given
            String input = "José da Silva Ação";

            // When
            String withoutAccents = FormatUtils.removeAccents(input);

            // Then
            assertThat(withoutAccents).isEqualTo("Jose da Silva Acao");
        }

        @Test
        @DisplayName("Should normalize text for search")
        void shouldNormalizeTextForSearch() {
            // Given
            String input = "José da SILVA";

            // When
            String normalized = FormatUtils.normalizeForSearch(input);

            // Then
            assertThat(normalized).isEqualTo("jose da silva");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should handle null and empty strings")
        void shouldHandleNullAndEmptyStrings(String input) {
            // When & Then
            assertThat(FormatUtils.capitalizeFirst(input)).isEqualTo(input);
            assertThat(FormatUtils.capitalizeWords(input)).isEqualTo(input);
            assertThat(FormatUtils.toTitleCase(input)).isEqualTo(input);
            assertThat(FormatUtils.removeAccents(input)).isEqualTo(input);
            assertThat(FormatUtils.normalizeForSearch(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("Should truncate text with ellipsis")
        void shouldTruncateTextWithEllipsis() {
            // Given
            String longText = "This is a very long text that should be truncated";
            int maxLength = 20;

            // When
            String truncated = FormatUtils.truncateWithEllipsis(longText, maxLength);

            // Then
            assertThat(truncated)
                    .hasSize(maxLength)
                    .endsWith("...");
        }

        @Test
        @DisplayName("Should not truncate short text")
        void shouldNotTruncateShortText() {
            // Given
            String shortText = "Short text";
            int maxLength = 20;

            // When
            String result = FormatUtils.truncateWithEllipsis(shortText, maxLength);

            // Then
            assertThat(result).isEqualTo(shortText);
        }
    }

    @Nested
    @DisplayName("Number Formatting Tests")
    class NumberFormattingTests {

        @Test
        @DisplayName("Should format integer with thousands separator")
        void shouldFormatIntegerWithThousandsSeparator() {
            // Given
            int number = 1234567;

            // When
            String formatted = FormatUtils.formatNumber(number);

            // Then
            assertThat(formatted).isEqualTo("1.234.567");
        }

        @Test
        @DisplayName("Should format decimal with Brazilian format")
        void shouldFormatDecimalWithBrazilianFormat() {
            // Given
            double number = 1234.56;

            // When
            String formatted = FormatUtils.formatDecimal(number);

            // Then
            assertThat(formatted).isEqualTo("1.234,56");
        }

        @Test
        @DisplayName("Should format percentage")
        void shouldFormatPercentage() {
            // Given
            double percentage = 0.1234;

            // When
            String formatted = FormatUtils.formatPercentage(percentage);

            // Then
            assertThat(formatted).isEqualTo("12,34%");
        }

        @Test
        @DisplayName("Should format decimal with custom precision")
        void shouldFormatDecimalWithCustomPrecision() {
            // Given
            double number = 123.456789;
            int decimalPlaces = 3;

            // When
            String formatted = FormatUtils.formatDecimal(number, decimalPlaces);

            // Then
            assertThat(formatted).isEqualTo("123,457"); // Rounded
        }

        @Test
        @DisplayName("Should format file size")
        void shouldFormatFileSize() {
            // Given & When & Then
            assertThat(FormatUtils.formatFileSize(1024)).isEqualTo("1,00 KB");
            assertThat(FormatUtils.formatFileSize(1048576)).isEqualTo("1,00 MB");
            assertThat(FormatUtils.formatFileSize(1073741824)).isEqualTo("1,00 GB");
            assertThat(FormatUtils.formatFileSize(500)).isEqualTo("500 B");
        }
    }

    @Nested
    @DisplayName("Validation and Edge Cases Tests")
    class ValidationEdgeCasesTests {

        @Test
        @DisplayName("Should handle special characters in formatting")
        void shouldHandleSpecialCharactersInFormatting() {
            // Given
            String specialText = "Text with @#$%^&*() characters";

            // When & Then
            assertThatCode(() -> {
                FormatUtils.capitalizeFirst(specialText);
                FormatUtils.removeAccents(specialText);
                FormatUtils.normalizeForSearch(specialText);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle unicode characters")
        void shouldHandleUnicodeCharacters() {
            // Given
            String unicodeText = "Texto com ção, ã, é, ü, ñ";

            // When
            String withoutAccents = FormatUtils.removeAccents(unicodeText);
            String normalized = FormatUtils.normalizeForSearch(unicodeText);

            // Then
            assertThat(withoutAccents).doesNotContain("ç", "ã", "é", "ü", "ñ");
            assertThat(normalized).isLowerCase();
        }

        @Test
        @DisplayName("Should handle very large numbers")
        void shouldHandleVeryLargeNumbers() {
            // Given
            long largeNumber = 999999999999L;
            BigDecimal largeCurrency = new BigDecimal("999999999999.99");

            // When & Then
            assertThatCode(() -> {
                FormatUtils.formatNumber(largeNumber);
                FormatUtils.formatCurrency(largeCurrency);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle invalid date strings gracefully")
        void shouldHandleInvalidDateStringsGracefully() {
            // Given
            String[] invalidDates = {
                    "invalid-date",
                    "32/13/2023",
                    "2023-13-32",
                    "not a date"
            };

            // When & Then
            for (String invalidDate : invalidDates) {
                assertThatThrownBy(() -> FormatUtils.parseDate(invalidDate))
                        .isInstanceOf(RuntimeException.class);
            }
        }

        @Test
        @DisplayName("Should perform formatting operations efficiently")
        void shouldPerformFormattingOperationsEfficiently() {
            // Given
            String testText = "Test text for performance";
            BigDecimal testAmount = new BigDecimal("1234.56");
            LocalDate testDate = LocalDate.now();

            // When & Then - Should complete quickly
            assertThatCode(() -> {
                for (int i = 0; i < 1000; i++) {
                    FormatUtils.capitalizeFirst(testText);
                    FormatUtils.formatCurrency(testAmount);
                    FormatUtils.formatDate(testDate);
                    FormatUtils.formatCpf("12345678909");
                }
            }).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @CsvSource({
                "'', ''",
                "'   ', '   '",
                "'a', 'A'",
                "'hello', 'Hello'",
                "'HELLO', 'HELLO'"
        })
        @DisplayName("Should handle various capitalization scenarios")
        void shouldHandleVariousCapitalizationScenarios(String input, String expected) {
            // When
            String result = FormatUtils.capitalizeFirst(input);

            // Then
            assertThat(result).isEqualTo(expected);
        }
    }
}