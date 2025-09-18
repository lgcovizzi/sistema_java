package com.sistema.exception;

import com.sistema.exception.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidationException Tests")
class ValidationExceptionTest {

    @Test
    @DisplayName("Deve criar ValidationException com mensagem e código de erro")
    void testCreateValidationExceptionWithMessageAndErrorCode() {
        // Given
        String message = "Erro de validação";
        String errorCode = ErrorCodes.VALIDATION_FAILED;

        // When
        ValidationException exception = new ValidationException(message, errorCode);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertNull(exception.getFieldErrors());
    }

    @Test
    @DisplayName("Deve criar ValidationException com erros de campo")
    void testCreateValidationExceptionWithFieldErrors() {
        // Given
        String message = "Erro de validação de campos";
        String errorCode = ErrorCodes.VALIDATION_FAILED;
        Map<String, List<String>> fieldErrors = new HashMap<>();
        fieldErrors.put("email", Arrays.asList("Email é obrigatório", "Email deve ter formato válido"));
        fieldErrors.put("senha", Arrays.asList("Senha é obrigatória"));

        // When
        ValidationException exception = new ValidationException(message, errorCode, fieldErrors);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(fieldErrors, exception.getFieldErrors());
        assertEquals(2, exception.getFieldErrors().size());
        assertTrue(exception.getFieldErrors().containsKey("email"));
        assertTrue(exception.getFieldErrors().containsKey("senha"));
    }

    @Test
    @DisplayName("Deve criar ValidationException com mensagem, código, erros de campo e parâmetros")
    void testCreateValidationExceptionWithAllParameters() {
        // Given
        String message = "Erro completo de validação";
        String errorCode = ErrorCodes.VALIDATION_FAILED;
        Map<String, List<String>> fieldErrors = new HashMap<>();
        fieldErrors.put("nome", Arrays.asList("Nome é obrigatório"));
        Object[] parameters = {"param1", "param2"};

        // When
        ValidationException exception = new ValidationException(message, errorCode, fieldErrors, parameters);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(fieldErrors, exception.getFieldErrors());
        assertArrayEquals(parameters, exception.getParameters());
    }

    @Test
    @DisplayName("Deve criar ValidationException com mensagem, código, erros de campo, parâmetros e causa")
    void testCreateValidationExceptionWithCause() {
        // Given
        String message = "Erro de validação com causa";
        String errorCode = ErrorCodes.VALIDATION_FAILED;
        Map<String, List<String>> fieldErrors = new HashMap<>();
        fieldErrors.put("cpf", Arrays.asList("CPF inválido"));
        Object[] parameters = {"param1"};
        Throwable cause = new IllegalArgumentException("Causa raiz");

        // When
        ValidationException exception = new ValidationException(message, errorCode, fieldErrors, parameters, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(fieldErrors, exception.getFieldErrors());
        assertArrayEquals(parameters, exception.getParameters());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Deve criar ValidationException apenas com mensagem")
    void testCreateValidationExceptionWithMessageOnly() {
        // Given
        String message = "Erro simples de validação";

        // When
        ValidationException exception = new ValidationException(message);

        // Then
        assertEquals(message, exception.getMessage());
        assertNull(exception.getErrorCode());
        assertNull(exception.getFieldErrors());
        assertNull(exception.getParameters());
    }

    @Test
    @DisplayName("Deve ser uma BusinessException")
    void testValidationExceptionIsBusinessException() {
        // Given
        ValidationException exception = new ValidationException("Teste");

        // Then
        assertInstanceOf(BusinessException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    @DisplayName("Deve permitir fieldErrors nulo")
    void testValidationExceptionWithNullFieldErrors() {
        // Given
        String message = "Erro sem field errors";
        String errorCode = ErrorCodes.VALIDATION_FAILED;

        // When
        ValidationException exception = new ValidationException(message, errorCode, null);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertNull(exception.getFieldErrors());
    }

    @Test
    @DisplayName("Deve permitir fieldErrors vazio")
    void testValidationExceptionWithEmptyFieldErrors() {
        // Given
        String message = "Erro com field errors vazio";
        String errorCode = ErrorCodes.VALIDATION_FAILED;
        Map<String, List<String>> fieldErrors = new HashMap<>();

        // When
        ValidationException exception = new ValidationException(message, errorCode, fieldErrors);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(fieldErrors, exception.getFieldErrors());
        assertTrue(exception.getFieldErrors().isEmpty());
    }

    @Test
    @DisplayName("Deve manter imutabilidade dos fieldErrors")
    void testValidationExceptionFieldErrorsImmutability() {
        // Given
        String message = "Teste imutabilidade";
        String errorCode = ErrorCodes.VALIDATION_FAILED;
        Map<String, List<String>> originalFieldErrors = new HashMap<>();
        originalFieldErrors.put("campo1", Arrays.asList("erro1"));

        // When
        ValidationException exception = new ValidationException(message, errorCode, originalFieldErrors);
        
        // Tentativa de modificar o mapa original
        originalFieldErrors.put("campo2", Arrays.asList("erro2"));

        // Then
        assertEquals(1, exception.getFieldErrors().size());
        assertFalse(exception.getFieldErrors().containsKey("campo2"));
    }

    @Test
    @DisplayName("Deve lidar com listas de erros múltiplos por campo")
    void testValidationExceptionMultipleErrorsPerField() {
        // Given
        String message = "Múltiplos erros por campo";
        String errorCode = ErrorCodes.VALIDATION_FAILED;
        Map<String, List<String>> fieldErrors = new HashMap<>();
        fieldErrors.put("senha", Arrays.asList(
            "Senha é obrigatória",
            "Senha deve ter pelo menos 8 caracteres",
            "Senha deve conter pelo menos uma letra maiúscula",
            "Senha deve conter pelo menos um número"
        ));

        // When
        ValidationException exception = new ValidationException(message, errorCode, fieldErrors);

        // Then
        assertEquals(1, exception.getFieldErrors().size());
        assertEquals(4, exception.getFieldErrors().get("senha").size());
        assertTrue(exception.getFieldErrors().get("senha").contains("Senha é obrigatória"));
        assertTrue(exception.getFieldErrors().get("senha").contains("Senha deve ter pelo menos 8 caracteres"));
    }

    @Test
    @DisplayName("Deve preservar ordem dos erros de campo")
    void testValidationExceptionPreservesFieldErrorOrder() {
        // Given
        String message = "Teste ordem dos erros";
        String errorCode = ErrorCodes.VALIDATION_FAILED;
        Map<String, List<String>> fieldErrors = new HashMap<>();
        List<String> errors = Arrays.asList("Primeiro erro", "Segundo erro", "Terceiro erro");
        fieldErrors.put("campo", errors);

        // When
        ValidationException exception = new ValidationException(message, errorCode, fieldErrors);

        // Then
        List<String> retrievedErrors = exception.getFieldErrors().get("campo");
        assertEquals("Primeiro erro", retrievedErrors.get(0));
        assertEquals("Segundo erro", retrievedErrors.get(1));
        assertEquals("Terceiro erro", retrievedErrors.get(2));
    }
}