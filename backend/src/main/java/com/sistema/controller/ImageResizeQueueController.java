package com.sistema.controller;

import com.sistema.entity.ImageResizeQueue;
import com.sistema.repository.ImageResizeQueueRepository;
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
 * Controller para gerenciamento da fila de redimensionamento de imagens
 */
@RestController
@RequestMapping("/api/batch/image-queue")
@CrossOrigin(origins = "*")
public class ImageResizeQueueController {

    @Autowired
    private ImageResizeQueueRepository imageResizeQueueRepository;

    /**
     * DTO para requisição de adição de imagem à fila
     */
    public static class AddImageRequest {
        private String sourceFilePath;
        private String targetFilePath;
        private Integer targetWidth;
        private Integer targetHeight;
        private ImageResizeQueue.ResizeMode resizeMode = ImageResizeQueue.ResizeMode.SCALE_TO_FIT;
        private ImageResizeQueue.Priority priority = ImageResizeQueue.Priority.NORMAL;
        private Boolean applyWatermark = false;
        private String watermarkText;

        // Getters e Setters
        public String getSourceFilePath() { return sourceFilePath; }
        public void setSourceFilePath(String sourceFilePath) { this.sourceFilePath = sourceFilePath; }
        
        public String getTargetFilePath() { return targetFilePath; }
        public void setTargetFilePath(String targetFilePath) { this.targetFilePath = targetFilePath; }
        
        public Integer getTargetWidth() { return targetWidth; }
        public void setTargetWidth(Integer targetWidth) { this.targetWidth = targetWidth; }
        
        public Integer getTargetHeight() { return targetHeight; }
        public void setTargetHeight(Integer targetHeight) { this.targetHeight = targetHeight; }
        
        public ImageResizeQueue.ResizeMode getResizeMode() { return resizeMode; }
        public void setResizeMode(ImageResizeQueue.ResizeMode resizeMode) { this.resizeMode = resizeMode; }
        
        public ImageResizeQueue.Priority getPriority() { return priority; }
        public void setPriority(ImageResizeQueue.Priority priority) { this.priority = priority; }
        
        public Boolean getApplyWatermark() { return applyWatermark; }
        public void setApplyWatermark(Boolean applyWatermark) { this.applyWatermark = applyWatermark; }
        
        public String getWatermarkText() { return watermarkText; }
        public void setWatermarkText(String watermarkText) { this.watermarkText = watermarkText; }
    }

