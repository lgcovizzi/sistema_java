package com.sistema.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade para fila de emails.
 * Armazena emails que devem ser processados pelo Spring Batch.
 */
@Entity
@Table(name = "email_queue")
public class EmailQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "email_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EmailType emailType;

    @Column(name = "priority", nullable = false)
    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private EmailStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "attempts")
    private Integer attempts;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "template_variables", columnDefinition = "TEXT")
    private String templateVariables;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "batch_job_id")
    private Long batchJobId;

    // Construtores
    public EmailQueue() {
        this.createdAt = LocalDateTime.now();
        this.status = EmailStatus.PENDING;
        this.priority = Priority.NORMAL;
        this.attempts = 0;
        this.maxAttempts = 3;
    }

    public EmailQueue(String recipientEmail, String subject, String content, EmailType emailType) {
        this();
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.content = content;
        this.emailType = emailType;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public EmailType getEmailType() {
        return emailType;
    }

    public void setEmailType(EmailType emailType) {
        this.emailType = emailType;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public EmailStatus getStatus() {
        return status;
    }

    public void setStatus(EmailStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateVariables() {
        return templateVariables;
    }

    public void setTemplateVariables(String templateVariables) {
        this.templateVariables = templateVariables;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Long getBatchJobId() {
        return batchJobId;
    }

    public void setBatchJobId(Long batchJobId) {
        this.batchJobId = batchJobId;
    }

    // Métodos utilitários
    public void incrementAttempts() {
        this.attempts = (this.attempts == null) ? 1 : this.attempts + 1;
    }

    public boolean hasReachedMaxAttempts() {
        return this.attempts != null && this.maxAttempts != null && this.attempts >= this.maxAttempts;
    }

    public void markAsSent() {
        this.status = EmailStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = EmailStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void markAsProcessing() {
        this.status = EmailStatus.PROCESSING;
    }

    public boolean isScheduled() {
        return this.scheduledAt != null && this.scheduledAt.isAfter(LocalDateTime.now());
    }

    public boolean isReadyToProcess() {
        return this.status == EmailStatus.PENDING && 
               (this.scheduledAt == null || this.scheduledAt.isBefore(LocalDateTime.now()) || this.scheduledAt.isEqual(LocalDateTime.now()));
    }

    /**
     * Enum para tipos de email.
     */
    public enum EmailType {
        VERIFICATION("Email de Verificação"),
        PASSWORD_RECOVERY("Recuperação de Senha"),
        NOTIFICATION("Notificação"),
        MARKETING("Marketing"),
        SYSTEM("Sistema"),
        BULK("Envio em Massa");

        private final String description;

        EmailType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Enum para prioridade de email.
     */
    public enum Priority {
        LOW("Baixa"),
        NORMAL("Normal"),
        HIGH("Alta"),
        URGENT("Urgente");

        private final String description;

        Priority(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Enum para status de email.
     */
    public enum EmailStatus {
        PENDING("Pendente"),
        PROCESSING("Processando"),
        SENT("Enviado"),
        FAILED("Falhou"),
        CANCELLED("Cancelado");

        private final String description;

        EmailStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}