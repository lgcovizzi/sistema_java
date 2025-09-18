package com.sistema.exception;

import com.sistema.dto.ErrorResponse;
import com.sistema.service.ToastService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerToastTest {

    @Mock
    private ToastService toastService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void testHandleBusinessException_WithToastIntegration() {
        BusinessException exception = new BusinessException("BUSINESS_ERROR", "Erro de negócio específico");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBusinessException(exception, request);

        // Verifica a resposta HTTP
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Erro de negócio específico", response.getBody().getMessage());

        // Verifica se o toast foi chamado
        verify(toastService).fromException(exception);
    }

    @Test
    void testHandleBusinessException_ToastServiceThrowsException() {
        BusinessException exception = new BusinessException("BUSINESS_ERROR", "Erro de negócio");
        
        // Simula erro no ToastService
        doThrow(new RuntimeException("Erro no toast")).when(toastService).fromException(any());

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBusinessException(exception, request);

        // Verifica que a resposta ainda é retornada mesmo com erro no toast
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Erro de negócio", response.getBody().getMessage());

        // Verifica que o toast foi tentado
        verify(toastService).fromException(exception);
    }

    @Test
    void testHandleValidationException_WithToastIntegration() {
        // Mock do BindingResult
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("user", "email", "Email é obrigatório");
        FieldError fieldError2 = new FieldError("user", "password", "Senha deve ter pelo menos 8 caracteres");
        
        when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValid(exception, request);

        // Verifica a resposta HTTP
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Dados inválidos"));
    }

    @Test
    void testHandleAuthenticationException_WithToastIntegration() {
        AuthenticationException exception = new AuthenticationException("Credenciais inválidas");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAuthenticationException(exception, request);

        // Verifica a resposta HTTP
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Credenciais inválidas", response.getBody().getMessage());

        // Verifica se o toast foi chamado
        verify(toastService).fromException(exception);
    }

    @Test
    void testHandleResourceNotFoundException_WithToastIntegration() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Usuário não encontrado");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFoundException(exception, request);

        // Verifica a resposta HTTP
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Usuário não encontrado", response.getBody().getMessage());

        // Verifica se o toast foi chamado
        verify(toastService).fromException(exception);
    }

    @Test
    void testHandleGenericException_WithToastIntegration() {
        RuntimeException exception = new RuntimeException("Erro genérico");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(exception, request);

        // Verifica a resposta HTTP
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Erro interno do servidor", response.getBody().getMessage());
    }

    @Test
    void testMultipleExceptions_ToastCalledForEach() {
        BusinessException exception1 = new BusinessException("BUSINESS_ERROR", "Erro 1");
        BusinessException exception2 = new BusinessException("BUSINESS_ERROR", "Erro 2");

        globalExceptionHandler.handleBusinessException(exception1, request);
        globalExceptionHandler.handleBusinessException(exception2, request);
        AuthenticationException authException = new AuthenticationException("Auth erro");
        globalExceptionHandler.handleAuthenticationException(authException, request);

        // Verifica que o toast foi chamado para cada exceção
        verify(toastService).fromException(exception1);
        verify(toastService).fromException(exception2);
        verify(toastService).fromException(authException);
    }

    @Test
    void testToastServiceNull_NoException() {
        // Simula ToastService nulo (não deveria acontecer, mas testa robustez)
        globalExceptionHandler = new GlobalExceptionHandler();
        BusinessException exception = new BusinessException("BUSINESS_ERROR", "Teste");

        // Não deve lançar exceção mesmo com ToastService nulo
        assertDoesNotThrow(() -> globalExceptionHandler.handleBusinessException(exception, request));
    }

    @Test
    void testToastIntegration_WithDifferentExceptionTypes() {
        BusinessException businessEx = new BusinessException("BUSINESS_ERROR", "Erro de negócio");
        AuthenticationException authEx = new AuthenticationException("Erro de auth");

        globalExceptionHandler.handleBusinessException(businessEx, request);
        globalExceptionHandler.handleAuthenticationException(authEx, request);

        // Verifica que o toast foi chamado para diferentes tipos
        verify(toastService).fromException(businessEx);
        verify(toastService).fromException(authEx);
    }

    @Test
    void testToastServicePerformance_MultipleCallsQuickly() {
        // Testa múltiplas chamadas rápidas
        for (int i = 0; i < 10; i++) {
            BusinessException exception = new BusinessException("BUSINESS_ERROR", "Erro " + i);
            globalExceptionHandler.handleBusinessException(exception, request);
        }

        // Verifica que o toast foi chamado 10 vezes
        verify(toastService, times(10)).fromException(any(BusinessException.class));
    }

    @Test
    void testToastIntegration_WithLongMessages() {
        String longMessage = "Esta é uma mensagem muito longa que pode causar problemas no sistema de toast se não for tratada adequadamente pelo serviço";
        BusinessException exception = new BusinessException("BUSINESS_ERROR", longMessage);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBusinessException(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(longMessage, response.getBody().getMessage());
        
        verify(toastService).fromException(exception);
    }

    @Test
    void testToastIntegration_WithSpecialCharacters() {
        String specialMessage = "Erro com caracteres especiais: áéíóú çñü @#$%&*()";
        BusinessException exception = new BusinessException("BUSINESS_ERROR", specialMessage);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBusinessException(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(specialMessage, response.getBody().getMessage());
        
        verify(toastService).fromException(exception);
    }

    @Test
    void testToastIntegration_WithNullMessage() {
        BusinessException exception = new BusinessException("BUSINESS_ERROR", null);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBusinessException(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        verify(toastService).fromException(exception);
    }

    @Test
    void testToastIntegration_WithEmptyMessage() {
        BusinessException exception = new BusinessException("BUSINESS_ERROR", "");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBusinessException(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        verify(toastService).fromException(exception);
    }

    @Test
    void testErrorResponseStructure_AfterToastIntegration() {
        BusinessException exception = new BusinessException("BUSINESS_ERROR", "Erro de teste");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBusinessException(exception, request);

        // Verifica estrutura da resposta
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getTimestamp());
        assertNotNull(response.getBody().getPath());
        assertEquals("Erro de teste", response.getBody().getMessage());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
        
        // Verifica que o toast não afeta a estrutura da resposta
        verify(toastService).fromException(exception);
    }
}