    /**
     * Adiciona uma imagem à fila de redimensionamento
     */
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> addImageToQueue(@Valid @RequestBody AddImageRequest request) {
        try {
            // Validações básicas
            if (request.getSourceFilePath() == null || request.getSourceFilePath().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Caminho do arquivo de origem é obrigatório");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getTargetWidth() == null || request.getTargetHeight() == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Largura e altura de destino são obrigatórias");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getTargetWidth() <= 0 || request.getTargetHeight() <= 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Largura e altura devem ser maiores que zero");
                return ResponseEntity.badRequest().body(response);
            }

            ImageResizeQueue imageQueue = new ImageResizeQueue();
            imageQueue.setOriginalPath(request.getSourceFilePath());
            imageQueue.setTargetPath(request.getTargetFilePath());
            imageQueue.setTargetWidth(request.getTargetWidth());
            imageQueue.setTargetHeight(request.getTargetHeight());
            imageQueue.setResizeMode(request.getResizeMode());
            imageQueue.setPriority(request.getPriority());
            if (request.getApplyWatermark() != null && request.getApplyWatermark()) {
                imageQueue.setWatermarkText(request.getWatermarkText());
            }
            imageQueue.setStatus(ImageResizeQueue.ProcessingStatus.PENDING);
            imageQueue.setCreatedAt(LocalDateTime.now());
            
            imageQueue = imageResizeQueueRepository.save(imageQueue);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Imagem adicionada à fila com sucesso");
            response.put("imageId", imageQueue.getId());
            response.put("status", imageQueue.getStatus().name());
            response.put("priority", imageQueue.getPriority().name());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao adicionar imagem à fila: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lista imagens na fila com paginação e filtros
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> listImageQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<ImageResizeQueue> imagePage = imageResizeQueueRepository.findAll(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("images", imagePage.getContent());
            response.put("totalElements", imagePage.getTotalElements());
            response.put("totalPages", imagePage.getTotalPages());
            response.put("currentPage", page);
            response.put("pageSize", size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao listar imagens: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Obtém detalhes de uma imagem específica
     */
    @GetMapping("/{imageId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getImage(@PathVariable Long imageId) {
        try {
            Optional<ImageResizeQueue> imageOpt = imageResizeQueueRepository.findById(imageId);
            
            if (imageOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Imagem não encontrada");
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("image", imageOpt.get());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao buscar imagem: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Obtém estatísticas da fila de imagens
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getImageQueueStatistics() {
        try {
            long totalImages = imageResizeQueueRepository.count();
            long pendingImages = imageResizeQueueRepository.countByStatus(ImageResizeQueue.ProcessingStatus.PENDING);
            long processingImages = imageResizeQueueRepository.countByStatus(ImageResizeQueue.ProcessingStatus.PROCESSING);
            long completedImages = imageResizeQueueRepository.countByStatus(ImageResizeQueue.ProcessingStatus.COMPLETED);
            long failedImages = imageResizeQueueRepository.countByStatus(ImageResizeQueue.ProcessingStatus.FAILED);
            
            long highPriorityImages = imageResizeQueueRepository.countByPriority(ImageResizeQueue.Priority.HIGH);
            long normalPriorityImages = imageResizeQueueRepository.countByPriority(ImageResizeQueue.Priority.NORMAL);
            long lowPriorityImages = imageResizeQueueRepository.countByPriority(ImageResizeQueue.Priority.LOW);
            
            Map<String, Object> priorityStats = new HashMap<>();
            priorityStats.put("high", highPriorityImages);
            priorityStats.put("normal", normalPriorityImages);
            priorityStats.put("low", lowPriorityImages);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", Map.of(
                "total", totalImages,
                "pending", pendingImages,
                "processing", processingImages,
                "completed", completedImages,
                "failed", failedImages,
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
     * Remove imagens antigas da fila
     */
    @DeleteMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cleanupOldImages(
            @RequestParam(defaultValue = "30") int daysOld) {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
            List<ImageResizeQueue> oldImages = imageResizeQueueRepository.findOldCompletedImages(cutoffDate);
            imageResizeQueueRepository.deleteAll(oldImages);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Limpeza concluída");
            response.put("deletedCount", oldImages.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro na limpeza: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Reprocessa imagens falhadas
     */
    @PostMapping("/retry-failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> retryFailedImages() {
        try {
            List<ImageResizeQueue> failedImages = imageResizeQueueRepository.findByStatus(ImageResizeQueue.ProcessingStatus.FAILED);
            for (ImageResizeQueue image : failedImages) {
                image.setStatus(ImageResizeQueue.ProcessingStatus.PENDING);
                image.setAttempts(0);
                image.setErrorMessage(null);
            }
            imageResizeQueueRepository.saveAll(failedImages);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Imagens falhadas reprocessadas");
            response.put("retriedCount", failedImages.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao reprocessar imagens: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Cancela uma imagem pendente
     */
    @PostMapping("/{imageId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cancelImage(@PathVariable Long imageId) {
        try {
            Optional<ImageResizeQueue> imageOpt = imageResizeQueueRepository.findById(imageId);
            
            if (imageOpt.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Imagem não encontrada");
                return ResponseEntity.notFound().build();
            }
            
            ImageResizeQueue image = imageOpt.get();
            if (image.getStatus() != ImageResizeQueue.ProcessingStatus.PENDING) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Apenas imagens pendentes podem ser canceladas");
                return ResponseEntity.badRequest().body(response);
            }
            
            image.setStatus(ImageResizeQueue.ProcessingStatus.FAILED);
            image.setErrorMessage("Cancelado pelo usuário");
            imageResizeQueueRepository.save(image);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Imagem cancelada com sucesso");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao cancelar imagem: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Desbloqueia imagens travadas
     */
    @PostMapping("/unlock-stuck")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> unlockStuckImages(
            @RequestParam(defaultValue = "60") int minutesStuck) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(minutesStuck);
            List<ImageResizeQueue> stuckImages = imageResizeQueueRepository.findStuckImages(cutoffTime);
            
            for (ImageResizeQueue image : stuckImages) {
                image.setStatus(ImageResizeQueue.ProcessingStatus.PENDING);
            }
            imageResizeQueueRepository.saveAll(stuckImages);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Imagens desbloqueadas");
            response.put("unlockedCount", stuckImages.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao desbloquear imagens: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lista modos de redimensionamento disponíveis
     */
    @GetMapping("/resize-modes")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getResizeModes() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("resizeModes", ImageResizeQueue.ResizeMode.values());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erro ao obter modos de redimensionamento: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}