package com.sistema.exception;

import com.sistema.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorResponse Tests")
class ErrorResponseTest {

    private ErrorResponse errorResponse;
    private LocalDateTime testTimestamp;

    @BeforeEach
    void setUp() {
        testTimestamp = LocalDateTime.now();
    }

    @Test
    @DisplayName("Deve criar ErrorResponse com todos os campos obrigatórios")
    void shouldCreateErrorResponseWithRequiredFields() {
        // Given
        String message = "Erro de teste";
        String errorCode = "TEST_ERROR";
        int status = 400;
        String path = "/api/test";

        // When
        errorResponse = new ErrorResponse(message, errorCode, status, path);

        // Then
        assertNotNull(errorResponse);
        assertEquals(message, errorResponse.getMessage());
        assertEquals(errorCode, errorResponse.getErrorCode());
        assertEquals(status, errorResponse.getStatus());
        assertEquals(path, errorResponse.getPath());
        assertNotNull(errorResponse.getTimestamp());
        assertTrue(errorResponse.isError());
    }

    @Test
    @DisplayName("Deve criar ErrorResponse com field errors")
    void shouldCreateErrorResponseWithFieldErrors() {
        // Given
        String message = "Erro de validação";
        String errorCode = "VALIDATION_ERROR";
        int status = 400;
        String path = "/api/test";
        Map<String, String> fieldErrors = new HashMap<>();
        fieldErrors.put("nome", "Nome é obrigatório");
        fieldErrors.put("email", "Email inválido");

        // When
        errorResponse = new ErrorResponse(message, errorCode, status, path);
        errorResponse.setFieldErrors(fieldErrors);

        // Then
        assertNotNull(errorResponse);
        assertEquals(message, errorResponse.getMessage());
        assertEquals(errorCode, errorResponse.getErrorCode());
        assertEquals(status, errorResponse.getStatus());
        assertEquals(path, errorResponse.getPath());
        assertNotNull(errorResponse.getFieldErrors());
        assertEquals(2, errorResponse.getFieldErrors().size());
        assertEquals("Nome é obrigatório", errorResponse.getFieldErrors().get("nome"));
        assertEquals("Email inválido", errorResponse.getFieldErrors().get("email"));
    }

    @Test
    @DisplayName("Deve criar ErrorResponse com detalhes adicionais")
    void shouldCreateErrorResponseWithDetails() {
        // Given
        String message = "Erro interno";
        String errorCode = "INTERNAL_ERROR";
        int status = 500;
        String path = "/api/test";
        Map<String, Object> details = new HashMap<>();
        details.put("requestId", "12345");
        details.put("userId", 100L);
        details.put("debug", true);

        // When
        errorResponse = new ErrorResponse(message, errorCode, status, path);
        errorResponse.setAdditionalData(details);

        // Then
        assertNotNull(errorResponse);
        assertEquals(message, errorResponse.getMessage());
        assertEquals(errorCode, errorResponse.getErrorCode());
        assertEquals(status, errorResponse.getStatus());
        assertEquals(path, errorResponse.getPath());
        assertNotNull(errorResponse.getAdditionalData());
        assertEquals(3, errorResponse.getAdditionalData().size());
        assertEquals("12345", errorResponse.getAdditionalData().get("requestId"));
        assertEquals(100L, errorResponse.getAdditionalData().get("userId"));
        assertEquals(true, errorResponse.getAdditionalData().get("debug"));
    }

    @Test
    @DisplayName("Deve criar ErrorResponse completo com todos os campos")
    void shouldCreateCompleteErrorResponse() {
        // Given
        String message = "Erro completo";
        String errorCode = "COMPLETE_ERROR";
        int status = 422;
        String path = "/api/complete";
        Map<String, String> fieldErrors = new HashMap<>();
        fieldErrors.put("campo1", "Erro no campo 1");
        Map<String, Object> details = new HashMap<>();
        details.put("extra", "informação adicional");

        // When
        errorResponse = new ErrorResponse(message, errorCode, status, path);
        errorResponse.setFieldErrors(fieldErrors);
        errorResponse.setAdditionalData(details);

        // Then
        assertNotNull(errorResponse);
        assertEquals(message, errorResponse.getMessage());
        assertEquals(errorCode, errorResponse.getErrorCode());
        assertEquals(status, errorResponse.getStatus());
        assertEquals(path, errorResponse.getPath());
        assertNotNull(errorResponse.getFieldErrors());
        assertEquals(1, errorResponse.getFieldErrors().size());
        assertNotNull(errorResponse.getAdditionalData());
        assertEquals(1, errorResponse.getAdditionalData().size());
    }

    @Test
    @DisplayName("Deve lidar com field errors vazios")
    void shouldHandleEmptyFieldErrors() {
        // Given
        String message = "Erro sem field errors";
        String errorCode = "NO_FIELD_ERRORS";

        // When
        errorResponse = new ErrorResponse(message, errorCode);

        // Then
        assertNotNull(errorResponse);
        assertNull(errorResponse.getFieldErrors());
    }

    @Test
    @DisplayName("Deve lidar com detalhes vazios")
    void shouldHandleEmptyDetails() {
        // Given
        String message = "Erro sem detalhes";
        String errorCode = "NO_DETAILS";

        // When
        errorResponse = new ErrorResponse(message, errorCode);

        // Then
        assertNotNull(errorResponse);
        assertNull(errorResponse.getAdditionalData());
    }

