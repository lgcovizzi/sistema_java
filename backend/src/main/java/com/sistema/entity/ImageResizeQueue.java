package com.sistema.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade para fila de redução de imagens.
 * Armazena informações sobre imagens que devem ser redimensionadas.
 */
@Entity
@Table(name = "image_resize_queue")
public class ImageResizeQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_path", nullable = false)
    private String originalPath;

    @Column(name = "target_path", nullable = false)
    private String targetPath;

    @Column(name = "original_width")
    private Integer originalWidth;

    @Column(name = "original_height")
    private Integer originalHeight;

    @Column(name = "target_width", nullable = false)
    private Integer targetWidth;

    @Column(name = "target_height", nullable = false)
    private Integer targetHeight;

    @Column(name = "quality")
    private Float quality;

    @Column(name = "format")
    private String format;

    @Column(name = "resize_mode", nullable = false)
    @Enumerated(EnumType.STRING)
    private ResizeMode resizeMode;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    @Column(name = "priority", nullable = false)
    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "attempts")
    private Integer attempts;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "original_size_bytes")
    private Long originalSizeBytes;

    @Column(name = "target_size_bytes")
    private Long targetSizeBytes;

    @Column(name = "compression_ratio")
    private Float compressionRatio;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "batch_job_id")
    private Long batchJobId;

    @Column(name = "preserve_metadata")
    private Boolean preserveMetadata;

    @Column(name = "watermark_text")
    private String watermarkText;

    // Construtores
    public ImageResizeQueue() {
        this.createdAt = LocalDateTime.now();
        this.status = ProcessingStatus.PENDING;
        this.priority = Priority.NORMAL;
        this.attempts = 0;
        this.maxAttempts = 3;
        this.quality = 0.8f;
        this.preserveMetadata = false;
    }

    public ImageResizeQueue(String originalPath, String targetPath, Integer targetWidth, Integer targetHeight, ResizeMode resizeMode) {
        this();
        this.originalPath = originalPath;
        this.targetPath = targetPath;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.resizeMode = resizeMode;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public Integer getOriginalWidth() {
        return originalWidth;
    }

    public void setOriginalWidth(Integer originalWidth) {
        this.originalWidth = originalWidth;
    }

    public Integer getOriginalHeight() {
        return originalHeight;
    }

    public void setOriginalHeight(Integer originalHeight) {
        this.originalHeight = originalHeight;
    }

    public Integer getTargetWidth() {
        return targetWidth;
    }

    public void setTargetWidth(Integer targetWidth) {
        this.targetWidth = targetWidth;
    }

    public Integer getTargetHeight() {
        return targetHeight;
    }

    public void setTargetHeight(Integer targetHeight) {
        this.targetHeight = targetHeight;
    }

    public Float getQuality() {
        return quality;
    }

    public void setQuality(Float quality) {
        this.quality = quality;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public ResizeMode getResizeMode() {
        return resizeMode;
    }

    public void setResizeMode(ResizeMode resizeMode) {
        this.resizeMode = resizeMode;
    }

    public ProcessingStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessingStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
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

    public Long getOriginalSizeBytes() {
        return originalSizeBytes;
    }

    public void setOriginalSizeBytes(Long originalSizeBytes) {
        this.originalSizeBytes = originalSizeBytes;
    }

    public Long getTargetSizeBytes() {
        return targetSizeBytes;
    }

    public void setTargetSizeBytes(Long targetSizeBytes) {
        this.targetSizeBytes = targetSizeBytes;
    }

    public Float getCompressionRatio() {
        return compressionRatio;
    }

    public void setCompressionRatio(Float compressionRatio) {
        this.compressionRatio = compressionRatio;
    }

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
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

    public Boolean getPreserveMetadata() {
        return preserveMetadata;
    }

    public void setPreserveMetadata(Boolean preserveMetadata) {
        this.preserveMetadata = preserveMetadata;
    }

    public String getWatermarkText() {
        return watermarkText;
    }

    public void setWatermarkText(String watermarkText) {
        this.watermarkText = watermarkText;
    }

    // Métodos utilitários
    public void incrementAttempts() {
        this.attempts = (this.attempts == null) ? 1 : this.attempts + 1;
    }

    public boolean hasReachedMaxAttempts() {
        return this.attempts != null && this.maxAttempts != null && this.attempts >= this.maxAttempts;
    }

    public void markAsStarted() {
        this.status = ProcessingStatus.PROCESSING;
        this.startedAt = LocalDateTime.now();
    }

    public void markAsCompleted() {
        this.status = ProcessingStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        calculateCompressionRatio();
    }

    public void markAsFailed(String errorMessage) {
        this.status = ProcessingStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public boolean isReadyToProcess() {
        return this.status == ProcessingStatus.PENDING;
    }

    private void calculateCompressionRatio() {
        if (originalSizeBytes != null && targetSizeBytes != null && originalSizeBytes > 0) {
            this.compressionRatio = (float) targetSizeBytes / originalSizeBytes;
        }
    }

    public double getCompressionPercentage() {
        if (compressionRatio != null) {
            return (1.0 - compressionRatio) * 100.0;
        }
        return 0.0;
    }

    /**
     * Enum para modos de redimensionamento.
     */
    public enum ResizeMode {
        SCALE_TO_FIT("Ajustar mantendo proporção"),
        SCALE_TO_FILL("Preencher cortando se necessário"),
        STRETCH("Esticar para dimensões exatas"),
        CROP("Cortar para dimensões exatas"),
        THUMBNAIL("Miniatura com qualidade otimizada");

        private final String description;

        ResizeMode(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Enum para status de processamento.
     */
    public enum ProcessingStatus {
        PENDING("Pendente"),
        PROCESSING("Processando"),
        COMPLETED("Concluído"),
        FAILED("Falhou"),
        CANCELLED("Cancelado");

        private final String description;

        ProcessingStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Enum para prioridade de processamento.
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
}