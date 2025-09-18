package com.sistema.exception;

/**
 * Exceção para violações de rate limiting.
 * Representa situações onde o usuário excedeu o limite de tentativas.
 */
public class RateLimitException extends BusinessException {
    
    private final long remainingSeconds;
    private final int maxAttempts;
    private final String limitType;
    
    public RateLimitException(String message, long remainingSeconds) {
        super("RATE_LIMITED", message);
        this.remainingSeconds = remainingSeconds;
        this.maxAttempts = 0;
        this.limitType = null;
    }
    
    public RateLimitException(String message, long remainingSeconds, int maxAttempts, String limitType) {
        super("RATE_LIMITED", message);
        this.remainingSeconds = remainingSeconds;
        this.maxAttempts = maxAttempts;
        this.limitType = limitType;
    }
    
    public RateLimitException(String errorCode, String message, long remainingSeconds, int maxAttempts, String limitType) {
        super(errorCode, message);
        this.remainingSeconds = remainingSeconds;
        this.maxAttempts = maxAttempts;
        this.limitType = limitType;
    }
    
    public long getRemainingSeconds() {
        return remainingSeconds;
    }
    
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    public String getLimitType() {
        return limitType;
    }
}