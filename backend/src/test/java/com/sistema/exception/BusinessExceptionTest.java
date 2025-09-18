package com.sistema.exception;

import com.sistema.util.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BusinessException Tests")
class BusinessExceptionTest {

    @Test
    @DisplayName("Deve criar BusinessException com mensagem e código de erro")
    void testCreateBusinessExceptionWithMessageAndErrorCode() {
        // Given
        String message = "Erro de negócio";
        String errorCode = ErrorCodes.INVALID_REQUEST;

        // When
        BusinessException exception = new BusinessException(message, errorCode);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertNull(exception.getParameters());
    }

    @Test
    @DisplayName("Deve criar BusinessException com mensagem, código e parâmetros")
    void testCreateBusinessExceptionWithParameters() {
        // Given
        String message = "Erro com parâmetros: {0} e {1}";
        String errorCode = ErrorCodes.VALIDATION_FAILED;
        Object[] parameters = {"param1", "param2"};

        // When
        BusinessException exception = new BusinessException(message, errorCode, parameters);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertArrayEquals(parameters, exception.getParameters());
    }

    @Test
    @DisplayName("Deve criar BusinessException com mensagem, código, parâmetros e causa")
    void testCreateBusinessExceptionWithCause() {
        // Given
        String message = "Erro com causa";
        String errorCode = ErrorCodes.INTERNAL_ERROR;
        Object[] parameters = {"param1"};
        Throwable cause = new RuntimeException("Causa raiz");

        // When
        BusinessException exception = new BusinessException(message, errorCode, parameters, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertArrayEquals(parameters, exception.getParameters());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Deve criar BusinessException apenas com mensagem")
    void testCreateBusinessExceptionWithMessageOnly() {
        // Given
        String message = "Erro simples";
        String errorCode = ErrorCodes.INTERNAL_ERROR;

        // When
        BusinessException exception = new BusinessException(errorCode, message);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertNull(exception.getParameters());
    }

    @Test
    @DisplayName("Deve criar BusinessException com mensagem e causa")
    void testCreateBusinessExceptionWithMessageAndCause() {
        // Given
        String message = "Erro com causa";
        String errorCode = ErrorCodes.INTERNAL_ERROR;
        Throwable cause = new IllegalArgumentException("Argumento inválido");

        // When
        BusinessException exception = new BusinessException(errorCode, message, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(errorCode, exception.getErrorCode());
        assertNull(exception.getParameters());
    }

    @Test
    @DisplayName("Deve permitir errorCode nulo")
    void testBusinessExceptionWithNullErrorCode() {
        // Given
        String message = "Erro sem código";

        // When
        BusinessException exception = new BusinessException(null, message);

        // Then
        assertEquals(message, exception.getMessage());
        assertNull(exception.getErrorCode());
    }

    @Test
    @DisplayName("Deve permitir parâmetros nulos")
    void testBusinessExceptionWithNullParameters() {
        // Given
        String message = "Erro sem parâmetros";
        String errorCode = ErrorCodes.INVALID_REQUEST;

        // When
        BusinessException exception = new BusinessException(errorCode, message, (Object[]) null);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertNull(exception.getParameters());
    }

    @Test
    @DisplayName("Deve ser uma RuntimeException")
    void testBusinessExceptionIsRuntimeException() {
        // Given
        BusinessException exception = new BusinessException(ErrorCodes.INTERNAL_ERROR, "Teste");

        // Then
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    @DisplayName("Deve manter stack trace corretamente")
    void testBusinessExceptionStackTrace() {
        // Given/When
        BusinessException exception = new BusinessException("Teste", ErrorCodes.INVALID_REQUEST);

        // Then
        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
        assertEquals("testBusinessExceptionStackTrace", exception.getStackTrace()[0].getMethodName());
    }

    @Test
    @DisplayName("Deve permitir parâmetros vazios")
    void testBusinessExceptionWithEmptyParameters() {
        // Given
        String message = "Erro com array vazio";
        String errorCode = ErrorCodes.VALIDATION_FAILED;
        Object[] parameters = {};

        // When
        BusinessException exception = new BusinessException(message, errorCode, parameters);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertArrayEquals(parameters, exception.getParameters());
        assertEquals(0, exception.getParameters().length);
    }
}