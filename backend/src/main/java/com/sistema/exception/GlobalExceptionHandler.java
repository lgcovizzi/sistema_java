package com.sistema.exception;

import com.sistema.dto.ErrorResponse;
import com.sistema.service.ToastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler global para tratamento centralizado de exceções.
 * Captura e trata todas as exceções da aplicação de forma padronizada.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @Autowired
    private ToastService toastService;
    
    /**
     * Trata exceções de negócio customizadas.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        logger.warn("Erro de negócio: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        // Adiciona mensagem toast baseada na exceção
        try {
            toastService.fromException(ex);
        } catch (Exception toastEx) {
            logger.warn("Erro ao adicionar toast: {}", toastEx.getMessage());
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getMessage(),
            ex.getErrorCode(),
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Trata exceções de validação.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex, HttpServletRequest request) {
        logger.warn("Erro de validação: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getMessage(),
            ex.getErrorCode(),
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI()
        );
        
        if (ex.hasFieldErrors()) {
            Map<String, String> convertedFieldErrors = new HashMap<>();
            ex.getFieldErrors().forEach((field, errors) -> {
                convertedFieldErrors.put(field, String.join(", ", errors));
            });
            errorResponse.setFieldErrors(convertedFieldErrors);
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Trata exceções de autenticação.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        logger.warn("Erro de autenticação: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getMessage(),
            ex.getErrorCode(),
            HttpStatus.UNAUTHORIZED.value(),
            request.getRequestURI()
        );
        
        if (ex.isRequiresCaptcha()) {
            errorResponse.withAdditionalData("requiresCaptcha", true);
        }
        
        if (ex.getRemainingSeconds() > 0) {
            errorResponse.withAdditionalData("remainingSeconds", ex.getRemainingSeconds());
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * Trata exceções de recurso não encontrado.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        logger.warn("Recurso não encontrado: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getMessage(),
            ex.getErrorCode(),
            HttpStatus.NOT_FOUND.value(),
            request.getRequestURI()
        );
        
        if (ex.getResourceType() != null) {
            errorResponse.withAdditionalData("resourceType", ex.getResourceType());
        }
        
        if (ex.getResourceId() != null) {
            errorResponse.withAdditionalData("resourceId", ex.getResourceId());
        }
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Trata exceções de rate limiting.
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitException(RateLimitException ex, HttpServletRequest request) {
        logger.warn("Rate limit excedido: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getMessage(),
            ex.getErrorCode(),
            HttpStatus.TOO_MANY_REQUESTS.value(),
            request.getRequestURI()
        );
        
        errorResponse.withAdditionalData("remainingSeconds", ex.getRemainingSeconds());
        
        if (ex.getMaxAttempts() > 0) {
            errorResponse.withAdditionalData("maxAttempts", ex.getMaxAttempts());
        }
        
        if (ex.getLimitType() != null) {
            errorResponse.withAdditionalData("limitType", ex.getLimitType());
        }
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }
    
    /**
     * Trata erros de validação do Spring (Bean Validation).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        logger.warn("Erro de validação de entrada: {}", ex.getMessage());
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        ErrorResponse errorResponse = new ErrorResponse(
            "Dados de entrada inválidos",
            "VALIDATION_ERROR",
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI()
        ).withFieldErrors(fieldErrors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Trata exceções de acesso negado.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        logger.warn("Acesso negado: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "Acesso negado. Você não tem permissão para acessar este recurso",
            "ACCESS_DENIED",
            HttpStatus.FORBIDDEN.value(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Trata exceções genéricas não capturadas.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        logger.error("Erro interno não tratado: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "Erro interno do servidor. Tente novamente mais tarde",
            "INTERNAL_ERROR",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Trata exceções de runtime não específicas.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        logger.error("Erro de runtime: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "Erro interno do servidor",
            "RUNTIME_ERROR",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}