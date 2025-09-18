package com.sistema.exception;

/**
 * Exceção para recursos não encontrados.
 * Representa situações onde um recurso solicitado não existe.
 */
public class ResourceNotFoundException extends BusinessException {
    
    private final String resourceType;
    private final String resourceId;
    
    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
        this.resourceType = null;
        this.resourceId = null;
    }
    
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super("RESOURCE_NOT_FOUND", String.format("%s com ID '%s' não encontrado", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    public ResourceNotFoundException(String message, String errorCode, Object[] parameters) {
        super(errorCode, message, parameters);
        this.resourceType = null;
        this.resourceId = null;
    }
    
    public ResourceNotFoundException(String message, String errorCode, Object[] parameters, Throwable cause) {
        super(errorCode, message, cause, parameters);
        this.resourceType = null;
        this.resourceId = null;
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super("RESOURCE_NOT_FOUND", message, cause);
        this.resourceType = null;
        this.resourceId = null;
    }
    
    public ResourceNotFoundException(String errorCode, String message, String resourceType, String resourceId) {
        super(errorCode, message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public String getResourceId() {
        return resourceId;
    }
}