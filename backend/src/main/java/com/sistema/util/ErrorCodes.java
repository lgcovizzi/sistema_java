package com.sistema.util;

/**
 * Constantes para códigos de erro padronizados.
 * Centraliza todos os códigos de erro utilizados na aplicação.
 */
public final class ErrorCodes {
    
    // Códigos de erro gerais
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    
    // Códigos de erro de autenticação
    public static final String AUTHENTICATION_ERROR = "AUTHENTICATION_ERROR";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String INVALID_TOKEN = "INVALID_TOKEN";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN";
    public static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
    public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    
    // Códigos de erro de usuário
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS";
    public static final String EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS";
    public static final String CPF_ALREADY_EXISTS = "CPF_ALREADY_EXISTS";
    public static final String INVALID_PASSWORD = "INVALID_PASSWORD";
    public static final String PASSWORD_TOO_WEAK = "PASSWORD_TOO_WEAK";
    public static final String CURRENT_PASSWORD_INCORRECT = "CURRENT_PASSWORD_INCORRECT";
    
    // Códigos de erro de captcha
    public static final String INVALID_CAPTCHA = "INVALID_CAPTCHA";
    public static final String CAPTCHA_EXPIRED = "CAPTCHA_EXPIRED";
    public static final String CAPTCHA_REQUIRED = "CAPTCHA_REQUIRED";
    
    // Códigos de erro de email
    public static final String EMAIL_SEND_ERROR = "EMAIL_SEND_ERROR";
    public static final String EMAIL_VERIFICATION_ERROR = "EMAIL_VERIFICATION_ERROR";
    public static final String VERIFICATION_TOKEN_INVALID = "VERIFICATION_TOKEN_INVALID";
    public static final String VERIFICATION_TOKEN_EXPIRED = "VERIFICATION_TOKEN_EXPIRED";
    public static final String USER_NOT_FOUND_OR_VERIFIED = "USER_NOT_FOUND_OR_VERIFIED";
    
    // Códigos de erro de perfil
    public static final String PROFILE_UPDATE_ERROR = "PROFILE_UPDATE_ERROR";
    public static final String INVALID_PROFILE_DATA = "INVALID_PROFILE_DATA";
    
    // Códigos de erro de recuperação de senha
    public static final String PASSWORD_RESET_ERROR = "PASSWORD_RESET_ERROR";
    public static final String PASSWORD_RESET_TOKEN_INVALID = "PASSWORD_RESET_TOKEN_INVALID";
    public static final String PASSWORD_RESET_TOKEN_EXPIRED = "PASSWORD_RESET_TOKEN_EXPIRED";
    public static final String CPF_NOT_FOUND = "CPF_NOT_FOUND";
    
    // Códigos de erro de registro
    public static final String REGISTRATION_ERROR = "REGISTRATION_ERROR";
    public static final String INVALID_REGISTRATION_DATA = "INVALID_REGISTRATION_DATA";
    
    // Códigos de erro de validação específicos
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String INVALID_EMAIL_FORMAT = "INVALID_EMAIL_FORMAT";
    public static final String INVALID_CPF_FORMAT = "INVALID_CPF_FORMAT";
    public static final String INVALID_PHONE_FORMAT = "INVALID_PHONE_FORMAT";
    public static final String INVALID_DATE_FORMAT = "INVALID_DATE_FORMAT";
    public static final String FIELD_REQUIRED = "FIELD_REQUIRED";
    public static final String FIELD_TOO_LONG = "FIELD_TOO_LONG";
    public static final String FIELD_TOO_SHORT = "FIELD_TOO_SHORT";
    
    // Códigos de erro de sistema
    public static final String DATABASE_ERROR = "DATABASE_ERROR";
    public static final String REDIS_ERROR = "REDIS_ERROR";
    public static final String SMTP_ERROR = "SMTP_ERROR";
    public static final String CONFIGURATION_ERROR = "CONFIGURATION_ERROR";
    
    // Códigos de erro de rate limiting específicos
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String LOGIN_RATE_LIMITED = "LOGIN_RATE_LIMITED";
    public static final String PASSWORD_RESET_RATE_LIMITED = "PASSWORD_RESET_RATE_LIMITED";
    public static final String EMAIL_VERIFICATION_RATE_LIMITED = "EMAIL_VERIFICATION_RATE_LIMITED";
    public static final String REGISTRATION_RATE_LIMITED = "REGISTRATION_RATE_LIMITED";
    
    // Construtor privado para evitar instanciação
    private ErrorCodes() {
        throw new UnsupportedOperationException("Esta é uma classe utilitária e não deve ser instanciada");
    }
}