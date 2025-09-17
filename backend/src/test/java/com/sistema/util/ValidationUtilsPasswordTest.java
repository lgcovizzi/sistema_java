package com.sistema.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para validação de senhas no ValidationUtils.
 */
@DisplayName("ValidationUtils - Password Validation Tests")
class ValidationUtilsPasswordTest {

    @Test
    @DisplayName("Deve aceitar senha válida com todos os critérios")
    void shouldAcceptValidPassword() {
        // Given
        String validPassword = "MinhaSenh@123";
        
        // When & Then
        assertDoesNotThrow(() -> ValidationUtils.validatePassword(validPassword));
        assertTrue(ValidationUtils.isValidPassword(validPassword));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Senha@123",      // 9 caracteres
        "MinhaSenh@123",  // 14 caracteres
        "P@ssw0rd",       // 9 caracteres
        "Teste123!",      // 10 caracteres
        "AbC123@def"      // 10 caracteres
    })
    @DisplayName("Deve aceitar senhas válidas com diferentes tamanhos")
    void shouldAcceptValidPasswordsWithDifferentLengths(String password) {
        assertDoesNotThrow(() -> ValidationUtils.validatePassword(password));
        assertTrue(ValidationUtils.isValidPassword(password));
    }

    @Test
    @DisplayName("Deve rejeitar senha nula")
    void shouldRejectNullPassword() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ValidationUtils.validatePassword(null)
        );
        assertEquals("password não pode ser nulo", exception.getMessage());
        assertFalse(ValidationUtils.isValidPassword(null));
    }

    @Test
    @DisplayName("Deve rejeitar senha vazia")
    void shouldRejectEmptyPassword() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ValidationUtils.validatePassword("")
        );
        assertEquals("password não pode estar vazio", exception.getMessage());
        assertFalse(ValidationUtils.isValidPassword(""));
    }

    @Test
    @DisplayName("Deve rejeitar senha com apenas espaços")
    void shouldRejectBlankPassword() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ValidationUtils.validatePassword("   ")
        );
        assertEquals("password não pode estar vazio", exception.getMessage());
        assertFalse(ValidationUtils.isValidPassword("   "));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "123456",        // 6 caracteres
        "1234567",       // 7 caracteres
        "Abc123@"        // 7 caracteres
    })
    @DisplayName("Deve rejeitar senhas com menos de 8 caracteres")
    void shouldRejectPasswordsWithLessThan8Characters(String password) {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ValidationUtils.validatePassword(password)
        );
        assertEquals("Senha deve ter pelo menos 8 caracteres", exception.getMessage());
        assertFalse(ValidationUtils.isValidPassword(password));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "MINHASENHA123@", // Sem minúscula
        "minhasenha123@", // Sem maiúscula
        "MinhaSenh@",     // Sem número
        "MinhaSenh123"    // Sem caractere especial
    })
    @DisplayName("Deve rejeitar senhas que não atendem critérios de complexidade")
    void shouldRejectPasswordsNotMeetingComplexityRequirements(String password) {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ValidationUtils.validatePassword(password)
        );
        assertEquals(
            "Senha deve conter pelo menos: 1 letra minúscula, 1 maiúscula, 1 número e 1 caractere especial",
            exception.getMessage()
        );
        assertFalse(ValidationUtils.isValidPassword(password));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Teste123!",      // Com !
        "Teste123@",      // Com @
        "Teste123#",      // Com #
        "Teste123$",      // Com $
        "Teste123%",      // Com %
        "Teste123^",      // Com ^
        "Teste123&",      // Com &
        "Teste123*",      // Com *
        "Teste123(",      // Com (
        "Teste123)",      // Com )
        "Teste123_",      // Com _
        "Teste123+",      // Com +
        "Teste123-",      // Com -
        "Teste123=",      // Com =
        "Teste123[",      // Com [
        "Teste123]",      // Com ]
        "Teste123{",      // Com {
        "Teste123}",      // Com }
        "Teste123;",      // Com ;
        "Teste123'",      // Com '
        "Teste123:",      // Com :
        "Teste123\"",     // Com "
        "Teste123\\",     // Com \
        "Teste123|",      // Com |
        "Teste123,",      // Com ,
        "Teste123.",      // Com .
        "Teste123<",      // Com <
        "Teste123>",      // Com >
        "Teste123/",      // Com /
        "Teste123?"       // Com ?
    })
    @DisplayName("Deve aceitar senhas com diferentes caracteres especiais")
    void shouldAcceptPasswordsWithDifferentSpecialCharacters(String password) {
        assertDoesNotThrow(() -> ValidationUtils.validatePassword(password));
        assertTrue(ValidationUtils.isValidPassword(password));
    }

    @Test
    @DisplayName("Deve aceitar senha com múltiplos critérios de cada tipo")
    void shouldAcceptPasswordWithMultipleCriteriaOfEachType() {
        // Given
        String password = "AbCdEf123456!@#$";
        
        // When & Then
        assertDoesNotThrow(() -> ValidationUtils.validatePassword(password));
        assertTrue(ValidationUtils.isValidPassword(password));
    }

    @Test
    @DisplayName("Deve aceitar senha longa com todos os critérios")
    void shouldAcceptLongPasswordWithAllCriteria() {
        // Given
        String password = "EstaSenhaEhMuitoLongaETemTodosOsCriterios123!@#";
        
        // When & Then
        assertDoesNotThrow(() -> ValidationUtils.validatePassword(password));
        assertTrue(ValidationUtils.isValidPassword(password));
    }
}