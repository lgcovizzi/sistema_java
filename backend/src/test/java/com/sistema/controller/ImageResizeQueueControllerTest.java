package com.sistema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.entity.ImageResizeQueue;
import com.sistema.repository.ImageResizeQueueRepository;
import com.sistema.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes unitários para o controller ImageResizeQueueController.
 * 
 * Testa:
 * - Endpoints de listagem e paginação
 * - Endpoints de criação de tarefas
 * - Endpoints de consulta por status
 * - Endpoints de estatísticas
 * - Autenticação e autorização
 * - Validação de entrada
 * - Tratamento de erros
 */
@WebMvcTest(ImageResizeQueueController.class)
@ActiveProfiles("test")
@DisplayName("ImageResizeQueueController - Testes Unitários")
class ImageResizeQueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ImageResizeQueueRepository repository;

    @MockBean
    private JwtService jwtService;

    private ImageResizeQueue sampleQueue;
    private List<ImageResizeQueue> sampleQueueList;

    @BeforeEach
    void setUp() {
        sampleQueue = createSampleImageResizeQueue();
        sampleQueueList = Arrays.asList(
            sampleQueue,
            createImageResizeQueue(2L, ImageResizeQueue.ProcessingStatus.PROCESSING),
            createImageResizeQueue(3L, ImageResizeQueue.ProcessingStatus.COMPLETED)
        );
    }

    private ImageResizeQueue createSampleImageResizeQueue() {
        return createImageResizeQueue(1L, ImageResizeQueue.ProcessingStatus.PENDING);
    }

    private ImageResizeQueue createImageResizeQueue(Long id, ImageResizeQueue.ProcessingStatus status) {
        ImageResizeQueue queue = new ImageResizeQueue();
        queue.setId(id);
        queue.setStatus(status);
        queue.setPriority(ImageResizeQueue.Priority.NORMAL);
        queue.setOriginalPath("/path/original" + id + ".jpg");
        queue.setTargetPath("/path/target" + id + ".jpg");
        queue.setOriginalWidth(1920);
        queue.setOriginalHeight(1080);
        queue.setTargetWidth(800);
        queue.setTargetHeight(600);
        queue.setQuality(0.8f);
        queue.setFormat("JPEG");
        queue.setResizeMode(ImageResizeQueue.ResizeMode.SCALE_TO_FIT);
        queue.setCreatedAt(LocalDateTime.now());
        queue.setAttempts(0);
        queue.setMaxAttempts(3);
        queue.setPreserveMetadata(false);
        return queue;
    }

    @Nested
    @DisplayName("Endpoints de Listagem")
    class ListingEndpoints {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve listar todas as entradas da fila")
        void shouldListAllQueueEntries() throws Exception {
            // Given
            when(repository.findAll()).thenReturn(sampleQueueList);

            // When & Then
            mockMvc.perform(get("/api/image-resize-queue")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].status").value("PENDING"))
                    .andExpect(jsonPath("$[1].status").value("PROCESSING"))
                    .andExpect(jsonPath("$[2].status").value("COMPLETED"));

            verify(repository).findAll();
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve listar entradas com paginação")
        void shouldListEntriesWithPagination() throws Exception {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<ImageResizeQueue> page = new PageImpl<>(sampleQueueList, pageable, sampleQueueList.size());
            when(repository.findAll(any(Pageable.class))).thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/image-resize-queue/page")
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(3))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.number").value(0));

            verify(repository).findAll(any(Pageable.class));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Deve negar acesso para usuário comum")
        void shouldDenyAccessForRegularUser() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/image-resize-queue")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());

            verify(repository, never()).findAll();
        }
    }

    @Nested
    @DisplayName("Endpoints de Consulta por ID")
    class GetByIdEndpoints {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar entrada por ID")
        void shouldReturnEntryById() throws Exception {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(sampleQueue));

            // When & Then
            mockMvc.perform(get("/api/image-resize-queue/1")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.originalPath").value("/path/original1.jpg"))
                    .andExpect(jsonPath("$.targetPath").value("/path/target1.jpg"));

            verify(repository).findById(1L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar 404 para ID não encontrado")
        void shouldReturn404ForNotFoundId() throws Exception {
            // Given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/image-resize-queue/999")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(repository).findById(999L);
        }
    }

    @Nested
    @DisplayName("Endpoints de Consulta por Status")
    class StatusQueryEndpoints {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar entradas por status PENDING")
        void shouldReturnEntriesByPendingStatus() throws Exception {
            // Given
            List<ImageResizeQueue> pendingEntries = Arrays.asList(sampleQueue);
            when(repository.findByStatus(ImageResizeQueue.ProcessingStatus.PENDING))
                .thenReturn(pendingEntries);

            // When & Then
            mockMvc.perform(get("/api/image-resize-queue/status/PENDING")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].status").value("PENDING"));

            verify(repository).findByStatus(ImageResizeQueue.ProcessingStatus.PENDING);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar entradas por status PROCESSING")
        void shouldReturnEntriesByProcessingStatus() throws Exception {
            // Given
            List<ImageResizeQueue> processingEntries = Arrays.asList(
                createImageResizeQueue(2L, ImageResizeQueue.ProcessingStatus.PROCESSING)
            );
            when(repository.findByStatus(ImageResizeQueue.ProcessingStatus.PROCESSING))
                .thenReturn(processingEntries);

            // When & Then
            mockMvc.perform(get("/api/image-resize-queue/status/PROCESSING")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].status").value("PROCESSING"));

            verify(repository).findByStatus(ImageResizeQueue.ProcessingStatus.PROCESSING);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar erro para status inválido")
        void shouldReturnErrorForInvalidStatus() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/image-resize-queue/status/INVALID_STATUS")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verify(repository, never()).findByStatus(any());
        }
    }

    @Nested
    @DisplayName("Endpoints de Estatísticas")
    class StatisticsEndpoints {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar contadores por status")
        void shouldReturnCountersByStatus() throws Exception {
            // Given
            when(repository.countByStatus(ImageResizeQueue.ProcessingStatus.PENDING)).thenReturn(5L);
            when(repository.countByStatus(ImageResizeQueue.ProcessingStatus.PROCESSING)).thenReturn(2L);
            when(repository.countByStatus(ImageResizeQueue.ProcessingStatus.COMPLETED)).thenReturn(10L);
            when(repository.countByStatus(ImageResizeQueue.ProcessingStatus.FAILED)).thenReturn(1L);

            // When & Then
            mockMvc.perform(get("/api/image-resize-queue/statistics/status")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.PENDING").value(5))
                    .andExpect(jsonPath("$.PROCESSING").value(2))
                    .andExpect(jsonPath("$.COMPLETED").value(10))
                    .andExpect(jsonPath("$.FAILED").value(1));

            verify(repository).countByStatus(ImageResizeQueue.ProcessingStatus.PENDING);
            verify(repository).countByStatus(ImageResizeQueue.ProcessingStatus.PROCESSING);
            verify(repository).countByStatus(ImageResizeQueue.ProcessingStatus.COMPLETED);
            verify(repository).countByStatus(ImageResizeQueue.ProcessingStatus.FAILED);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar contadores por prioridade")
        void shouldReturnCountersByPriority() throws Exception {
            // Given
            when(repository.countByPriority(ImageResizeQueue.Priority.LOW)).thenReturn(3L);
            when(repository.countByPriority(ImageResizeQueue.Priority.NORMAL)).thenReturn(8L);
            when(repository.countByPriority(ImageResizeQueue.Priority.HIGH)).thenReturn(4L);
            when(repository.countByPriority(ImageResizeQueue.Priority.URGENT)).thenReturn(1L);

            // When & Then
            mockMvc.perform(get("/api/image-resize-queue/statistics/priority")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.LOW").value(3))
                    .andExpect(jsonPath("$.NORMAL").value(8))
                    .andExpect(jsonPath("$.HIGH").value(4))
                    .andExpect(jsonPath("$.URGENT").value(1));

            verify(repository).countByPriority(ImageResizeQueue.Priority.LOW);
            verify(repository).countByPriority(ImageResizeQueue.Priority.NORMAL);
            verify(repository).countByPriority(ImageResizeQueue.Priority.HIGH);
            verify(repository).countByPriority(ImageResizeQueue.Priority.URGENT);
        }
    }

    @Nested
    @DisplayName("Endpoints de Criação")
    class CreationEndpoints {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve criar nova entrada na fila")
        void shouldCreateNewQueueEntry() throws Exception {
            // Given
            ImageResizeQueue newQueue = createSampleImageResizeQueue();
            newQueue.setId(null); // Nova entrada não tem ID
            
            ImageResizeQueue savedQueue = createSampleImageResizeQueue();
            savedQueue.setId(1L); // Entrada salva tem ID
            
            when(repository.save(any(ImageResizeQueue.class))).thenReturn(savedQueue);

            String requestBody = objectMapper.writeValueAsString(newQueue);

            // When & Then
            mockMvc.perform(post("/api/image-resize-queue")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.originalPath").value("/path/original1.jpg"));

            verify(repository).save(any(ImageResizeQueue.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve validar campos obrigatórios na criação")
        void shouldValidateRequiredFieldsOnCreation() throws Exception {
            // Given
            ImageResizeQueue invalidQueue = new ImageResizeQueue();
            // Não definir campos obrigatórios
            
            String requestBody = objectMapper.writeValueAsString(invalidQueue);

            // When & Then
            mockMvc.perform(post("/api/image-resize-queue")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest());

            verify(repository, never()).save(any(ImageResizeQueue.class));
        }
    }

    @Nested
    @DisplayName("Endpoints de Atualização")
    class UpdateEndpoints {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve atualizar entrada existente")
        void shouldUpdateExistingEntry() throws Exception {
            // Given
            ImageResizeQueue updatedQueue = createSampleImageResizeQueue();
            updatedQueue.setStatus(ImageResizeQueue.ProcessingStatus.PROCESSING);
            updatedQueue.setStartedAt(LocalDateTime.now());
            
            when(repository.findById(1L)).thenReturn(Optional.of(sampleQueue));
            when(repository.save(any(ImageResizeQueue.class))).thenReturn(updatedQueue);

            String requestBody = objectMapper.writeValueAsString(updatedQueue);

            // When & Then
            mockMvc.perform(put("/api/image-resize-queue/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("PROCESSING"));

            verify(repository).findById(1L);
            verify(repository).save(any(ImageResizeQueue.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar 404 ao atualizar entrada inexistente")
        void shouldReturn404WhenUpdatingNonExistentEntry() throws Exception {
            // Given
            when(repository.findById(999L)).thenReturn(Optional.empty());
            
            ImageResizeQueue updateData = createSampleImageResizeQueue();
            String requestBody = objectMapper.writeValueAsString(updateData);

            // When & Then
            mockMvc.perform(put("/api/image-resize-queue/999")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isNotFound());

            verify(repository).findById(999L);
            verify(repository, never()).save(any(ImageResizeQueue.class));
        }
    }

    @Nested
    @DisplayName("Endpoints de Exclusão")
    class DeletionEndpoints {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve deletar entrada existente")
        void shouldDeleteExistingEntry() throws Exception {
            // Given
            when(repository.findById(1L)).thenReturn(Optional.of(sampleQueue));
            doNothing().when(repository).deleteById(1L);

            // When & Then
            mockMvc.perform(delete("/api/image-resize-queue/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(repository).findById(1L);
            verify(repository).deleteById(1L);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve retornar 404 ao deletar entrada inexistente")
        void shouldReturn404WhenDeletingNonExistentEntry() throws Exception {
            // Given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(delete("/api/image-resize-queue/999")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(repository).findById(999L);
            verify(repository, never()).deleteById(999L);
        }
    }

    @Nested
    @DisplayName("Endpoints de Processamento")
    class ProcessingEndpoints {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve obter próximas entradas para processamento")
        void shouldGetNextEntriesForProcessing() throws Exception {
            // Given
            List<ImageResizeQueue> nextEntries = Arrays.asList(sampleQueue);
            Page<ImageResizeQueue> page = new PageImpl<>(nextEntries);
            
            when(repository.findReadyToProcess(any(Pageable.class)))
                .thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/image-resize-queue/next")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].status").value("PENDING"));

            verify(repository).findTopPendingImages(any(Pageable.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve marcar entrada como processando")
        void shouldMarkEntryAsProcessing() throws Exception {
            // Given
            ImageResizeQueue processingQueue = createSampleImageResizeQueue();
            processingQueue.setStatus(ImageResizeQueue.ProcessingStatus.PROCESSING);
            processingQueue.setStartedAt(LocalDateTime.now());
            
            when(repository.findById(1L)).thenReturn(Optional.of(sampleQueue));
            when(repository.save(any(ImageResizeQueue.class))).thenReturn(processingQueue);

            // When & Then
            mockMvc.perform(patch("/api/image-resize-queue/1/start")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("PROCESSING"))
                    .andExpect(jsonPath("$.startedAt").exists());

            verify(repository).findById(1L);
            verify(repository).save(any(ImageResizeQueue.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Deve marcar entrada como completada")
        void shouldMarkEntryAsCompleted() throws Exception {
            // Given
            ImageResizeQueue completedQueue = createSampleImageResizeQueue();
            completedQueue.setStatus(ImageResizeQueue.ProcessingStatus.COMPLETED);
            completedQueue.setCompletedAt(LocalDateTime.now());
            
            when(repository.findById(1L)).thenReturn(Optional.of(sampleQueue));
            when(repository.save(any(ImageResizeQueue.class))).thenReturn(completedQueue);

            // When & Then
            mockMvc.perform(patch("/api/image-resize-queue/1/complete")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.completedAt").exists());

            verify(repository).findById(1L);
            verify(repository).save(any(ImageResizeQueue.class));
        }
    }

    @Nested
    @DisplayName("Validação de Segurança")
    class SecurityValidation {

        @Test
        @DisplayName("Deve exigir autenticação para todos os endpoints")
        void shouldRequireAuthenticationForAllEndpoints() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/image-resize-queue"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(post("/api/image-resize-queue")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(put("/api/image-resize-queue/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(delete("/api/image-resize-queue/1")
                    .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Deve negar acesso para usuários sem permissão ADMIN")
        void shouldDenyAccessForUsersWithoutAdminPermission() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/image-resize-queue"))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/image-resize-queue")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }
}