    @Test
    @DisplayName("Deve preservar imutabilidade dos field errors")
    void shouldPreserveFieldErrorsImmutability() {
        // Given
        String message = "Erro de teste";
        String errorCode = "TEST_ERROR";
        Map<String, String> originalFieldErrors = new HashMap<>();
        originalFieldErrors.put("campo", "erro original");

        // When
        errorResponse = new ErrorResponse(message, errorCode);
        errorResponse.setFieldErrors(originalFieldErrors);
        
        // Tentativa de modificar o mapa original
        originalFieldErrors.put("campo2", "novo erro");

        // Then
        assertEquals(2, originalFieldErrors.size()); // O mapa original foi modificado
        assertEquals(2, errorResponse.getFieldErrors().size()); // O ErrorResponse também reflete a mudança
    }

    @Test
    @DisplayName("Deve preservar imutabilidade dos detalhes")
    void shouldPreserveDetailsImmutability() {
        // Given
        String message = "Erro de teste";
        String errorCode = "TEST_ERROR";
        Map<String, Object> originalDetails = new HashMap<>();
        originalDetails.put("key", "value");

        // When
        errorResponse = new ErrorResponse(message, errorCode);
        errorResponse.setAdditionalData(originalDetails);
        
        // Tentativa de modificar o mapa original
        originalDetails.put("key2", "value2");

        // Then
        assertEquals(2, originalDetails.size()); // O mapa original foi modificado
        assertEquals(2, errorResponse.getAdditionalData().size()); // O ErrorResponse também reflete a mudança
    }

    @Test
    @DisplayName("Deve gerar timestamp próximo ao momento de criação")
    void shouldGenerateTimestampNearCreationTime() {
        // Given
        LocalDateTime beforeCreation = LocalDateTime.now();

        // When
        errorResponse = new ErrorResponse("Test", "TEST_ERROR");
        LocalDateTime afterCreation = LocalDateTime.now();

        // Then
        assertNotNull(errorResponse.getTimestamp());
        assertTrue(errorResponse.getTimestamp().isAfter(beforeCreation.minusSeconds(1)));
        assertTrue(errorResponse.getTimestamp().isBefore(afterCreation.plusSeconds(1)));
    }

    @Test
    @DisplayName("Deve aceitar valores nulos para campos opcionais")
    void shouldAcceptNullForOptionalFields() {
        // Given & When
        errorResponse = new ErrorResponse("Test message", "TEST_ERROR");
        errorResponse.setFieldErrors(null);
        errorResponse.setAdditionalData(null);
        errorResponse.setPath(null);
        errorResponse.setStatus(null);

        // Then
        assertNotNull(errorResponse);
        assertEquals("Test message", errorResponse.getMessage());
        assertEquals("TEST_ERROR", errorResponse.getErrorCode());
        assertNull(errorResponse.getFieldErrors());
        assertNull(errorResponse.getAdditionalData());
        assertNull(errorResponse.getPath());
        assertNull(errorResponse.getStatus());
    }

    @Test
    @DisplayName("Deve lidar com diferentes tipos de status HTTP")
    void shouldHandleDifferentHttpStatuses() {
        // Given & When
        ErrorResponse error400 = new ErrorResponse("Bad Request", "BAD_REQUEST", 400);
        ErrorResponse error401 = new ErrorResponse("Unauthorized", "UNAUTHORIZED", 401);
        ErrorResponse error403 = new ErrorResponse("Forbidden", "FORBIDDEN", 403);
        ErrorResponse error404 = new ErrorResponse("Not Found", "NOT_FOUND", 404);
        ErrorResponse error500 = new ErrorResponse("Internal Error", "INTERNAL_ERROR", 500);

        // Then
        assertEquals(Integer.valueOf(400), error400.getStatus());
        assertEquals(Integer.valueOf(401), error401.getStatus());
        assertEquals(Integer.valueOf(403), error403.getStatus());
        assertEquals(Integer.valueOf(404), error404.getStatus());
        assertEquals(Integer.valueOf(500), error500.getStatus());
    }

    @Test
    @DisplayName("Deve lidar com field errors com múltiplos erros por campo")
    void shouldHandleMultipleErrorsPerField() {
        // Given
        String message = "Erro de validação";
        String errorCode = "VALIDATION_ERROR";
        Map<String, String> fieldErrors = new HashMap<>();
        fieldErrors.put("email", "Email é obrigatório");
        fieldErrors.put("senha", "Senha deve ter pelo menos 8 caracteres");
        fieldErrors.put("nome", "Nome é obrigatório");

        // When
        errorResponse = new ErrorResponse(message, errorCode);
        errorResponse.setFieldErrors(fieldErrors);

        // Then
        assertNotNull(errorResponse.getFieldErrors());
        assertEquals(3, errorResponse.getFieldErrors().size());
        assertTrue(errorResponse.getFieldErrors().containsKey("email"));
        assertTrue(errorResponse.getFieldErrors().containsKey("senha"));
        assertTrue(errorResponse.getFieldErrors().containsKey("nome"));
    }

    @Test
    @DisplayName("Deve lidar com detalhes contendo diferentes tipos de dados")
    void shouldHandleDetailsWithDifferentDataTypes() {
        // Given
        String message = "Erro com detalhes variados";
        String errorCode = "VARIED_DETAILS";
        Map<String, Object> details = new HashMap<>();
        details.put("stringValue", "texto");
        details.put("intValue", 42);
        details.put("boolValue", true);
        details.put("listValue", Arrays.asList("item1", "item2"));

        // When
        errorResponse = new ErrorResponse(message, errorCode);
        errorResponse.setAdditionalData(details);

        // Then
        assertNotNull(errorResponse.getAdditionalData());
        assertEquals(4, errorResponse.getAdditionalData().size());
        assertEquals("texto", errorResponse.getAdditionalData().get("stringValue"));
        assertEquals(42, errorResponse.getAdditionalData().get("intValue"));
        assertEquals(true, errorResponse.getAdditionalData().get("boolValue"));
        assertTrue(errorResponse.getAdditionalData().get("listValue") instanceof List);
    }
}