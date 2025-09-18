package com.sistema.service.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para BaseService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BaseService Tests")
class BaseServiceTest {

    private TestableBaseService baseService;

    @BeforeEach
    void setUp() {
        baseService = new TestableBaseService();
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should execute operation successfully with return value")
        void shouldExecuteOperationSuccessfully() {
            // Given
            String expectedResult = "success";

            // When
            String result = baseService.executeWithErrorHandling(
                () -> expectedResult,
                "Test operation"
            );

            // Then
            assertEquals(expectedResult, result);
        }

        @Test
        @DisplayName("Should handle exception in operation with return value")
        void shouldHandleExceptionInOperation() {
            // Given
            String errorMessage = "Test operation";
            RuntimeException originalException = new RuntimeException("Original error");

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                baseService.executeWithErrorHandling(
                    () -> {
                        throw originalException;
                    },
                    errorMessage
                )
            );

            assertTrue(exception.getMessage().contains(errorMessage));
            assertTrue(exception.getMessage().contains("Original error"));
            assertEquals(originalException, exception.getCause());
        }

        @Test
        @DisplayName("Should execute void operation successfully")
        void shouldExecuteVoidOperationSuccessfully() {
            // Given
            boolean[] executed = {false};

            // When
            assertDoesNotThrow(() ->
                baseService.executeWithErrorHandling(
                    () -> executed[0] = true,
                    "Test void operation"
                )
            );

            // Then
            assertTrue(executed[0]);
        }

        @Test
        @DisplayName("Should handle exception in void operation")
        void shouldHandleExceptionInVoidOperation() {
            // Given
            String errorMessage = "Test void operation";
            RuntimeException originalException = new RuntimeException("Void error");

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                baseService.executeWithErrorHandling(
                    () -> {
                        throw originalException;
                    },
                    errorMessage
                )
            );

            assertTrue(exception.getMessage().contains(errorMessage));
            assertTrue(exception.getMessage().contains("Void error"));
            assertEquals(originalException, exception.getCause());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate not empty string successfully")
        void shouldValidateNotEmptySuccessfully() {
            // Given
            String validValue = "valid string";

            // When & Then
            assertDoesNotThrow(() -> baseService.validateNotEmpty(validValue, "testParam"));
        }

