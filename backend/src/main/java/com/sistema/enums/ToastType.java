package com.sistema.enums;

/**
 * Tipos de mensagens de toast/notificação
 */
public enum ToastType {
    SUCCESS("success"),
    ERROR("error"),
    WARNING("warning"),
    INFO("info");
    
    private final String value;
    
    ToastType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}