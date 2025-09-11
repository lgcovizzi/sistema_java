package com.sistema.java.controller;

import com.sistema.java.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controller para testes e administração do sistema de email
 * Referência: Sistema de Email com MailHog - project_rules.md
 * Referência: Controle de Acesso - project_rules.md
 */
@RestController
@RequestMapping("/api/email")
public class EmailController {

    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);
    
    private final EmailService emailService;
    
    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }
    
    /**
     * Envia email de teste (apenas para administradores)
     * Referência: Controle de Acesso - project_rules.md
     */
    @PostMapping("/teste")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> enviarEmailTeste(@RequestParam String destinatario) {
        try {
            logger.info("Enviando email de teste para: {}", destinatario);
            
            boolean sucesso = emailService.enviarEmailTeste(destinatario);
            
            Map<String, Object> response = new HashMap<>();
            response.put("sucesso", sucesso);
            response.put("destinatario", destinatario);
            response.put("mensagem", sucesso ? 
                "Email de teste enviado com sucesso! Verifique o MailHog em http://localhost:8025" :
                "Falha ao enviar email de teste. Verifique as configurações do MailHog.");
            response.put("configuracaoMailHog", Map.of(
                "host", emailService.getMailHost(),
                "porta", emailService.getMailPort(),
                "remetente", emailService.getEmailFrom(),
                "webUI", "http://localhost:8025"
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao enviar email de teste: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("sucesso", false);
            response.put("erro", e.getMessage());
            response.put("mensagem", "Erro interno ao enviar email de teste");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Verifica status do serviço de email (apenas para administradores)
     * Referência: Controle de Acesso - project_rules.md
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> verificarStatusEmail() {
        try {
            boolean conectividade = emailService.verificarConexaoMailHog();
            Map<String, Object> estatisticas = emailService.obterEstatisticasEmail();
            
            Map<String, Object> response = new HashMap<>();
            response.put("conectividade", conectividade);
            response.put("status", conectividade ? "Ativo" : "Inativo");
            response.put("estatisticas", estatisticas);
            response.put("configuracao", Map.of(
                "host", emailService.getMailHost(),
                "porta", emailService.getMailPort(),
                "remetente", emailService.getEmailFrom(),
                "nomeRemetente", emailService.getNomeRemetente(),
                "baseUrl", emailService.getBaseUrl()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao verificar status do email: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("conectividade", false);
            response.put("status", "Erro");
            response.put("erro", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Obtém estatísticas detalhadas de emails (apenas para administradores)
     * Referência: Controle de Acesso - project_rules.md
     */
    @GetMapping("/estatisticas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> obterEstatisticasEmail() {
        try {
            Map<String, Object> estatisticas = emailService.obterEstatisticasEmail();
            return ResponseEntity.ok(estatisticas);
            
        } catch (Exception e) {
            logger.error("Erro ao obter estatísticas de email: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("erro", e.getMessage());
            response.put("mensagem", "Erro ao obter estatísticas de email");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Envia email simples personalizado (apenas para administradores)
     * Referência: Controle de Acesso - project_rules.md
     */
    @PostMapping("/enviar-simples")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> enviarEmailSimples(
            @RequestParam String destinatario,
            @RequestParam String assunto,
            @RequestParam String conteudo) {
        try {
            logger.info("Enviando email simples para: {} com assunto: {}", destinatario, assunto);
            
            CompletableFuture<Boolean> resultado = emailService.enviarEmailSimples(destinatario, assunto, conteudo);
            boolean sucesso = resultado.join(); // Aguardar resultado
            
            Map<String, Object> response = new HashMap<>();
            response.put("sucesso", sucesso);
            response.put("destinatario", destinatario);
            response.put("assunto", assunto);
            response.put("mensagem", sucesso ? 
                "Email enviado com sucesso! Verifique o MailHog em http://localhost:8025" :
                "Falha ao enviar email. Verifique as configurações.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao enviar email simples: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("sucesso", false);
            response.put("erro", e.getMessage());
            response.put("mensagem", "Erro interno ao enviar email");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Endpoint para verificar configurações do MailHog (público para desenvolvimento)
     */
    @GetMapping("/mailhog-info")
    public ResponseEntity<Map<String, Object>> obterInfoMailHog() {
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("host", emailService.getMailHost());
            info.put("porta", emailService.getMailPort());
            info.put("webUI", "http://localhost:8025");
            info.put("remetente", emailService.getEmailFrom());
            info.put("status", emailService.verificarConexaoMailHog() ? "Ativo" : "Inativo");
            info.put("instrucoes", Map.of(
                "acessarWebUI", "Abra http://localhost:8025 no navegador para ver emails enviados",
                "testarEmail", "Use POST /api/email/teste?destinatario=test@localhost (requer autenticação admin)",
                "verificarStatus", "Use GET /api/email/status (requer autenticação admin)"
            ));
            
            return ResponseEntity.ok(info);
            
        } catch (Exception e) {
            logger.error("Erro ao obter informações do MailHog: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("erro", e.getMessage());
            response.put("mensagem", "Erro ao obter informações do MailHog");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}