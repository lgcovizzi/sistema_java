package com.sistema.controller;

import com.sistema.service.ToastService;
import com.sistema.service.ToastService.ToastMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller para gerenciar mensagens toast via API REST
 */
@RestController
@RequestMapping("/api/toast")
public class ToastController {
    
    @Autowired
    private ToastService toastService;
    
    /**
     * Obtém todas as mensagens toast e as remove da sessão
     */
    @GetMapping("/messages")
    public ResponseEntity<List<ToastMessage>> getMessages() {
        List<ToastMessage> messages = toastService.getAndClearMessages();
        return ResponseEntity.ok(messages);
    }
    
    /**
     * Obtém mensagens sem removê-las da sessão
     */
    @GetMapping("/messages/peek")
    public ResponseEntity<List<ToastMessage>> peekMessages() {
        List<ToastMessage> messages = toastService.getMessages();
        return ResponseEntity.ok(messages);
    }
    
    /**
     * Adiciona uma nova mensagem toast
     */
    @PostMapping("/message")
    public ResponseEntity<Map<String, String>> addMessage(@Valid @RequestBody ToastRequest request) {
        switch (request.getType().toLowerCase()) {
            case "success":
                if (request.getTitle() != null) {
                    toastService.success(request.getTitle(), request.getMessage());
                } else {
                    toastService.success(request.getMessage());
                }
                break;
            case "error":
                if (request.getTitle() != null) {
                    toastService.error(request.getTitle(), request.getMessage());
                } else {
                    toastService.error(request.getMessage());
                }
                break;
            case "warning":
                if (request.getTitle() != null) {
                    toastService.warning(request.getTitle(), request.getMessage());
                } else {
                    toastService.warning(request.getMessage());
                }
                break;
            case "info":
                if (request.getTitle() != null) {
                    toastService.info(request.getTitle(), request.getMessage());
                } else {
                    toastService.info(request.getMessage());
                }
                break;
            default:
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Tipo de toast inválido");
                return ResponseEntity.badRequest().body(errorResponse);
        }
        
        Map<String, String> successResponse = new HashMap<>();
        successResponse.put("status", "success");
        return ResponseEntity.ok(successResponse);
    }
    
    /**
     * Adiciona mensagem de sucesso
     */
    @PostMapping("/success")
    public ResponseEntity<Map<String, String>> addSuccessMessage(@Valid @RequestBody MessageRequest request) {
        if (request.getTitle() != null) {
            toastService.success(request.getTitle(), request.getMessage());
        } else {
            toastService.success(request.getMessage());
        }
        Map<String, String> successResponse1 = new HashMap<>();
        successResponse1.put("status", "success");
        return ResponseEntity.ok(successResponse1);
    }
    
    /**
     * Adiciona mensagem de erro
     */
    @PostMapping("/error")
    public ResponseEntity<Map<String, String>> addErrorMessage(@Valid @RequestBody MessageRequest request) {
        if (request.getTitle() != null) {
            toastService.error(request.getTitle(), request.getMessage());
        } else {
            toastService.error(request.getMessage());
        }
        Map<String, String> successResponse2 = new HashMap<>();
        successResponse2.put("status", "success");
        return ResponseEntity.ok(successResponse2);
    }
    
    /**
     * Adiciona mensagem de aviso
     */
    @PostMapping("/warning")
    public ResponseEntity<Map<String, String>> addWarningMessage(@Valid @RequestBody MessageRequest request) {
        if (request.getTitle() != null) {
            toastService.warning(request.getTitle(), request.getMessage());
        } else {
            toastService.warning(request.getMessage());
        }
        Map<String, String> successResponse3 = new HashMap<>();
        successResponse3.put("status", "success");
        return ResponseEntity.ok(successResponse3);
    }
    
    /**
     * Adiciona mensagem de informação
     */
    @PostMapping("/info")
    public ResponseEntity<Map<String, String>> addInfoMessage(@Valid @RequestBody MessageRequest request) {
        if (request.getTitle() != null) {
            toastService.info(request.getTitle(), request.getMessage());
        } else {
            toastService.info(request.getMessage());
        }
        Map<String, String> successResponse4 = new HashMap<>();
        successResponse4.put("status", "success");
        return ResponseEntity.ok(successResponse4);
    }
    
    /**
     * Limpa todas as mensagens
     */
    @DeleteMapping("/messages")
    public ResponseEntity<Map<String, String>> clearMessages() {
        toastService.clearMessages();
        Map<String, String> response = new HashMap<>();
        response.put("status", "cleared");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Verifica se há mensagens
     */
    @GetMapping("/has-messages")
    public ResponseEntity<Map<String, Object>> hasMessages() {
        boolean hasMessages = toastService.hasMessages();
        int count = toastService.getMessageCount();
        
        Map<String, Object> response = new HashMap<>();
        response.put("hasMessages", hasMessages);
        response.put("count", count);
        return ResponseEntity.ok(response);
    }
    
    /**
     * DTO para requisições de toast completas
     */
    public static class ToastRequest {
        @NotBlank(message = "Tipo é obrigatório")
        @Pattern(regexp = "^(success|error|warning|info)$", 
                message = "Tipo deve ser: success, error, warning ou info")
        private String type;
        
        private String title;
        
        @NotBlank(message = "Mensagem é obrigatória")
        private String message;
        
        // Getters and Setters
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
    
    /**
     * DTO para requisições de mensagem simples
     */
    public static class MessageRequest {
        private String title;
        
        @NotBlank(message = "Mensagem é obrigatória")
        private String message;
        
        // Getters and Setters
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}