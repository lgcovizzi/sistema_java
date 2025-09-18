package com.sistema.exception;

import java.util.List;
import java.util.Map;

/**
 * Exceção para erros de validação de entrada.
 * Contém informações detalhadas sobre quais campos falharam na validação.
 */
public class ValidationException extends BusinessException {
    
    private final Map<String, List<String>> fieldErrors;
    
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
        this.fieldErrors = null;
    }
    
    public ValidationException(String message, String errorCode) {
        super(errorCode, message);
        this.fieldErrors = null;
    }
    
    public ValidationException(String message, Map<String, List<String>> fieldErrors) {
        super("VALIDATION_ERROR", message);
        this.fieldErrors = fieldErrors;
    }
    
    public ValidationException(String message, String errorCode, Map<String, List<String>> fieldErrors) {
        super(errorCode, message);
        this.fieldErrors = fieldErrors;
    }
    
    public ValidationException(String message, String errorCode, Map<String, List<String>> fieldErrors, Object... parameters) {
        super(errorCode, message, parameters);
        this.fieldErrors = fieldErrors;
    }
    
    public ValidationException(String message, String errorCode, Map<String, List<String>> fieldErrors, Object[] parameters, Throwable cause) {
        super(errorCode, message, cause, parameters);
        this.fieldErrors = fieldErrors;
    }
    
    public Map<String, List<String>> getFieldErrors() {
        return fieldErrors;
    }
    
    public boolean hasFieldErrors() {
        return fieldErrors != null && !fieldErrors.isEmpty();
    }
}