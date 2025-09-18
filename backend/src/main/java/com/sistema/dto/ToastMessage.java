package com.sistema.dto;

import com.sistema.enums.ToastType;

/**
 * DTO para mensagens de toast/notificação
 */
public class ToastMessage {
    
    private String message;
    private ToastType type;
    private String title;
    private Long duration;
    
    public ToastMessage() {}
    
    public ToastMessage(String message, ToastType type) {
        this.message = message;
        this.type = type;
    }
    
    public ToastMessage(String message, ToastType type, String title) {
        this.message = message;
        this.type = type;
        this.title = title;
    }
    
    public ToastMessage(String message, ToastType type, String title, Long duration) {
        this.message = message;
        this.type = type;
        this.title = title;
        this.duration = duration;
    }
    
    // Getters e Setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public ToastType getType() {
        return type;
    }
    
    public void setType(ToastType type) {
        this.type = type;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public Long getDuration() {
        return duration;
    }
    
    public void setDuration(Long duration) {
        this.duration = duration;
    }
    
    // Métodos de conveniência
    public static ToastMessage success(String message) {
        return new ToastMessage(message, ToastType.SUCCESS);
    }
    
    public static ToastMessage error(String message) {
        return new ToastMessage(message, ToastType.ERROR);
    }
    
    public static ToastMessage warning(String message) {
        return new ToastMessage(message, ToastType.WARNING);
    }
    
    public static ToastMessage info(String message) {
        return new ToastMessage(message, ToastType.INFO);
    }
}