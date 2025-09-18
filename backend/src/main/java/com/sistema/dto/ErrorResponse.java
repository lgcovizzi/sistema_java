package com.sistema.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Classe para resposta padronizada de erros.
 * Fornece estrutura consistente para todas as respostas de erro da API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private boolean error = true;
    private String message;
    private String errorCode;
    private LocalDateTime timestamp;
    private String path;
    private Integer status;
    private Map<String, String> fieldErrors;
    private Map<String, Object> additionalData;
    
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ErrorResponse(String message, String errorCode) {
        this();
        this.message = message;
        this.errorCode = errorCode;
    }
    
    public ErrorResponse(String message, String errorCode, Integer status) {
        this(message, errorCode);
        this.status = status;
    }
    
    public ErrorResponse(String message, String errorCode, Integer status, String path) {
        this(message, errorCode, status);
        this.path = path;
    }
    
    // Getters e Setters
    public boolean isError() {
        return error;
    }
    
    public void setError(boolean error) {
        this.error = error;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
    
    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
    
    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }
    
    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
    }
    
    // Métodos de conveniência
    public ErrorResponse withFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
        return this;
    }
    
    public ErrorResponse withAdditionalData(String key, Object value) {
        if (this.additionalData == null) {
            this.additionalData = new java.util.HashMap<>();
        }
        this.additionalData.put(key, value);
        return this;
    }
    
    public ErrorResponse withAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
        return this;
    }
}