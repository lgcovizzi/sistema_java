package com.sistema.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CpfGenerator Tests")
class CpfGeneratorTest {

    @Nested
    @DisplayName("Basic CPF Generation")
    class BasicCpfGenerationTests {

        @RepeatedTest(10)
        @DisplayName("Should generate valid CPF with formatting")
        void shouldGenerateValidCpfWithFormatting() {
            // When
            String cpf = CpfGenerator.generateValidCpf();

            // Then
            assertThat(cpf).isNotNull();
            assertThat(cpf).matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
            assertThat(ValidationUtils.isValidCpf(cpf)).isTrue();
        }

        @RepeatedTest(10)
        @DisplayName("Should generate valid CPF without formatting")
        void shouldGenerateValidCpfWithoutFormatting() {
            // When
            String cpf = CpfGenerator.generateValidCpf(false);

            // Then
            assertThat(cpf).isNotNull();
            assertThat(cpf).matches("\\d{11}");
            assertThat(cpf).hasSize(11);
            assertThat(ValidationUtils.isValidCpf(cpf)).isTrue();
        }

        @Test
        @DisplayName("Should generate different CPFs on multiple calls")
        void shouldGenerateDifferentCpfsOnMultipleCalls() {
            // When
            Set<String> generatedCpfs = new HashSet<>();
            for (int i = 0; i < 20; i++) {
                generatedCpfs.add(CpfGenerator.generateValidCpf(false));
            }

            // Then - Should have generated at least 15 different CPFs (allowing some duplicates due to randomness)
            assertThat(generatedCpfs).hasSizeGreaterThan(15);
        }
    }

    @Nested
    @DisplayName("Multiple CPF Generation")
    class MultipleCpfGenerationTests {

        @Test
        @DisplayName("Should generate multiple valid CPFs with formatting")
        void shouldGenerateMultipleValidCpfsWithFormatting() {
            // Given
            int count = 5;

            // When
            List<String> cpfs = CpfGenerator.generateMultipleValidCpfs(count, true);

            // Then
            assertThat(cpfs).hasSize(count);
            cpfs.forEach(cpf -> {
                assertThat(cpf).matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
                assertThat(ValidationUtils.isValidCpf(cpf)).isTrue();
            });
        }

        @Test
        @DisplayName("Should generate multiple valid CPFs without formatting")
        void shouldGenerateMultipleValidCpfsWithoutFormatting() {
            // Given
            int count = 5;

            // When
            List<String> cpfs = CpfGenerator.generateMultipleValidCpfs(count, false);

            // Then
            assertThat(cpfs).hasSize(count);
            cpfs.forEach(cpf -> {
                assertThat(cpf).matches("\\d{11}");
                assertThat(ValidationUtils.isValidCpf(cpf)).isTrue();
            });
        }

        @Test
        @DisplayName("Should generate multiple CPFs with default formatting")
        void shouldGenerateMultipleCpfsWithDefaultFormatting() {
            // Given
            int count = 3;

            // When
            List<String> cpfs = CpfGenerator.generateMultipleValidCpfs(count);

            // Then
            assertThat(cpfs).hasSize(count);
            cpfs.forEach(cpf -> {
                assertThat(cpf).matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
                assertThat(ValidationUtils.isValidCpf(cpf)).isTrue();
            });
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 5, 10, 50, 100})
        @DisplayName("Should generate correct number of CPFs")
        void shouldGenerateCorrectNumberOfCpfs(int count) {
            // When
            List<String> cpfs = CpfGenerator.generateMultipleValidCpfs(count);

            // Then
            assertThat(cpfs).hasSize(count);
            assertThat(cpfs).allMatch(ValidationUtils::isValidCpf);
        }

