package com.sistema.exception;

import com.sistema.util.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimitException Tests")
class RateLimitExceptionTest {

    @Test
    @DisplayName("Deve criar RateLimitException com mensagem e tempo restante")
    void testCreateRateLimitExceptionWithMessageAndRemainingTime() {
        // Given
        String message = "Limite de requisições excedido";
        long remainingSeconds = 300L;

        // When
        RateLimitException exception = new RateLimitException(message, remainingSeconds);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals("RATE_LIMITED", exception.getErrorCode());
        assertEquals(remainingSeconds, exception.getRemainingSeconds());
        assertEquals(0, exception.getMaxAttempts());
        assertNull(exception.getLimitType());
    }

    @Test
    @DisplayName("Deve criar RateLimitException com mensagem, tempo restante, tentativas máximas e tipo")
    void testCreateRateLimitExceptionWithParameters() {
        // Given
        String message = "Limite de 100 requisições por minuto excedido. Tente novamente em 45 segundos.";
        long remainingSeconds = 45L;
        int maxAttempts = 100;
        String limitType = "LOGIN";

        // When
        RateLimitException exception = new RateLimitException(message, remainingSeconds, maxAttempts, limitType);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals("RATE_LIMITED", exception.getErrorCode());
        assertEquals(remainingSeconds, exception.getRemainingSeconds());
        assertEquals(maxAttempts, exception.getMaxAttempts());
        assertEquals(limitType, exception.getLimitType());
    }

    @Test
    @DisplayName("Deve criar RateLimitException com código de erro personalizado")
    void testCreateRateLimitExceptionWithCustomErrorCode() {
        // Given
        String message = "Rate limit atingido com erro interno";
        String errorCode = ErrorCodes.RATE_LIMIT_EXCEEDED;
        long remainingSeconds = 120L;
        int maxAttempts = 5;
        String limitType = "API";

        // When
        RateLimitException exception = new RateLimitException(errorCode, message, remainingSeconds, maxAttempts, limitType);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(remainingSeconds, exception.getRemainingSeconds());
        assertEquals(maxAttempts, exception.getMaxAttempts());
        assertEquals(limitType, exception.getLimitType());
    }

    @Test
    @DisplayName("Deve criar RateLimitException com mensagem e tempo restante padrão")
    void testCreateRateLimitExceptionWithMessageOnly() {
        // Given
        String message = "Muitas requisições";
        long remainingSeconds = 60L;

        // When
        RateLimitException exception = new RateLimitException(message, remainingSeconds);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals("RATE_LIMITED", exception.getErrorCode());
        assertEquals(remainingSeconds, exception.getRemainingSeconds());
        assertEquals(0, exception.getMaxAttempts());
        assertNull(exception.getLimitType());
    }

    @Test
    @DisplayName("Deve criar RateLimitException com informações básicas")
    void testCreateRateLimitExceptionWithBasicInfo() {
        // Given
        String message = "Erro no controle de rate limit";
        long remainingSeconds = 180L;
        int maxAttempts = 10;
        String limitType = "REQUEST";

        // When
        RateLimitException exception = new RateLimitException(message, remainingSeconds, maxAttempts, limitType);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals("RATE_LIMITED", exception.getErrorCode());
        assertEquals(remainingSeconds, exception.getRemainingSeconds());
        assertEquals(maxAttempts, exception.getMaxAttempts());
        assertEquals(limitType, exception.getLimitType());
    }

