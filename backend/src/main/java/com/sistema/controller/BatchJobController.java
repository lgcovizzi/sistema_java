package com.sistema.controller;

import com.sistema.entity.BatchJob;
import com.sistema.service.BatchJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador REST para gerenciar jobs do Spring Batch
 * Fornece endpoints para criar, monitorar e gerenciar jobs de processamento
 */
@RestController
@RequestMapping("/api/batch")
@CrossOrigin(origins = "*")
public class BatchJobController {

    @Autowired
    private BatchJobService batchJobService;

    /**
     * Executa um job de processamento de emails
     */
    @PostMapping("/email/execute")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> executeEmailJob() {
        try {
            BatchJob job = batchJobService.executeEmailProcessingJob();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Job de processamento de emails iniciado com sucesso");
            response.put("jobId", job.getId());
            response.put("jobName", job.getJobName());
            response.put("status", job.getStatus().name());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao executar job de emails: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Executa um job de redimensionamento de imagens
     */
    @PostMapping("/image/execute")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> executeImageResizeJob() {
        try {
            BatchJob job = batchJobService.executeImageResizeJob();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Job de redimensionamento de imagens iniciado com sucesso");
            response.put("jobId", job.getId());
            response.put("jobName", job.getJobName());
            response.put("status", job.getStatus().name());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao executar job de imagens: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Executa um job de processamento de arquivos
     */
    @PostMapping("/file/execute")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> executeFileProcessingJob() {
        try {
            BatchJob job = batchJobService.executeFileProcessingJob();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Job de processamento de arquivos iniciado com sucesso");
            response.put("jobId", job.getId());
            response.put("jobName", job.getJobName());
            response.put("status", job.getStatus().name());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao executar job de arquivos: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Busca um job específico por ID
     */
    @GetMapping("/{jobId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getJob(@PathVariable Long jobId) {
        Optional<BatchJob> job = batchJobService.findById(jobId);
        if (job.isPresent()) {
            BatchJob batchJob = job.get();
            
            Map<String, Object> jobData = new HashMap<>();
            jobData.put("id", batchJob.getId());
            jobData.put("jobName", batchJob.getJobName());
            jobData.put("type", batchJob.getJobType().name());
            jobData.put("status", batchJob.getStatus().name());
            jobData.put("startTime", batchJob.getStartedAt());
            jobData.put("endTime", batchJob.getCompletedAt());
            jobData.put("itemsProcessed", batchJob.getProcessedItems());
            jobData.put("totalItems", batchJob.getTotalItems());
            jobData.put("errorMessage", batchJob.getErrorMessage());
            jobData.put("progress", batchJob.getProgress());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("job", jobData);
            
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Lista jobs com paginação e filtros
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> listJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<BatchJob> jobs;
            
            if (type != null && status != null) {
                BatchJob.JobType jobType = BatchJob.JobType.valueOf(type.toUpperCase());
                BatchJob.JobStatus jobStatus = BatchJob.JobStatus.valueOf(status.toUpperCase());
                jobs = batchJobService.findByTypeAndStatus(jobType, jobStatus, pageable);
            } else if (type != null) {
                BatchJob.JobType jobType = BatchJob.JobType.valueOf(type.toUpperCase());
                jobs = batchJobService.findByType(jobType, pageable);
            } else if (status != null) {
                BatchJob.JobStatus jobStatus = BatchJob.JobStatus.valueOf(status.toUpperCase());
                jobs = batchJobService.findByStatus(jobStatus, pageable);
            } else {
                jobs = batchJobService.findAll(pageable);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("jobs", jobs.getContent());
            response.put("totalElements", jobs.getTotalElements());
            response.put("totalPages", jobs.getTotalPages());
            response.put("currentPage", jobs.getNumber());
            response.put("size", jobs.getSize());
            response.put("hasNext", jobs.hasNext());
            response.put("hasPrevious", jobs.hasPrevious());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao listar jobs: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Obtém estatísticas dos jobs
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getJobStatistics() {
        try {
            Map<String, Object> stats = batchJobService.getJobStatistics();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao obter estatísticas: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Remove jobs antigos (limpeza)
     */
    @DeleteMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cleanupOldJobs(
            @RequestParam(defaultValue = "30") int daysOld) {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
            int deletedCount = batchJobService.deleteOldJobs(cutoffDate);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Limpeza concluída com sucesso");
            response.put("deletedJobs", deletedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro na limpeza: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Cancela um job em execução (se possível)
     */
    @PostMapping("/{jobId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cancelJob(@PathVariable Long jobId) {
        try {
            boolean cancelled = batchJobService.cancelJob(jobId);
            Map<String, Object> response = new HashMap<>();
            if (cancelled) {
                response.put("success", true);
                response.put("message", "Job cancelado com sucesso");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Não foi possível cancelar o job (pode já ter terminado)");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao cancelar job: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Obtém jobs em execução
     */
    @GetMapping("/running")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getRunningJobs() {
        try {
            var runningJobs = batchJobService.findByStatus(
                BatchJob.JobStatus.RUNNING, 
                PageRequest.of(0, 50, Sort.by("startedAt").descending())
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("runningJobs", runningJobs.getContent());
            response.put("count", runningJobs.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao obter jobs em execução: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}