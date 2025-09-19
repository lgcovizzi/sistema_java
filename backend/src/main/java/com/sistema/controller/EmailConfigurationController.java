package com.sistema.controller;

import com.sistema.entity.EmailConfiguration;
import com.sistema.enums.EmailProvider;
import com.sistema.service.EmailConfigurationService;
import com.sistema.service.SmtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller para gerenciamento de configurações de email.
 * Permite aos administradores configurar provedores de email (Mailtrap, Gmail).
 * 
 * @author Sistema
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin/email-config")
@PreAuthorize("hasRole('ADMIN')")
public class EmailConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(EmailConfigurationController.class);

    @Autowired
    private EmailConfigurationService emailConfigurationService;

    @Autowired
    private SmtpService smtpService;

    /**
     * Lista todas as configurações de email.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllConfigurations() {
        try {
            logger.info("Listando todas as configurações de email");
            
            List<EmailConfiguration> configurations = emailConfigurationService.getAllConfigurations();
            Optional<EmailConfiguration> defaultConfigOpt = emailConfigurationService.getDefaultConfiguration();
            EmailConfiguration defaultConfig = defaultConfigOpt.orElse(null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("configurations", configurations);
            response.put("defaultConfiguration", defaultConfig);
            response.put("totalConfigurations", configurations.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao listar configurações de email", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao listar configurações de email");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Obtém uma configuração específica por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getConfiguration(@PathVariable Long id) {
        try {
            logger.info("Obtendo configuração de email com ID: {}", id);
            
            EmailConfiguration configuration = emailConfigurationService.getConfiguration(id);
            
            if (configuration == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Configuração não encontrada");
                
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("configuration", configuration);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao obter configuração de email com ID: {}", id, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao obter configuração de email");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Cria uma nova configuração de email.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createConfiguration(@Valid @RequestBody EmailConfiguration configuration) {
        try {
            logger.info("Criando nova configuração de email para provedor: {}", configuration.getProvider());
            
            EmailConfiguration savedConfiguration = emailConfigurationService.createConfiguration(configuration);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Configuração criada com sucesso");
            response.put("configuration", savedConfiguration);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Erro de validação ao criar configuração: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Erro ao criar configuração de email", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao criar configuração de email");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Atualiza uma configuração existente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateConfiguration(
            @PathVariable Long id, 
            @Valid @RequestBody EmailConfiguration configuration) {
        try {
            logger.info("Atualizando configuração de email com ID: {}", id);
            
            EmailConfiguration updatedConfiguration = emailConfigurationService.updateConfiguration(id, configuration);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Configuração atualizada com sucesso");
            response.put("configuration", updatedConfiguration);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Erro de validação ao atualizar configuração: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Erro ao atualizar configuração de email com ID: {}", id, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao atualizar configuração de email");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Define uma configuração como padrão.
     */
    @PutMapping("/{id}/set-default")
    public ResponseEntity<Map<String, Object>> setAsDefault(@PathVariable Long id) {
        try {
            logger.info("Definindo configuração com ID {} como padrão", id);
            
            emailConfigurationService.setAsDefault(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Configuração definida como padrão com sucesso");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Erro ao definir configuração como padrão: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Erro ao definir configuração como padrão com ID: {}", id, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao definir configuração como padrão");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Habilita ou desabilita uma configuração.
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleConfiguration(@PathVariable Long id) {
        try {
            logger.info("Alternando status da configuração com ID: {}", id);
            
            EmailConfiguration configuration = emailConfigurationService.getConfiguration(id);
            if (configuration == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Configuração não encontrada");
                
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            boolean newStatus = !configuration.getIsActive();
            emailConfigurationService.toggleConfiguration(id, newStatus);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", newStatus ? "Configuração habilitada" : "Configuração desabilitada");
            response.put("enabled", newStatus);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao alternar status da configuração com ID: {}", id, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao alternar status da configuração");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Remove uma configuração.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteConfiguration(@PathVariable Long id) {
        try {
            logger.info("Removendo configuração de email com ID: {}", id);
            
            emailConfigurationService.deleteConfiguration(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Configuração removida com sucesso");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Erro ao remover configuração: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Erro ao remover configuração de email com ID: {}", id, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao remover configuração de email");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Testa uma configuração de email.
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testConfiguration(@PathVariable Long id) {
        try {
            logger.info("Testando configuração de email com ID: {}", id);
            
            boolean testResult = smtpService.testConnection();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", testResult);
            response.put("message", testResult ? 
                "Teste de conexão realizado com sucesso" : 
                "Falha no teste de conexão");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao testar configuração de email com ID: {}", id, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao testar configuração de email");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Lista os provedores de email disponíveis.
     */
    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getAvailableProviders() {
        try {
            logger.info("Listando provedores de email disponíveis");
            
            EmailProvider[] providers = EmailProvider.values();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("providers", providers);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao listar provedores de email", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao listar provedores de email");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Obtém configurações padrão para um provedor específico.
     */
    @GetMapping("/providers/{provider}/defaults")
    public ResponseEntity<Map<String, Object>> getProviderDefaults(@PathVariable EmailProvider provider) {
        try {
            logger.info("Obtendo configurações padrão para provedor: {}", provider);
            
            Map<String, Object> defaults = provider.getDefaultConfiguration();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("provider", provider);
            response.put("defaults", defaults);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao obter configurações padrão para provedor: {}", provider, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao obter configurações padrão");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}