    @Test
    @DisplayName("Deve ser uma BusinessException")
    void testRateLimitExceptionIsBusinessException() {
        // Given
        RateLimitException exception = new RateLimitException("Teste", 60L);

        // Then
        assertInstanceOf(BusinessException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    @DisplayName("Deve lidar com informações de rate limit por IP")
    void testRateLimitExceptionWithIpInformation() {
        // Given
        String message = "IP 192.168.1.100 excedeu o limite de 1000 requisições por hora";
        String errorCode = ErrorCodes.RATE_LIMIT_EXCEEDED;
        long remainingSeconds = 3600L; // 1 hora
        int maxAttempts = 1000;
        String limitType = "IP";

        // When
        RateLimitException exception = new RateLimitException(errorCode, message, remainingSeconds, maxAttempts, limitType);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(remainingSeconds, exception.getRemainingSeconds());
        assertEquals(maxAttempts, exception.getMaxAttempts());
        assertEquals(limitType, exception.getLimitType());
    }

    @Test
    @DisplayName("Deve suportar informações de tempo de reset")
    void testRateLimitExceptionWithResetTime() {
        // Given
        String message = "Rate limit excedido. Reset em 120 segundos às 2024-01-15T14:30:00Z";
        String errorCode = ErrorCodes.RATE_LIMIT_EXCEEDED;
        long remainingSeconds = 120L;
        int maxAttempts = 100;
        String limitType = "TIME_WINDOW";

        // When
        RateLimitException exception = new RateLimitException(errorCode, message, remainingSeconds, maxAttempts, limitType);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(remainingSeconds, exception.getRemainingSeconds());
        assertEquals(maxAttempts, exception.getMaxAttempts());
        assertEquals(limitType, exception.getLimitType());
    }

    @Test
    @DisplayName("Deve lidar com diferentes tipos de rate limit")
    void testRateLimitExceptionWithDifferentLimitTypes() {
        // Test rate limit por usuário
        RateLimitException userException = new RateLimitException(
            ErrorCodes.RATE_LIMIT_EXCEEDED,
            "Usuário user123 excedeu limite de 50 requisições", 
            300L, 50, "USER");
        assertEquals("USER", userException.getLimitType());
        assertEquals(50, userException.getMaxAttempts());

        // Test rate limit por endpoint
        RateLimitException endpointException = new RateLimitException(
            ErrorCodes.RATE_LIMIT_EXCEEDED,
            "Endpoint /api/auth/login recebeu 1000 requisições, limite é 500", 
            600L, 500, "ENDPOINT");
        assertEquals("ENDPOINT", endpointException.getLimitType());
        assertEquals(500, endpointException.getMaxAttempts());

        // Test rate limit global
        RateLimitException globalException = new RateLimitException(
            ErrorCodes.RATE_LIMIT_EXCEEDED,
            "Sistema atingiu limite global de 10000 requisições por minuto", 
            60L, 10000, "GLOBAL");
        assertEquals("GLOBAL", globalException.getLimitType());
        assertEquals(10000, globalException.getMaxAttempts());
    }

    @Test
    @DisplayName("Deve preservar informações de janela de tempo")
    void testRateLimitExceptionWithTimeWindow() {
        // Given
        String message = "Limite de 100 requisições em janela de 5 minutos excedido. Requisições atuais: 150";
        String errorCode = ErrorCodes.RATE_LIMIT_EXCEEDED;
        long remainingSeconds = 300L; // 5 minutos
        int maxAttempts = 100;
        String limitType = "TIME_WINDOW";

        // When
        RateLimitException exception = new RateLimitException(errorCode, message, remainingSeconds, maxAttempts, limitType);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(remainingSeconds, exception.getRemainingSeconds());
        assertEquals(maxAttempts, exception.getMaxAttempts());
        assertEquals(limitType, exception.getLimitType());
    }

    @Test
    @DisplayName("Deve suportar informações de headers HTTP")
    void testRateLimitExceptionWithHttpHeaders() {
        // Given
        String message = "Rate limit excedido. Verifique headers: X-RateLimit-Limit=1000, X-RateLimit-Remaining=0";
        String errorCode = ErrorCodes.RATE_LIMIT_EXCEEDED;
        long remainingSeconds = 3600L; // 1 hora
        int maxAttempts = 1000;
        String limitType = "HTTP_HEADER";

        // When
        RateLimitException exception = new RateLimitException(errorCode, message, remainingSeconds, maxAttempts, limitType);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(remainingSeconds, exception.getRemainingSeconds());
        assertEquals(maxAttempts, exception.getMaxAttempts());
        assertEquals(limitType, exception.getLimitType());
    }

    @Test
    @DisplayName("Deve lidar com rate limit por API key")
    void testRateLimitExceptionWithApiKey() {
        // Given
        String message = "API Key ak_123456789 excedeu quota de 10000 requisições. Plano: Premium";
        String errorCode = ErrorCodes.RATE_LIMIT_EXCEEDED;
        long remainingSeconds = 86400L; // 24 horas
        int maxAttempts = 10000;
        String limitType = "API_KEY";

        // When
        RateLimitException exception = new RateLimitException(errorCode, message, remainingSeconds, maxAttempts, limitType);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(remainingSeconds, exception.getRemainingSeconds());
        assertEquals(maxAttempts, exception.getMaxAttempts());
        assertEquals(limitType, exception.getLimitType());
    }

    @Test
    @DisplayName("Deve preservar stack trace para debugging")
    void testRateLimitExceptionStackTrace() {
        // Given/When
        RateLimitException exception = new RateLimitException("Rate limit excedido", 60L);

        // Then
        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
        assertEquals("testRateLimitExceptionStackTrace", 
                    exception.getStackTrace()[0].getMethodName());
    }

    @Test
    @DisplayName("Deve suportar rate limit com burst allowance")
    void testRateLimitExceptionWithBurstAllowance() {
        // Given
        String message = "Rate limit excedido. Limite base: 10/s, Burst: 50, Usado: 75";
        String errorCode = ErrorCodes.RATE_LIMIT_EXCEEDED;
        long remainingSeconds = 60L;
        int maxAttempts = 50; // Burst allowance
        String limitType = "BURST";

        // When
        RateLimitException exception = new RateLimitException(errorCode, message, remainingSeconds, maxAttempts, limitType);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(remainingSeconds, exception.getRemainingSeconds());
        assertEquals(maxAttempts, exception.getMaxAttempts());
        assertEquals(limitType, exception.getLimitType());
    }

    @Test
    @DisplayName("Deve lidar com rate limit distribuído")
    void testRateLimitExceptionWithDistributedLimiting() {
        // Given
        String message = "Rate limit distribuído excedido. Nó: node-01, Limite global: 5000, Uso atual: 5250";
        String errorCode = ErrorCodes.RATE_LIMIT_EXCEEDED;
        long remainingSeconds = 120L;
        int maxAttempts = 5000; // Limite global
        String limitType = "DISTRIBUTED";

        // When
        RateLimitException exception = new RateLimitException(errorCode, message, remainingSeconds, maxAttempts, limitType);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(remainingSeconds, exception.getRemainingSeconds());
        assertEquals(maxAttempts, exception.getMaxAttempts());
        assertEquals(limitType, exception.getLimitType());
    }

    @Test
    @DisplayName("Deve suportar informações de retry-after")
    void testRateLimitExceptionWithRetryAfter() {
        // Given
        String message = "Rate limit excedido. Retry-After: 300 segundos";
        String errorCode = ErrorCodes.RATE_LIMIT_EXCEEDED;
        long remainingSeconds = 300L; // 5 minutos
        int maxAttempts = 100;
        String limitType = "RETRY_AFTER";

        // When
        RateLimitException exception = new RateLimitException(errorCode, message, remainingSeconds, maxAttempts, limitType);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(remainingSeconds, exception.getRemainingSeconds());
        assertEquals(maxAttempts, exception.getMaxAttempts());
        assertEquals(limitType, exception.getLimitType());
    }
}