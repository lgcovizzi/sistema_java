package com.sistema.controller;

import com.sistema.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para testes de envio de email
 */
@RestController
@RequestMapping("/api/email")
public class EmailController {

    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);

    @Autowired
    private EmailService emailService;

    /**
     * Endpoint para testar envio de email
     * 
     * @param email Email de destino
     * @return Resposta com status do envio
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTestEmail(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Enviando email de teste para: {}", email);
            emailService.sendTestEmail(email);
            
            response.put("success", true);
            response.put("message", "Email de teste enviado com sucesso!");
            response.put("email", email);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao enviar email de teste", e);
            
            response.put("success", false);
            response.put("message", "Erro ao enviar email: " + e.getMessage());
            response.put("email", email);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Endpoint para enviar email de boas-vindas
     * 
     * @param email Email de destino
     * @param name Nome do usuário
     * @return Resposta com status do envio
     */
    @PostMapping("/welcome")
    public ResponseEntity<Map<String, Object>> sendWelcomeEmail(
            @RequestParam String email, 
            @RequestParam String name) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Enviando email de boas-vindas para: {} ({})", email, name);
            emailService.sendWelcomeEmail(email, name);
            
            response.put("success", true);
            response.put("message", "Email de boas-vindas enviado com sucesso!");
            response.put("email", email);
            response.put("name", name);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao enviar email de boas-vindas", e);
            
            response.put("success", false);
            response.put("message", "Erro ao enviar email: " + e.getMessage());
            response.put("email", email);
            response.put("name", name);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Endpoint para verificar configuração de email
     * 
     * @return Status da configuração
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getEmailConfig() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("success", true);
            response.put("message", "Configuração de email carregada");
            response.put("provider", "Mailtrap");
            response.put("host", "sandbox.smtp.mailtrap.io");
            response.put("port", 2525);
            response.put("tls_enabled", true);
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Erro ao verificar configuração de email", e);
            
            response.put("success", false);
            response.put("message", "Erro ao verificar configuração: " + e.getMessage());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}