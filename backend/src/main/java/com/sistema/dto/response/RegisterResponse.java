package com.sistema.dto.response;

/**
 * DTO para resposta de cadastro de usuário.
 */
public class RegisterResponse {
    
    private boolean success;
    private String message;
    private String userId;
    private String email;
    private boolean emailVerified;
    
    // Construtores
    public RegisterResponse() {}
    
    public RegisterResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public RegisterResponse(boolean success, String message, String userId, String email, boolean emailVerified) {
        this.success = success;
        this.message = message;
        this.userId = userId;
        this.email = email;
        this.emailVerified = emailVerified;
    }
    
    // Getters e Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public boolean isEmailVerified() {
        return emailVerified;
    }
    
    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}
