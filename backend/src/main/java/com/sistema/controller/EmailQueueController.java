package com.sistema.controller;

import com.sistema.entity.EmailQueue;
import com.sistema.repository.EmailQueueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller para gerenciamento da fila de emails
 */
@RestController
@RequestMapping("/api/batch/email-queue")
@CrossOrigin(origins = "*")
public class EmailQueueController {

    @Autowired
    private EmailQueueRepository emailQueueRepository;

    /**
     * DTO para requisição de adição de email à fila
     */
    public static class AddEmailRequest {
        private String toEmail;
        private String subject;
        private String content;
        private EmailQueue.EmailType type = EmailQueue.EmailType.NOTIFICATION;
        private EmailQueue.Priority priority = EmailQueue.Priority.NORMAL;
        private LocalDateTime scheduledFor;

        // Getters e Setters
        public String getToEmail() { return toEmail; }
        public void setToEmail(String toEmail) { this.toEmail = toEmail; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public EmailQueue.EmailType getType() { return type; }
        public void setType(EmailQueue.EmailType type) { this.type = type; }
        
        public EmailQueue.Priority getPriority() { return priority; }
        public void setPriority(EmailQueue.Priority priority) { this.priority = priority; }
        
        public LocalDateTime getScheduledFor() { return scheduledFor; }
        public void setScheduledFor(LocalDateTime scheduledFor) { this.scheduledFor = scheduledFor; }
    }

    /**
     * Adiciona um email à fila de processamento
     */
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> addEmailToQueue(@Valid @RequestBody AddEmailRequest request) {
        try {
            // Validações básicas
            if (request.getToEmail() == null || request.getToEmail().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email de destino é obrigatório");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Assunto do email é obrigatório");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Conteúdo do email é obrigatório");
                return ResponseEntity.badRequest().body(response);
            }

            EmailQueue emailQueue = new EmailQueue();
            emailQueue.setRecipientEmail(request.getToEmail());
            emailQueue.setSubject(request.getSubject());
            emailQueue.setContent(request.getContent());
            emailQueue.setEmailType(request.getType());
            emailQueue.setPriority(request.getPriority());
            emailQueue.setScheduledAt(request.getScheduledFor());
            emailQueue = emailQueueRepository.save(emailQueue);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email adicionado à fila com sucesso");
            response.put("emailId", emailQueue.getId());
            response.put("status", emailQueue.getStatus().name());
            response.put("priority", emailQueue.getPriority().name());
            response.put("scheduledFor", emailQueue.getScheduledAt());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao adicionar email à fila: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lista emails na fila com paginação e filtros
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> listEmailQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String priority) {
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<EmailQueue> emailPage = emailQueueRepository.findAll(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("emails", emailPage.getContent());
            response.put("totalElements", emailPage.getTotalElements());
            response.put("totalPages", emailPage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao listar emails: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Obtém detalhes de um email específico
     */
    @GetMapping("/{emailId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getEmail(@PathVariable Long emailId) {
        try {
            Optional<EmailQueue> emailOpt = emailQueueRepository.findById(emailId);
            
            if (emailOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email não encontrado");
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("email", emailOpt.get());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao buscar email: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Obtém estatísticas da fila de emails
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getEmailQueueStatistics() {
        try {
            long totalEmails = emailQueueRepository.count();
            long pendingEmails = emailQueueRepository.countByStatus(EmailQueue.EmailStatus.PENDING);
            long processingEmails = emailQueueRepository.countByStatus(EmailQueue.EmailStatus.PROCESSING);
            long sentEmails = emailQueueRepository.countByStatus(EmailQueue.EmailStatus.SENT);
            long failedEmails = emailQueueRepository.countByStatus(EmailQueue.EmailStatus.FAILED);
            
            long highPriorityEmails = emailQueueRepository.countByPriority(EmailQueue.Priority.HIGH);
            long normalPriorityEmails = emailQueueRepository.countByPriority(EmailQueue.Priority.NORMAL);
            long lowPriorityEmails = emailQueueRepository.countByPriority(EmailQueue.Priority.LOW);
            
            Map<String, Object> priorityStats = new HashMap<>();
            priorityStats.put("high", highPriorityEmails);
            priorityStats.put("normal", normalPriorityEmails);
            priorityStats.put("low", lowPriorityEmails);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", Map.of(
                "total", totalEmails,
                "pending", pendingEmails,
                "processing", processingEmails,
                "sent", sentEmails,
                "failed", failedEmails,
                "pendingByPriority", priorityStats
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao obter estatísticas: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Remove emails antigos da fila
     */
    @DeleteMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cleanupOldEmails(
            @RequestParam(defaultValue = "30") int daysOld) {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
            List<EmailQueue> oldEmails = emailQueueRepository.findByCreatedAtBeforeAndStatusIn(
                cutoffDate, 
                List.of(EmailQueue.EmailStatus.SENT, EmailQueue.EmailStatus.FAILED)
            );
            int deletedCount = oldEmails.size();
            emailQueueRepository.deleteOldEmails(cutoffDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Limpeza concluída");
            response.put("deletedCount", deletedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro na limpeza: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Reprocessa emails falhados
     */
    @PostMapping("/retry-failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> retryFailedEmails() {
        try {
            // Buscar emails falhados e marcar como pendentes para reprocessamento
            List<EmailQueue> failedEmails = emailQueueRepository.findByStatus(EmailQueue.EmailStatus.FAILED);
            int retriedCount = 0;
            
            for (EmailQueue email : failedEmails) {
                if (email.getAttempts() < email.getMaxAttempts()) {
                    email.setStatus(EmailQueue.EmailStatus.PENDING);
                    email.setErrorMessage(null);
                    emailQueueRepository.save(email);
                    retriedCount++;
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Emails falhados reprocessados");
            response.put("retriedCount", retriedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao reprocessar emails: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Cancela um email pendente
     */
    @PostMapping("/{emailId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cancelEmail(@PathVariable Long emailId) {
        try {
            Optional<EmailQueue> emailOpt = emailQueueRepository.findById(emailId);
            
            if (emailOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email não encontrado");
                return ResponseEntity.notFound().build();
            }
            
            EmailQueue email = emailOpt.get();
            if (email.getStatus() != EmailQueue.EmailStatus.PENDING) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Apenas emails pendentes podem ser cancelados");
                return ResponseEntity.badRequest().body(response);
            }
            
            email.setStatus(EmailQueue.EmailStatus.CANCELLED);
            emailQueueRepository.save(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email cancelado com sucesso");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao cancelar email: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}