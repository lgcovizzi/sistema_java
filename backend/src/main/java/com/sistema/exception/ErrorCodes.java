package com.sistema.exception;

/**
 * Códigos de erro padronizados da aplicação
 */
public final class ErrorCodes {
    
    // Códigos de autenticação
    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_001";
    public static final String AUTH_USER_NOT_FOUND = "AUTH_002";
    public static final String AUTH_USER_DISABLED = "AUTH_003";
    public static final String AUTH_EMAIL_NOT_VERIFIED = "AUTH_004";
    public static final String AUTH_TOKEN_EXPIRED = "AUTH_005";
    public static final String AUTH_TOKEN_INVALID = "AUTH_006";
    public static final String AUTH_INSUFFICIENT_PERMISSIONS = "AUTH_007";
    
    // Códigos de validação
    public static final String VALIDATION_FAILED = "VAL_000";
    public static final String VALIDATION_REQUIRED_FIELD = "VAL_001";
    public static final String VALIDATION_INVALID_FORMAT = "VAL_002";
    public static final String VALIDATION_INVALID_LENGTH = "VAL_003";
    public static final String VALIDATION_INVALID_EMAIL = "VAL_004";
    public static final String VALIDATION_INVALID_CPF = "VAL_005";
    public static final String VALIDATION_WEAK_PASSWORD = "VAL_006";
    
    // Códigos de usuário
    public static final String USER_EMAIL_ALREADY_EXISTS = "USER_001";
    public static final String USER_CPF_ALREADY_EXISTS = "USER_002";
    public static final String USER_NOT_FOUND = "USER_003";
    public static final String USER_CREATION_FAILED = "USER_004";
    
    // Códigos de recursos
    public static final String RESOURCE_NOT_FOUND = "RES_001";
    public static final String PAGE_NOT_FOUND = "RES_002";
    
    // Códigos de captcha
    public static final String CAPTCHA_REQUIRED = "CAPTCHA_001";
    public static final String CAPTCHA_INVALID = "CAPTCHA_002";
    public static final String CAPTCHA_EXPIRED = "CAPTCHA_003";
    public static final String CAPTCHA_NOT_FOUND = "CAPTCHA_004";
    
    // Códigos de sistema
    public static final String SYSTEM_INTERNAL_ERROR = "SYS_001";
    public static final String SYSTEM_SERVICE_UNAVAILABLE = "SYS_002";
    public static final String SYSTEM_DATABASE_ERROR = "SYS_003";
    public static final String SYSTEM_REDIS_ERROR = "SYS_004";
    
    // Códigos de email
    public static final String EMAIL_SEND_FAILED = "EMAIL_001";
    public static final String EMAIL_VERIFICATION_FAILED = "EMAIL_002";
    public static final String EMAIL_TOKEN_EXPIRED = "EMAIL_003";
    public static final String EMAIL_TOKEN_INVALID = "EMAIL_004";
    
    private ErrorCodes() {
        // Classe utilitária - não deve ser instanciada
    }
}