        @Test
        @DisplayName("Should throw exception for invalid count")
        void shouldThrowExceptionForInvalidCount() {
            // When & Then
            assertThatThrownBy(() -> CpfGenerator.generateMultipleValidCpfs(0))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> CpfGenerator.generateMultipleValidCpfs(-1))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> CpfGenerator.generateMultipleValidCpfs(1001))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("CPF Generation from Sequence")
    class CpfGenerationFromSequenceTests {

        @Test
        @DisplayName("Should generate CPF from valid sequence with formatting")
        void shouldGenerateCpfFromValidSequenceWithFormatting() {
            // Given
            String sequence = "123456789";

            // When
            String cpf = CpfGenerator.generateCpfFromSequence(sequence, true);

            // Then
            assertThat(cpf).isNotNull();
            assertThat(cpf).matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
            assertThat(cpf).startsWith("123.456.789");
            assertThat(ValidationUtils.isValidCpf(cpf)).isTrue();
        }

        @Test
        @DisplayName("Should generate CPF from valid sequence without formatting")
        void shouldGenerateCpfFromValidSequenceWithoutFormatting() {
            // Given
            String sequence = "123456789";

            // When
            String cpf = CpfGenerator.generateCpfFromSequence(sequence, false);

            // Then
            assertThat(cpf).isNotNull();
            assertThat(cpf).matches("\\d{11}");
            assertThat(cpf).startsWith("123456789");
            assertThat(ValidationUtils.isValidCpf(cpf)).isTrue();
        }

        @Test
        @DisplayName("Should generate CPF from sequence with default formatting")
        void shouldGenerateCpfFromSequenceWithDefaultFormatting() {
            // Given
            String sequence = "987654321";

            // When
            String cpf = CpfGenerator.generateCpfFromSequence(sequence);

            // Then
            assertThat(cpf).isNotNull();
            assertThat(cpf).matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
            assertThat(cpf).startsWith("987.654.321");
            assertThat(ValidationUtils.isValidCpf(cpf)).isTrue();
        }

        @Test
        @DisplayName("Should generate same CPF for same sequence")
        void shouldGenerateSameCpfForSameSequence() {
            // Given
            String sequence = "111222333";

            // When
            String cpf1 = CpfGenerator.generateCpfFromSequence(sequence);
            String cpf2 = CpfGenerator.generateCpfFromSequence(sequence);

            // Then
            assertThat(cpf1).isEqualTo(cpf2);
            assertThat(ValidationUtils.isValidCpf(cpf1)).isTrue();
        }

        @Test
        @DisplayName("Should throw exception for invalid sequence")
        void shouldThrowExceptionForInvalidSequence() {
            // When & Then
            assertThatThrownBy(() -> CpfGenerator.generateCpfFromSequence(null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> CpfGenerator.generateCpfFromSequence(""))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> CpfGenerator.generateCpfFromSequence("12345678"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exatamente 9 dígitos");

            assertThatThrownBy(() -> CpfGenerator.generateCpfFromSequence("1234567890"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exatamente 9 dígitos");

            assertThatThrownBy(() -> CpfGenerator.generateCpfFromSequence("12345678a"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exatamente 9 dígitos numéricos");
        }
    }

    @Nested
    @DisplayName("Example and Validation Methods")
    class ExampleAndValidationTests {

        @Test
        @DisplayName("Should generate consistent example CPF")
        void shouldGenerateConsistentExampleCpf() {
            // When
            String example1 = CpfGenerator.generateExampleCpf();
            String example2 = CpfGenerator.generateExampleCpf();

            // Then
            assertThat(example1).isEqualTo(example2);
            assertThat(example1).isEqualTo("123.456.789-09");
            assertThat(ValidationUtils.isValidCpf(example1)).isTrue();
        }

        @Test
        @DisplayName("Should validate generated CPF correctly")
        void shouldValidateGeneratedCpfCorrectly() {
            // Given
            String validCpf = CpfGenerator.generateValidCpf();
            String invalidCpf = "111.111.111-11";

            // When & Then
            assertThat(CpfGenerator.validateGeneratedCpf(validCpf)).isTrue();
            assertThat(CpfGenerator.validateGeneratedCpf(invalidCpf)).isFalse();
        }

        @Test
        @DisplayName("Should generate and validate CPF result")
        void shouldGenerateAndValidateCpfResult() {
            // When
            CpfGenerator.CpfGenerationResult result = CpfGenerator.generateAndValidate();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFormattedCpf()).matches("\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
            assertThat(result.getUnformattedCpf()).matches("\\d{11}");
            assertThat(result.isValid()).isTrue();
            assertThat(result.toString()).contains("CPF:");
            assertThat(result.toString()).contains("Válido: true");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle edge sequences correctly")
        void shouldHandleEdgeSequencesCorrectly() {
            // Given - Using sequences that will generate valid CPFs
            String[] edgeSequences = {
                    "123456789",
                    "987654321",
                    "111222333",
                    "555666777"
            };

            // When & Then
            for (String sequence : edgeSequences) {
                String cpf = CpfGenerator.generateCpfFromSequence(sequence);
                assertThat(ValidationUtils.isValidCpf(cpf)).isTrue();
            }
        }

        @Test
        @DisplayName("Should generate unique CPFs for different sequences")
        void shouldGenerateUniqueCpfsForDifferentSequences() {
            // Given
            String[] sequences = {"111111111", "222222222", "333333333"};

            // When
            Set<String> generatedCpfs = new HashSet<>();
            for (String sequence : sequences) {
                generatedCpfs.add(CpfGenerator.generateCpfFromSequence(sequence));
            }

            // Then
            assertThat(generatedCpfs).hasSize(sequences.length);
        }

        @Test
        @DisplayName("Should handle maximum count generation")
        void shouldHandleMaximumCountGeneration() {
            // When
            List<String> cpfs = CpfGenerator.generateMultipleValidCpfs(1000);

            // Then
            assertThat(cpfs).hasSize(1000);
            assertThat(cpfs).allMatch(ValidationUtils::isValidCpf);
        }
    }

    @Nested
    @DisplayName("CpfGenerationResult Tests")
    class CpfGenerationResultTests {

        @Test
        @DisplayName("Should create result with correct values")
        void shouldCreateResultWithCorrectValues() {
            // Given
            String formatted = "123.456.789-09";
            String unformatted = "12345678909";
            boolean isValid = true;

            // When
            CpfGenerator.CpfGenerationResult result = 
                    new CpfGenerator.CpfGenerationResult(formatted, unformatted, isValid);

            // Then
            assertThat(result.getFormattedCpf()).isEqualTo(formatted);
            assertThat(result.getUnformattedCpf()).isEqualTo(unformatted);
            assertThat(result.isValid()).isEqualTo(isValid);
        }

        @Test
        @DisplayName("Should format toString correctly")
        void shouldFormatToStringCorrectly() {
            // Given
            CpfGenerator.CpfGenerationResult result = 
                    new CpfGenerator.CpfGenerationResult("123.456.789-09", "12345678909", true);

            // When
            String toString = result.toString();

            // Then
            assertThat(toString).contains("CPF: 123.456.789-09");
            assertThat(toString).contains("sem formatação: 12345678909");
            assertThat(toString).contains("Válido: true");
        }
    }
}