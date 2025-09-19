package com.sistema.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidade para controle da fila de processamento de arquivos.
 * Gerencia operações como listagem, análise, compressão e organização de arquivos.
 */
@Entity
@Table(name = "file_processing_queue")
public class FileProcessingQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Caminho de origem é obrigatório")
    @Size(max = 1000, message = "Caminho de origem deve ter no máximo 1000 caracteres")
    @Column(name = "source_path", nullable = false, length = 1000)
    private String sourcePath;

    @Size(max = 1000, message = "Caminho de destino deve ter no máximo 1000 caracteres")
    @Column(name = "target_path", length = 1000)
    private String targetPath;

    @NotNull(message = "Tipo de operação é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProcessingStatus status = ProcessingStatus.PENDING;

    @NotNull(message = "Prioridade é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "batch_job_id")
    private Long batchJobId;

    @Size(max = 100, message = "Usuário criador deve ter no máximo 100 caracteres")
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 3;

    @Size(max = 2000, message = "Mensagem de erro deve ter no máximo 2000 caracteres")
    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    // Campos específicos para operações de arquivo
    @Column(name = "file_pattern")
    private String filePattern;

    @Column(name = "include_subdirectories")
    private Boolean includeSubdirectories = true;

    @Column(name = "file_count")
    private Long fileCount;

    @Column(name = "total_size_bytes")
    private Long totalSizeBytes;

    @Column(name = "compression_level")
    private Integer compressionLevel;

    @Column(name = "compression_format")
    private String compressionFormat;

    @Column(name = "delete_source_after_processing")
    private Boolean deleteSourceAfterProcessing = false;

    @Column(name = "preserve_directory_structure")
    private Boolean preserveDirectoryStructure = true;

    @Size(max = 500, message = "Filtros devem ter no máximo 500 caracteres")
    @Column(name = "filters", length = 500)
    private String filters;

    @Size(max = 2000, message = "Metadados devem ter no máximo 2000 caracteres")
    @Column(name = "metadata", length = 2000)
    private String metadata;

    @Size(max = 1000, message = "Resultado deve ter no máximo 1000 caracteres")
    @Column(name = "result", length = 1000)
    private String result;

    // Construtores
    public FileProcessingQueue() {}

    public FileProcessingQueue(String sourcePath, OperationType operationType) {
        this.sourcePath = sourcePath;
        this.operationType = operationType;
    }

    public FileProcessingQueue(String sourcePath, String targetPath, OperationType operationType, Priority priority) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.operationType = operationType;
        this.priority = priority;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
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

    public Long getBatchJobId() {
        return batchJobId;
    }

    public void setBatchJobId(Long batchJobId) {
        this.batchJobId = batchJobId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public String getFilePattern() {
        return filePattern;
    }

    public void setFilePattern(String filePattern) {
        this.filePattern = filePattern;
    }

    public Boolean getIncludeSubdirectories() {
        return includeSubdirectories;
    }

    public void setIncludeSubdirectories(Boolean includeSubdirectories) {
        this.includeSubdirectories = includeSubdirectories;
    }

    public Long getFileCount() {
        return fileCount;
    }

    public void setFileCount(Long fileCount) {
        this.fileCount = fileCount;
    }

    public Long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public void setTotalSizeBytes(Long totalSizeBytes) {
        this.totalSizeBytes = totalSizeBytes;
    }

    public Integer getCompressionLevel() {
        return compressionLevel;
    }

    public void setCompressionLevel(Integer compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    public String getCompressionFormat() {
        return compressionFormat;
    }

    public void setCompressionFormat(String compressionFormat) {
        this.compressionFormat = compressionFormat;
    }

    public Boolean getDeleteSourceAfterProcessing() {
        return deleteSourceAfterProcessing;
    }

    public void setDeleteSourceAfterProcessing(Boolean deleteSourceAfterProcessing) {
        this.deleteSourceAfterProcessing = deleteSourceAfterProcessing;
    }

    public Boolean getPreserveDirectoryStructure() {
        return preserveDirectoryStructure;
    }

    public void setPreserveDirectoryStructure(Boolean preserveDirectoryStructure) {
        this.preserveDirectoryStructure = preserveDirectoryStructure;
    }

    public String getFilters() {
        return filters;
    }

    public void setFilters(String filters) {
        this.filters = filters;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    // Métodos utilitários
    public boolean isPending() {
        return ProcessingStatus.PENDING.equals(this.status);
    }

    public boolean isProcessing() {
        return ProcessingStatus.PROCESSING.equals(this.status);
    }

    public boolean isCompleted() {
        return ProcessingStatus.COMPLETED.equals(this.status);
    }

    public boolean isFailed() {
        return ProcessingStatus.FAILED.equals(this.status);
    }

    public boolean canRetry() {
        return isFailed() && attempts < maxAttempts;
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public void markAsStarted() {
        this.status = ProcessingStatus.PROCESSING;
        this.startedAt = LocalDateTime.now();
        incrementAttempts();
    }

    public void markAsCompleted() {
        this.status = ProcessingStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markAsFailed(String errorMessage) {
        this.status = ProcessingStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public void calculateProcessingTime() {
        if (startedAt != null && completedAt != null) {
            this.processingTimeMs = java.time.Duration.between(startedAt, completedAt).toMillis();
        }
    }

    // Enums
    public enum OperationType {
        LIST_FILES("Listar arquivos"),
        ANALYZE_DIRECTORY("Analisar diretório"),
        COMPRESS_FILES("Comprimir arquivos"),
        EXTRACT_FILES("Extrair arquivos"),
        ORGANIZE_FILES("Organizar arquivos"),
        DUPLICATE_DETECTION("Detectar duplicatas"),
        FILE_VALIDATION("Validar arquivos"),
        METADATA_EXTRACTION("Extrair metadados"),
        BATCH_RENAME("Renomear em lote"),
        SYNC_DIRECTORIES("Sincronizar diretórios");

        private final String description;

        OperationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum ProcessingStatus {
        PENDING("Pendente"),
        PROCESSING("Processando"),
        COMPLETED("Concluído"),
        FAILED("Falhou");

        private final String description;

        ProcessingStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum Priority {
        LOW("Baixa"),
        MEDIUM("Média"),
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

    // equals e hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileProcessingQueue that = (FileProcessingQueue) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // toString
    @Override
    public String toString() {
        return "FileProcessingQueue{" +
                "id=" + id +
                ", sourcePath='" + sourcePath + '\'' +
                ", targetPath='" + targetPath + '\'' +
                ", operationType=" + operationType +
                ", status=" + status +
                ", priority=" + priority +
                ", attempts=" + attempts +
                ", maxAttempts=" + maxAttempts +
                ", createdAt=" + createdAt +
                '}';
    }
}