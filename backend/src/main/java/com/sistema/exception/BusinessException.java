package com.sistema.exception;

/**
 * Exceção base para erros de regras de negócio.
 * Representa erros que são esperados e devem ser tratados de forma específica.
 */
public class BusinessException extends RuntimeException {
    
    private final String errorCode;
    private final Object[] parameters;
    
    public BusinessException(String message) {
        super(message);
        this.errorCode = null;
        this.parameters = null;
    }
    
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.parameters = null;
    }
    
    public BusinessException(String errorCode, String message, Object... parameters) {
        super(message);
        this.errorCode = errorCode;
        this.parameters = parameters;
    }
    
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.parameters = null;
    }
    
    public BusinessException(String errorCode, String message, Throwable cause, Object... parameters) {
        super(message, cause);
        this.errorCode = errorCode;
        this.parameters = parameters;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public Object[] getParameters() {
        return parameters;
    }
}