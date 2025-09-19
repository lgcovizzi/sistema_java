package com.sistema.integration;

import com.sistema.entity.ImageResizeQueue;
import com.sistema.repository.ImageResizeQueueRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para o sistema de filas de redimensionamento de imagens.
 * 
 * Testa:
 * - Integração completa entre Controller, Service e Repository
 * - Persistência no banco de dados H2
 * - Endpoints da API REST
 * - Validações de segurança
 * - Transações e rollback
 * - Cenários de erro e recuperação
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_integration",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "logging.level.org.springframework.web=DEBUG"
})
@Transactional
@DisplayName("ImageResizeQueue - Testes de Integração")
class ImageResizeQueueIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ImageResizeQueueRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    private ImageResizeQueue sampleQueue;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        
        sampleQueue = new ImageResizeQueue();
        sampleQueue.setOriginalPath("/uploads/test-image.jpg");
        sampleQueue.setTargetPath("/resized/test-image-thumb.jpg");
        sampleQueue.setTargetWidth(150);
        sampleQueue.setTargetHeight(150);
        sampleQueue.setFormat("JPEG");
        sampleQueue.setQuality(85.0f);
        sampleQueue.setStatus(ImageResizeQueue.ProcessingStatus.PENDING);
        sampleQueue.setPriority(ImageResizeQueue.Priority.NORMAL);
        sampleQueue.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("Endpoints da API REST")
    class RestApiEndpoints {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve listar todas as filas com paginação")
        void shouldListAllQueuesWithPagination() throws Exception {
            // Given
            repository.save(sampleQueue);

            // When & Then
            mockMvc.perform(get("/api/image-resize-queue")
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].originalPath").value("/uploads/test-image.jpg"))
                    .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.size").value(10));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve buscar fila por ID")
        void shouldFindQueueById() throws Exception {
            // Given
            ImageResizeQueue saved = repository.save(sampleQueue);

            // When & Then
            mockMvc.perform(get("/api/image-resize-queue/{id}", saved.getId())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(saved.getId()))
                    .andExpect(jsonPath("$.originalPath").value("/uploads/test-image.jpg"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar 404 para ID inexistente")
        void shouldReturn404ForNonExistentId() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/image-resize-queue/{id}", 999L)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve criar nova fila")
        void shouldCreateNewQueue() throws Exception {
            // Given
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("originalPath", "/uploads/new-image.jpg");
            requestBody.put("targetPath", "/resized/new-image-thumb.jpg");
            requestBody.put("targetWidth", 200);
            requestBody.put("targetHeight", 200);
            requestBody.put("targetFormat", "PNG");
            requestBody.put("quality", 90);
            requestBody.put("priority", "HIGH");

            // When & Then
            mockMvc.perform(post("/api/image-resize-queue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.originalPath").value("/uploads/new-image.jpg"))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.priority").value("HIGH"));

            // Verificar persistência
            List<ImageResizeQueue> queues = repository.findAll();
            assertThat(queues).hasSize(1);
            assertThat(queues.get(0).getOriginalPath()).isEqualTo("/uploads/new-image.jpg");
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve atualizar status da fila")
        void shouldUpdateQueueStatus() throws Exception {
            // Given
            ImageResizeQueue saved = repository.save(sampleQueue);
            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("status", "PROCESSING");

            // When & Then
            mockMvc.perform(put("/api/image-resize-queue/{id}/status", saved.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PROCESSING"));

            // Verificar persistência
            ImageResizeQueue updated = repository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(ImageResizeQueue.ProcessingStatus.PROCESSING);
            assertThat(updated.getStartedAt()).isNotNull();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve deletar fila")
        void shouldDeleteQueue() throws Exception {
            // Given
            ImageResizeQueue saved = repository.save(sampleQueue);

            // When & Then
            mockMvc.perform(delete("/api/image-resize-queue/{id}", saved.getId())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            // Verificar remoção
            assertThat(repository.findById(saved.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Consultas por Status")
    class StatusQueries {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve buscar filas por status")
        void shouldFindQueuesByStatus() throws Exception {
            // Given
            sampleQueue.setStatus(ImageResizeQueue.ProcessingStatus.PENDING);
            repository.save(sampleQueue);

            ImageResizeQueue processingQueue = new ImageResizeQueue();
            processingQueue.setOriginalPath("/uploads/processing.jpg");
            processingQueue.setTargetPath("/resized/processing-thumb.jpg");
            processingQueue.setTargetWidth(100);
            processingQueue.setTargetHeight(100);
            processingQueue.setStatus(ImageResizeQueue.ProcessingStatus.PROCESSING);
            processingQueue.setPriority(ImageResizeQueue.Priority.LOW);
            processingQueue.setCreatedAt(LocalDateTime.now());
            repository.save(processingQueue);

            // When & Then
            mockMvc.perform(get("/api/image-resize-queue/status/{status}", "PENDING")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].status").value("PENDING"))
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve buscar filas por prioridade")
        void shouldFindQueuesByPriority() throws Exception {
            // Given
            sampleQueue.setPriority(ImageResizeQueue.Priority.HIGH);
            repository.save(sampleQueue);

            // When & Then
            mockMvc.perform(get("/api/image-resize-queue/priority/{priority}", "HIGH")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].priority").value("HIGH"));
        }
    }

    @Nested
    @DisplayName("Estatísticas e Métricas")
    class StatisticsAndMetrics {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar estatísticas das filas")
        void shouldReturnQueueStatistics() throws Exception {
            // Given
            repository.save(sampleQueue);

            ImageResizeQueue completedQueue = new ImageResizeQueue();
            completedQueue.setOriginalPath("/uploads/completed.jpg");
            completedQueue.setTargetPath("/resized/completed-thumb.jpg");
            completedQueue.setTargetWidth(100);
            completedQueue.setTargetHeight(100);
            completedQueue.setStatus(ImageResizeQueue.ProcessingStatus.COMPLETED);
            completedQueue.setPriority(ImageResizeQueue.Priority.LOW);
            completedQueue.setCreatedAt(LocalDateTime.now());
            repository.save(completedQueue);

            // When & Then
            mockMvc.perform(get("/api/image-resize-queue/statistics")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(2))
                    .andExpect(jsonPath("$.pending").value(1))
                    .andExpect(jsonPath("$.completed").value(1));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar métricas de performance")
        void shouldReturnPerformanceMetrics() throws Exception {
            // Given
            sampleQueue.setStartedAt(LocalDateTime.now().minusMinutes(5));
            sampleQueue.setCompletedAt(LocalDateTime.now());
            sampleQueue.setStatus(ImageResizeQueue.ProcessingStatus.COMPLETED);
            repository.save(sampleQueue);

            // When & Then
            mockMvc.perform(get("/api/image-resize-queue/metrics")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.averageProcessingTime").exists())
                    .andExpect(jsonPath("$.totalProcessed").value(1));
        }
    }

    @Nested
    @DisplayName("Validações de Segurança")
    class SecurityValidations {

        @Test
        @DisplayName("Deve negar acesso sem autenticação")
        void shouldDenyAccessWithoutAuthentication() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/image-resize-queue")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Deve negar acesso com role insuficiente")
        void shouldDenyAccessWithInsufficientRole() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/image-resize-queue")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve permitir acesso com role adequada")
        void shouldAllowAccessWithAdequateRole() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/image-resize-queue")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Validações de Entrada")
    class InputValidations {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve validar campos obrigatórios na criação")
        void shouldValidateRequiredFieldsOnCreation() throws Exception {
            // Given
            Map<String, Object> invalidRequest = new HashMap<>();
            invalidRequest.put("targetWidth", 100);
            // originalPath ausente

            // When & Then
            mockMvc.perform(post("/api/image-resize-queue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve validar valores de dimensões")
        void shouldValidateDimensionValues() throws Exception {
            // Given
            Map<String, Object> invalidRequest = new HashMap<>();
            invalidRequest.put("originalPath", "/uploads/test.jpg");
            invalidRequest.put("targetPath", "/resized/test-thumb.jpg");
            invalidRequest.put("targetWidth", -100); // Valor inválido
            invalidRequest.put("targetHeight", 100);

            // When & Then
            mockMvc.perform(post("/api/image-resize-queue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve validar formato de qualidade")
        void shouldValidateQualityFormat() throws Exception {
            // Given
            Map<String, Object> invalidRequest = new HashMap<>();
            invalidRequest.put("originalPath", "/uploads/test.jpg");
            invalidRequest.put("targetPath", "/resized/test-thumb.jpg");
            invalidRequest.put("targetWidth", 100);
            invalidRequest.put("targetHeight", 100);
            invalidRequest.put("quality", 150); // Valor inválido (> 100)

            // When & Then
            mockMvc.perform(post("/api/image-resize-queue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Transações e Rollback")
    class TransactionsAndRollback {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve fazer rollback em caso de erro")
        void shouldRollbackOnError() throws Exception {
            // Given
            long initialCount = repository.count();

            // Simular erro através de dados inválidos que causem exceção
            Map<String, Object> invalidRequest = new HashMap<>();
            invalidRequest.put("originalPath", null); // Causa erro de validação
            invalidRequest.put("targetWidth", 100);

            // When & Then
            mockMvc.perform(post("/api/image-resize-queue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            // Verificar que não houve persistência
            assertThat(repository.count()).isEqualTo(initialCount);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve manter consistência em operações múltiplas")
        void shouldMaintainConsistencyInMultipleOperations() throws Exception {
            // Given
            ImageResizeQueue saved = repository.save(sampleQueue);
            long initialCount = repository.count();

            // Atualizar status
            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("status", "PROCESSING");

            // When & Then
            mockMvc.perform(put("/api/image-resize-queue/{id}/status", saved.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk());

            // Verificar consistência
            assertThat(repository.count()).isEqualTo(initialCount);
            ImageResizeQueue updated = repository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(ImageResizeQueue.ProcessingStatus.PROCESSING);
        }
    }

    @Nested
    @DisplayName("Cenários de Erro e Recuperação")
    class ErrorAndRecoveryScenarios {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve lidar com tentativas de processamento falhadas")
        void shouldHandleFailedProcessingAttempts() throws Exception {
            // Given
            sampleQueue.setStatus(ImageResizeQueue.ProcessingStatus.FAILED);
            sampleQueue.setAttempts(3);
            ImageResizeQueue saved = repository.save(sampleQueue);

            // When & Then
            mockMvc.perform(get("/api/image-resize-queue/{id}", saved.getId())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FAILED"))
                    .andExpect(jsonPath("$.attempts").value(3));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve permitir reprocessamento de itens falhados")
        void shouldAllowReprocessingOfFailedItems() throws Exception {
            // Given
            sampleQueue.setStatus(ImageResizeQueue.ProcessingStatus.FAILED);
            sampleQueue.setAttempts(2);
            ImageResizeQueue saved = repository.save(sampleQueue);

            Map<String, Object> retryRequest = new HashMap<>();
            retryRequest.put("status", "PENDING");

            // When & Then
            mockMvc.perform(put("/api/image-resize-queue/{id}/status", saved.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(retryRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"));

            // Verificar que foi resetado para reprocessamento
            ImageResizeQueue updated = repository.findById(saved.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(ImageResizeQueue.ProcessingStatus.PENDING);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve buscar itens que precisam de reprocessamento")
        void shouldFindItemsNeedingReprocessing() throws Exception {
            // Given
            sampleQueue.setStatus(ImageResizeQueue.ProcessingStatus.FAILED);
            sampleQueue.setAttempts(2);
            repository.save(sampleQueue);

            // When & Then
            mockMvc.perform(get("/api/image-resize-queue/failed")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].status").value("FAILED"))
                    .andExpect(jsonPath("$[0].attempts").value(2));
        }
    }

    @Nested
    @DisplayName("Performance e Paginação")
    class PerformanceAndPagination {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve paginar resultados corretamente")
        void shouldPaginateResultsCorrectly() throws Exception {
            // Given - Criar múltiplos registros
            for (int i = 0; i < 25; i++) {
                ImageResizeQueue queue = new ImageResizeQueue();
                queue.setOriginalPath("/uploads/test-" + i + ".jpg");
                queue.setTargetPath("/resized/test-" + i + "-thumb.jpg");
                queue.setTargetWidth(100);
                queue.setTargetHeight(100);
                queue.setStatus(ImageResizeQueue.ProcessingStatus.PENDING);
                queue.setPriority(ImageResizeQueue.Priority.HIGH);
                queue.setCreatedAt(LocalDateTime.now());
                repository.save(queue);
            }

            // When & Then - Primeira página
            mockMvc.perform(get("/api/image-resize-queue")
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(10))
                    .andExpect(jsonPath("$.totalElements").value(25))
                    .andExpect(jsonPath("$.totalPages").value(3))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(false));

            // When & Then - Última página
            mockMvc.perform(get("/api/image-resize-queue")
                    .param("page", "2")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(5))
                    .andExpect(jsonPath("$.first").value(false))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve ordenar resultados por data de criação")
        void shouldSortResultsByCreationDate() throws Exception {
            // Given
            LocalDateTime now = LocalDateTime.now();
            
            ImageResizeQueue older = new ImageResizeQueue();
            older.setOriginalPath("/uploads/older.jpg");
            older.setTargetPath("/resized/older-thumb.jpg");
            older.setTargetWidth(100);
            older.setTargetHeight(100);
            older.setStatus(ImageResizeQueue.ProcessingStatus.PENDING);
            older.setPriority(ImageResizeQueue.Priority.HIGH);
            older.setCreatedAt(now.minusHours(1));
            repository.save(older);

            ImageResizeQueue newer = new ImageResizeQueue();
            newer.setOriginalPath("/uploads/newer.jpg");
            newer.setTargetPath("/resized/newer-thumb.jpg");
            newer.setTargetWidth(100);
            newer.setTargetHeight(100);
            newer.setStatus(ImageResizeQueue.ProcessingStatus.PENDING);
            newer.setPriority(ImageResizeQueue.Priority.HIGH);
            newer.setCreatedAt(now);
            repository.save(newer);

            // When & Then
            mockMvc.perform(get("/api/image-resize-queue")
                    .param("sort", "createdAt,desc")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].originalPath").value("/uploads/newer.jpg"))
                    .andExpect(jsonPath("$.content[1].originalPath").value("/uploads/older.jpg"));
        }
    }
}