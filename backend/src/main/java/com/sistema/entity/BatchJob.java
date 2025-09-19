package com.sistema.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade para controle de jobs do Spring Batch.
 * Armazena informações sobre execuções de jobs e seu status.
 */
@Entity
@Table(name = "batch_jobs")
public class BatchJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "job_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private JobType jobType;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(name = "parameters", columnDefinition = "TEXT")
    private String parameters;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processed_items")
    private Integer processedItems;

    @Column(name = "total_items")
    private Integer totalItems;

    @Column(name = "created_by")
    private String createdBy;

    // Construtores
    public BatchJob() {
        this.createdAt = LocalDateTime.now();
        this.status = JobStatus.PENDING;
    }

    public BatchJob(String jobName, JobType jobType, String parameters, String createdBy) {
        this();
        this.jobName = jobName;
        this.jobType = jobType;
        this.parameters = parameters;
        this.createdBy = createdBy;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getProcessedItems() {
        return processedItems;
    }

    public void setProcessedItems(Integer processedItems) {
        this.processedItems = processedItems;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    // Métodos utilitários
    public void markAsStarted() {
        this.status = JobStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public void markAsCompleted() {
        this.status = JobStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public double getProgress() {
        if (totalItems == null || totalItems == 0) {
            return 0.0;
        }
        if (processedItems == null) {
            return 0.0;
        }
        return (double) processedItems / totalItems * 100.0;
    }

    /**
     * Enum para tipos de jobs.
     */
    public enum JobType {
        EMAIL_PROCESSING("Processamento de Emails"),
        IMAGE_RESIZE("Redução de Imagens"),
        FILE_LISTING("Listagem de Arquivos"),
        BULK_EMAIL("Envio em Massa de Emails"),
        DATA_CLEANUP("Limpeza de Dados");

        private final String description;

        JobType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Enum para status de jobs.
     */
    public enum JobStatus {
        PENDING("Pendente"),
        RUNNING("Executando"),
        COMPLETED("Concluído"),
        FAILED("Falhou"),
        CANCELLED("Cancelado");

        private final String description;

        JobStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}