        @Test
        @DisplayName("Should throw exception for null string")
        void shouldThrowExceptionForNullString() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                baseService.validateNotEmpty(null, "testParam")
            );

            assertTrue(exception.getMessage().contains("testParam"));
            assertTrue(exception.getMessage().contains("não pode ser nulo ou vazio"));
        }

        @Test
        @DisplayName("Should throw exception for empty string")
        void shouldThrowExceptionForEmptyString() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                baseService.validateNotEmpty("", "testParam")
            );

            assertTrue(exception.getMessage().contains("testParam"));
            assertTrue(exception.getMessage().contains("não pode ser nulo ou vazio"));
        }

        @Test
        @DisplayName("Should throw exception for blank string")
        void shouldThrowExceptionForBlankString() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                baseService.validateNotEmpty("   ", "testParam")
            );

            assertTrue(exception.getMessage().contains("testParam"));
            assertTrue(exception.getMessage().contains("não pode ser nulo ou vazio"));
        }

        @Test
        @DisplayName("Should validate not null object successfully")
        void shouldValidateNotNullSuccessfully() {
            // Given
            Object validObject = new Object();

            // When & Then
            assertDoesNotThrow(() -> baseService.validateNotNull(validObject, "testParam"));
        }

        @Test
        @DisplayName("Should throw exception for null object")
        void shouldThrowExceptionForNullObject() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                baseService.validateNotNull(null, "testParam")
            );

            assertTrue(exception.getMessage().contains("testParam"));
            assertTrue(exception.getMessage().contains("não pode ser nulo"));
        }

        @Test
        @DisplayName("Should validate valid ID successfully")
        void shouldValidateValidIdSuccessfully() {
            // Given
            Long validId = 1L;

            // When & Then
            assertDoesNotThrow(() -> baseService.validateId(validId, "testId"));
        }

        @Test
        @DisplayName("Should throw exception for null ID")
        void shouldThrowExceptionForNullId() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                baseService.validateId(null, "testId")
            );

            assertTrue(exception.getMessage().contains("testId"));
            assertTrue(exception.getMessage().contains("não pode ser nulo"));
        }

        @Test
        @DisplayName("Should throw exception for zero ID")
        void shouldThrowExceptionForZeroId() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                baseService.validateId(0L, "testId")
            );

            assertTrue(exception.getMessage().contains("testId"));
            assertTrue(exception.getMessage().contains("deve ser maior que zero"));
        }

        @Test
        @DisplayName("Should throw exception for negative ID")
        void shouldThrowExceptionForNegativeId() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                baseService.validateId(-1L, "testId")
            );

            assertTrue(exception.getMessage().contains("testId"));
            assertTrue(exception.getMessage().contains("deve ser maior que zero"));
        }
    }

    @Nested
    @DisplayName("Logging Tests")
    class LoggingTests {

        @Test
        @DisplayName("Should log search operation with result found")
        void shouldLogSearchOperationWithResultFound() {
            // Given
            String operation = "User search";
            String parameter = "test@example.com";
            Optional<String> result = Optional.of("found");

            // When & Then
            assertDoesNotThrow(() -> 
                baseService.logSearchOperation(operation, parameter, result)
            );
        }

        @Test
        @DisplayName("Should log search operation with no result")
        void shouldLogSearchOperationWithNoResult() {
            // Given
            String operation = "User search";
            String parameter = "notfound@example.com";
            Optional<String> result = Optional.empty();

            // When & Then
            assertDoesNotThrow(() -> 
                baseService.logSearchOperation(operation, parameter, result)
            );
        }

        @Test
        @DisplayName("Should log create operation")
        void shouldLogCreateOperation() {
            // Given
            String entityType = "User";
            Long entityId = 1L;

            // When & Then
            assertDoesNotThrow(() -> 
                baseService.logCreateOperation(entityType, entityId)
            );
        }

        @Test
        @DisplayName("Should log update operation")
        void shouldLogUpdateOperation() {
            // Given
            String entityType = "User";
            Long entityId = 1L;

            // When & Then
            assertDoesNotThrow(() -> 
                baseService.logUpdateOperation(entityType, entityId)
            );
        }

        @Test
        @DisplayName("Should log delete operation")
        void shouldLogDeleteOperation() {
            // Given
            String entityType = "User";
            Long entityId = 1L;

            // When & Then
            assertDoesNotThrow(() -> 
                baseService.logDeleteOperation(entityType, entityId)
            );
        }

        @Test
        @DisplayName("Should log error message")
        void shouldLogErrorMessage() {
            // Given
            String message = "Test error message";

            // When & Then
            assertDoesNotThrow(() -> baseService.logError(message));
        }

        @Test
        @DisplayName("Should log error message with exception")
        void shouldLogErrorMessageWithException() {
            // Given
            String message = "Test error message";
            Exception exception = new RuntimeException("Test exception");

            // When & Then
            assertDoesNotThrow(() -> baseService.logError(message, exception));
        }

        @Test
        @DisplayName("Should log info message")
        void shouldLogInfoMessage() {
            // Given
            String message = "Test info message";

            // When & Then
            assertDoesNotThrow(() -> baseService.logInfo(message));
        }

        @Test
        @DisplayName("Should log warn message")
        void shouldLogWarnMessage() {
            // Given
            String message = "Test warn message";

            // When & Then
            assertDoesNotThrow(() -> baseService.logWarn(message));
        }

        @Test
        @DisplayName("Should log debug message")
        void shouldLogDebugMessage() {
            // Given
            String message = "Test debug message";

            // When & Then
            assertDoesNotThrow(() -> baseService.logDebug(message));
        }
    }

    @Nested
    @DisplayName("Utility Tests")
    class UtilityTests {

        @Test
        @DisplayName("Should get current timestamp")
        void shouldGetCurrentTimestamp() {
            // When
            LocalDateTime timestamp = baseService.getCurrentTimestamp();

            // Then
            assertNotNull(timestamp);
            assertTrue(timestamp.isBefore(LocalDateTime.now().plusSeconds(1)));
            assertTrue(timestamp.isAfter(LocalDateTime.now().minusSeconds(1)));
        }

        @Test
        @DisplayName("Should format error message")
        void shouldFormatErrorMessage() {
            // Given
            String operation = "save user";
            String details = "validation failed";

            // When
            String result = baseService.formatErrorMessage(operation, details);

            // Then
            assertEquals("Erro ao save user: validation failed", result);
        }

        @Test
        @DisplayName("Should format error message with null details")
        void shouldFormatErrorMessageWithNullDetails() {
            // Given
            String operation = "save user";
            String details = null;

            // When
            String result = baseService.formatErrorMessage(operation, details);

            // Then
            assertEquals("Erro ao save user: null", result);
        }
    }

    /**
     * Implementação testável de BaseService para permitir testes.
     */
    private static class TestableBaseService extends BaseService {
        // Expõe métodos protected para teste
        
        @Override
        public <T> T executeWithErrorHandling(java.util.function.Supplier<T> operation, String errorMessage) {
            return super.executeWithErrorHandling(operation, errorMessage);
        }
        
        @Override
        public void executeWithErrorHandling(Runnable operation, String errorMessage) {
            super.executeWithErrorHandling(operation, errorMessage);
        }
        
        @Override
        public void validateNotEmpty(String value, String paramName) {
            super.validateNotEmpty(value, paramName);
        }
        
        @Override
        public void validateNotNull(Object object, String paramName) {
            super.validateNotNull(object, paramName);
        }
        
        @Override
        public void validateId(Long id, String paramName) {
            super.validateId(id, paramName);
        }
        
        @Override
        public void logSearchOperation(String operation, Object parameter, Optional<?> result) {
            super.logSearchOperation(operation, parameter, result);
        }
        
        @Override
        public void logCreateOperation(String entityType, Object entityId) {
            super.logCreateOperation(entityType, entityId);
        }
        
        @Override
        public void logUpdateOperation(String entityType, Object entityId) {
            super.logUpdateOperation(entityType, entityId);
        }
        
        @Override
        public void logDeleteOperation(String entityType, Object entityId) {
            super.logDeleteOperation(entityType, entityId);
        }
        
        @Override
        public LocalDateTime getCurrentTimestamp() {
            return super.getCurrentTimestamp();
        }
        
        @Override
        public String formatErrorMessage(String operation, String details) {
            return super.formatErrorMessage(operation, details);
        }
        
        @Override
        public void logError(String message) {
            super.logError(message);
        }
        
        @Override
        public void logError(String message, Exception exception) {
            super.logError(message, exception);
        }
        
        @Override
        public void logInfo(String message) {
            super.logInfo(message);
        }
        
        @Override
        public void logWarn(String message) {
            super.logWarn(message);
        }
        
        @Override
        public void logDebug(String message) {
            super.logDebug(message);
        }
    }
}