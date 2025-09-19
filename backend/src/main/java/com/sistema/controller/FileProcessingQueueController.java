package com.sistema.controller;

import com.sistema.entity.FileProcessingQueue;
import com.sistema.repository.FileProcessingQueueRepository;
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
 * Controller para gerenciamento da fila de processamento de arquivos
 */
@RestController
@RequestMapping("/api/batch/file-queue")
@CrossOrigin(origins = "*")
public class FileProcessingQueueController {

    @Autowired
    private FileProcessingQueueRepository fileProcessingQueueRepository;

    /**
     * DTO para requisição de adição de arquivo à fila
     */
    public static class AddFileRequest {
        private String sourceFilePath;
        private String targetFilePath;
        private FileProcessingQueue.OperationType operationType;
        private FileProcessingQueue.Priority priority = FileProcessingQueue.Priority.MEDIUM;
        private String parameters;

        // Getters e Setters
        public String getSourceFilePath() { return sourceFilePath; }
        public void setSourceFilePath(String sourceFilePath) { this.sourceFilePath = sourceFilePath; }
        
        public String getTargetFilePath() { return targetFilePath; }
        public void setTargetFilePath(String targetFilePath) { this.targetFilePath = targetFilePath; }
        
        public FileProcessingQueue.OperationType getOperationType() { return operationType; }
        public void setOperationType(FileProcessingQueue.OperationType operationType) { this.operationType = operationType; }
        
        public FileProcessingQueue.Priority getPriority() { return priority; }
        public void setPriority(FileProcessingQueue.Priority priority) { this.priority = priority; }
        
        public String getParameters() { return parameters; }
        public void setParameters(String parameters) { this.parameters = parameters; }
    }

