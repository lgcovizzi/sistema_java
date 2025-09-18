package com.sistema.exception;

/**
 * Exceção para erros de autenticação.
 * Representa falhas no processo de autenticação do usuário.
 */
public class AuthenticationException extends BusinessException {
    
    private final boolean requiresCaptcha;
    private final long remainingSeconds;
    
    public AuthenticationException(String message) {
        super("AUTHENTICATION_ERROR", message);
        this.requiresCaptcha = false;
        this.remainingSeconds = 0;
    }
    
    public AuthenticationException(String errorCode, String message) {
        super(errorCode, message);
        this.requiresCaptcha = false;
        this.remainingSeconds = 0;
    }
    
    public AuthenticationException(String errorCode, String message, boolean requiresCaptcha) {
        super(errorCode, message);
        this.requiresCaptcha = requiresCaptcha;
        this.remainingSeconds = 0;
    }
    
    public AuthenticationException(String errorCode, String message, long remainingSeconds) {
        super(errorCode, message);
        this.requiresCaptcha = false;
        this.remainingSeconds = remainingSeconds;
    }
    
    public AuthenticationException(String errorCode, String message, boolean requiresCaptcha, long remainingSeconds) {
        super(errorCode, message);
        this.requiresCaptcha = requiresCaptcha;
        this.remainingSeconds = remainingSeconds;
    }
    
    public boolean isRequiresCaptcha() {
        return requiresCaptcha;
    }
    
    public long getRemainingSeconds() {
        return remainingSeconds;
    }
}