    /**
     * Adiciona um arquivo à fila de processamento
     */
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> addFileToQueue(@Valid @RequestBody AddFileRequest request) {
        try {
            // Validações básicas
            if (request.getSourceFilePath() == null || request.getSourceFilePath().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Caminho do arquivo de origem é obrigatório");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getOperationType() == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Tipo de operação é obrigatório");
                return ResponseEntity.badRequest().body(response);
            }

            FileProcessingQueue fileQueue = new FileProcessingQueue();
            fileQueue.setSourcePath(request.getSourceFilePath());
            fileQueue.setTargetPath(request.getTargetFilePath());
            fileQueue.setOperationType(request.getOperationType());
            fileQueue.setPriority(request.getPriority());
            fileQueue.setMetadata(request.getParameters());
            fileQueue.setStatus(FileProcessingQueue.ProcessingStatus.PENDING);
            fileQueue.setCreatedAt(LocalDateTime.now());
            
            fileQueue = fileProcessingQueueRepository.save(fileQueue);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Arquivo adicionado à fila com sucesso");
            response.put("fileId", fileQueue.getId());
            response.put("status", fileQueue.getStatus().name());
            response.put("priority", fileQueue.getPriority().name());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao adicionar arquivo à fila: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lista arquivos na fila com paginação e filtros
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> listFileQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String priority) {
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<FileProcessingQueue> filePage = fileProcessingQueueRepository.findAll(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("files", filePage.getContent());
            response.put("totalElements", filePage.getTotalElements());
            response.put("totalPages", filePage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao listar arquivos: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Obtém detalhes de um arquivo específico
     */
    @GetMapping("/{fileId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getFile(@PathVariable Long fileId) {
        try {
            Optional<FileProcessingQueue> fileOpt = fileProcessingQueueRepository.findById(fileId);
            
            if (fileOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Arquivo não encontrado");
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("file", fileOpt.get());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao buscar arquivo: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Obtém estatísticas da fila de arquivos
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getFileQueueStatistics() {
        try {
            long totalFiles = fileProcessingQueueRepository.count();
            long pendingFiles = fileProcessingQueueRepository.countByStatus(FileProcessingQueue.ProcessingStatus.PENDING);
            long processingFiles = fileProcessingQueueRepository.countByStatus(FileProcessingQueue.ProcessingStatus.PROCESSING);
            long completedFiles = fileProcessingQueueRepository.countByStatus(FileProcessingQueue.ProcessingStatus.COMPLETED);
            long failedFiles = fileProcessingQueueRepository.countByStatus(FileProcessingQueue.ProcessingStatus.FAILED);
            
            long highPriorityFiles = fileProcessingQueueRepository.countByPriority(FileProcessingQueue.Priority.HIGH);
            long mediumPriorityFiles = fileProcessingQueueRepository.countByPriority(FileProcessingQueue.Priority.MEDIUM);
            long lowPriorityFiles = fileProcessingQueueRepository.countByPriority(FileProcessingQueue.Priority.LOW);
            
            Map<String, Object> priorityStats = new HashMap<>();
            priorityStats.put("high", highPriorityFiles);
            priorityStats.put("medium", mediumPriorityFiles);
            priorityStats.put("low", lowPriorityFiles);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", Map.of(
                "total", totalFiles,
                "pending", pendingFiles,
                "processing", processingFiles,
                "completed", completedFiles,
                "failed", failedFiles,
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
     * Remove arquivos antigos da fila
     */
    @DeleteMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cleanupOldFiles(
            @RequestParam(defaultValue = "30") int daysOld) {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
            List<FileProcessingQueue> oldFiles = fileProcessingQueueRepository.findOldCompletedFiles(
                FileProcessingQueue.ProcessingStatus.COMPLETED,
                FileProcessingQueue.ProcessingStatus.FAILED,
                cutoffDate);
            fileProcessingQueueRepository.deleteAll(oldFiles);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Limpeza concluída");
            response.put("deletedCount", oldFiles.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro na limpeza: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Reprocessa arquivos falhados
     */
    @PostMapping("/retry-failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> retryFailedFiles() {
        try {
            List<FileProcessingQueue> failedFiles = fileProcessingQueueRepository.findByStatus(FileProcessingQueue.ProcessingStatus.FAILED);
            for (FileProcessingQueue file : failedFiles) {
                file.setStatus(FileProcessingQueue.ProcessingStatus.PENDING);
                file.setAttempts(0);
                file.setErrorMessage(null);
            }
            fileProcessingQueueRepository.saveAll(failedFiles);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Arquivos falhados reprocessados");
            response.put("retriedCount", failedFiles.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao reprocessar arquivos: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Cancela um arquivo pendente
     */
    @PostMapping("/{fileId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cancelFile(@PathVariable Long fileId) {
        try {
            Optional<FileProcessingQueue> fileOpt = fileProcessingQueueRepository.findById(fileId);
            
            if (fileOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Arquivo não encontrado");
                return ResponseEntity.notFound().build();
            }
            
            FileProcessingQueue file = fileOpt.get();
            if (file.getStatus() != FileProcessingQueue.ProcessingStatus.PENDING) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Apenas arquivos pendentes podem ser cancelados");
                return ResponseEntity.badRequest().body(response);
            }
            
            file.setStatus(FileProcessingQueue.ProcessingStatus.FAILED);
            file.setErrorMessage("Cancelado pelo usuário");
            fileProcessingQueueRepository.save(file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Arquivo cancelado com sucesso");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao cancelar arquivo: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Desbloqueia arquivos travados
     */
    @PostMapping("/unlock-stuck")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> unlockStuckFiles(
            @RequestParam(defaultValue = "60") int minutesStuck) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(minutesStuck);
            List<FileProcessingQueue> stuckFiles = fileProcessingQueueRepository.findStuckFiles(
                FileProcessingQueue.ProcessingStatus.PROCESSING, cutoffTime);
            
            for (FileProcessingQueue file : stuckFiles) {
                file.setStatus(FileProcessingQueue.ProcessingStatus.PENDING);
            }
            fileProcessingQueueRepository.saveAll(stuckFiles);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Arquivos desbloqueados");
            response.put("unlockedCount", stuckFiles.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao desbloquear arquivos: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lista tipos de operação disponíveis
     */
    @GetMapping("/operation-types")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getOperationTypes() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("operationTypes", FileProcessingQueue.OperationType.values());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao obter tipos de operação: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}