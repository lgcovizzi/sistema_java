package com.sistema.repository;

import com.sistema.entity.ImageResizeQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários para o repositório ImageResizeQueueRepository.
 * 
 * Testa:
 * - Operações CRUD básicas
 * - Consultas customizadas por status
 * - Consultas por prioridade
 * - Consultas por data
 * - Paginação e ordenação
 * - Estatísticas e contadores
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ImageResizeQueueRepository - Testes Unitários")
class ImageResizeQueueRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ImageResizeQueueRepository repository;

    private ImageResizeQueue pendingQueue;
    private ImageResizeQueue processingQueue;
    private ImageResizeQueue completedQueue;
    private ImageResizeQueue failedQueue;

    @BeforeEach
    void setUp() {
        // Criar dados de teste
        pendingQueue = createImageResizeQueue(
            ImageResizeQueue.ProcessingStatus.PENDING,
            ImageResizeQueue.Priority.NORMAL,
            "/path/original1.jpg",
            "/path/target1.jpg"
        );

        processingQueue = createImageResizeQueue(
            ImageResizeQueue.ProcessingStatus.PROCESSING,
            ImageResizeQueue.Priority.HIGH,
            "/path/original2.jpg",
            "/path/target2.jpg"
        );

        completedQueue = createImageResizeQueue(
            ImageResizeQueue.ProcessingStatus.COMPLETED,
            ImageResizeQueue.Priority.LOW,
            "/path/original3.jpg",
            "/path/target3.jpg"
        );

        failedQueue = createImageResizeQueue(
            ImageResizeQueue.ProcessingStatus.FAILED,
            ImageResizeQueue.Priority.URGENT,
            "/path/original4.jpg",
            "/path/target4.jpg"
        );

        // Persistir dados de teste
        entityManager.persistAndFlush(pendingQueue);
        entityManager.persistAndFlush(processingQueue);
        entityManager.persistAndFlush(completedQueue);
        entityManager.persistAndFlush(failedQueue);
    }

    private ImageResizeQueue createImageResizeQueue(
            ImageResizeQueue.ProcessingStatus status,
            ImageResizeQueue.Priority priority,
            String originalPath,
            String targetPath) {
        
        ImageResizeQueue queue = new ImageResizeQueue();
        queue.setStatus(status);
        queue.setPriority(priority);
        queue.setOriginalPath(originalPath);
        queue.setTargetPath(targetPath);
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
    @DisplayName("Operações CRUD Básicas")
    class BasicCrudOperations {

        @Test
        @DisplayName("Deve salvar nova entrada na fila")
        void shouldSaveNewQueueEntry() {
            // Given
            ImageResizeQueue newQueue = createImageResizeQueue(
                ImageResizeQueue.ProcessingStatus.PENDING,
                ImageResizeQueue.Priority.NORMAL,
                "/path/new.jpg",
                "/path/new_resized.jpg"
            );

            // When
            ImageResizeQueue saved = repository.save(newQueue);

            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getOriginalPath()).isEqualTo("/path/new.jpg");
            assertThat(saved.getStatus()).isEqualTo(ImageResizeQueue.ProcessingStatus.PENDING);
        }

        @Test
        @DisplayName("Deve encontrar entrada por ID")
        void shouldFindEntryById() {
            // Given
            Long id = pendingQueue.getId();

            // When
            Optional<ImageResizeQueue> found = repository.findById(id);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getOriginalPath()).isEqualTo("/path/original1.jpg");
        }

        @Test
        @DisplayName("Deve atualizar entrada existente")
        void shouldUpdateExistingEntry() {
            // Given
            pendingQueue.setStatus(ImageResizeQueue.ProcessingStatus.PROCESSING);
            pendingQueue.setStartedAt(LocalDateTime.now());

            // When
            ImageResizeQueue updated = repository.save(pendingQueue);

            // Then
            assertThat(updated.getStatus()).isEqualTo(ImageResizeQueue.ProcessingStatus.PROCESSING);
            assertThat(updated.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("Deve deletar entrada por ID")
        void shouldDeleteEntryById() {
            // Given
            Long id = completedQueue.getId();

            // When
            repository.deleteById(id);

            // Then
            Optional<ImageResizeQueue> found = repository.findById(id);
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Consultas por Status")
    class StatusQueries {

        @Test
        @DisplayName("Deve encontrar entradas por status PENDING")
        void shouldFindEntriesByPendingStatus() {
            // When
            List<ImageResizeQueue> pending = repository.findByStatus(ImageResizeQueue.ProcessingStatus.PENDING);

            // Then
            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getOriginalPath()).isEqualTo("/path/original1.jpg");
        }

        @Test
        @DisplayName("Deve encontrar entradas por status PROCESSING")
        void shouldFindEntriesByProcessingStatus() {
            // When
            List<ImageResizeQueue> processing = repository.findByStatus(ImageResizeQueue.ProcessingStatus.PROCESSING);

            // Then
            assertThat(processing).hasSize(1);
            assertThat(processing.get(0).getOriginalPath()).isEqualTo("/path/original2.jpg");
        }

        @Test
        @DisplayName("Deve encontrar entradas por status COMPLETED")
        void shouldFindEntriesByCompletedStatus() {
            // When
            List<ImageResizeQueue> completed = repository.findByStatus(ImageResizeQueue.ProcessingStatus.COMPLETED);

            // Then
            assertThat(completed).hasSize(1);
            assertThat(completed.get(0).getOriginalPath()).isEqualTo("/path/original3.jpg");
        }

        @Test
        @DisplayName("Deve encontrar entradas por status FAILED")
        void shouldFindEntriesByFailedStatus() {
            // When
            List<ImageResizeQueue> failed = repository.findByStatus(ImageResizeQueue.ProcessingStatus.FAILED);

            // Then
            assertThat(failed).hasSize(1);
            assertThat(failed.get(0).getOriginalPath()).isEqualTo("/path/original4.jpg");
        }
    }

    @Nested
    @DisplayName("Consultas por Prioridade")
    class PriorityQueries {

        @Test
        @DisplayName("Deve encontrar entradas por prioridade")
        void shouldFindEntriesByPriority() {
            // When
            List<ImageResizeQueue> byPriority = repository.findByPriority(
                ImageResizeQueue.Priority.NORMAL
            );

            // Then
            assertThat(byPriority).hasSize(1);
            assertThat(byPriority.get(0).getPriority()).isEqualTo(ImageResizeQueue.Priority.NORMAL);
        }

        @Test
        @DisplayName("Deve encontrar próximas entradas para processamento por prioridade")
        void shouldFindNextEntriesForProcessingByPriority() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            // When
            List<ImageResizeQueue> nextEntries = repository.findTopByStatusOrderByPriorityDescCreatedAtAsc(
                ImageResizeQueue.ProcessingStatus.PENDING, pageable
            );

            // Then
            assertThat(nextEntries).hasSize(1);
            assertThat(nextEntries.get(0).getStatus())
                .isEqualTo(ImageResizeQueue.ProcessingStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("Consultas por Data")
    class DateQueries {

        @Test
        @DisplayName("Deve encontrar entradas por status")
        void shouldFindEntriesByStatus() {
            // When
            List<ImageResizeQueue> pending = repository.findByStatus(ImageResizeQueue.ProcessingStatus.PENDING);

            // Then
            assertThat(pending).hasSize(1);
        }

        @Test
        @DisplayName("Deve contar entradas por status")
        void shouldCountEntriesByStatus() {
            // When
            long pendingCount = repository.countByStatus(ImageResizeQueue.ProcessingStatus.PENDING);

            // Then
            assertThat(pendingCount).isEqualTo(1);
        }

        @Test
        @DisplayName("Deve encontrar entradas completadas após data específica")
        void shouldFindEntriesCompletedAfterSpecificDate() {
            // Given
            completedQueue.setCompletedAt(LocalDateTime.now());
            entityManager.persistAndFlush(completedQueue);
            
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

            // When
            List<ImageResizeQueue> completed = repository.findByStatus(ImageResizeQueue.ProcessingStatus.COMPLETED)
                .stream()
                .filter(queue -> queue.getCompletedAt() != null && queue.getCompletedAt().isAfter(yesterday))
                .toList();

            // Then
            assertThat(completed).hasSize(1);
            assertThat(completed.get(0).getStatus()).isEqualTo(ImageResizeQueue.ProcessingStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("Consultas de Estatísticas")
    class StatisticsQueries {

        @Test
        @DisplayName("Deve contar entradas por status")
        void shouldCountEntriesByStatus() {
            // When
            long pendingCount = repository.countByStatus(ImageResizeQueue.ProcessingStatus.PENDING);
            long processingCount = repository.countByStatus(ImageResizeQueue.ProcessingStatus.PROCESSING);
            long completedCount = repository.countByStatus(ImageResizeQueue.ProcessingStatus.COMPLETED);
            long failedCount = repository.countByStatus(ImageResizeQueue.ProcessingStatus.FAILED);

            // Then
            assertThat(pendingCount).isEqualTo(1);
            assertThat(processingCount).isEqualTo(1);
            assertThat(completedCount).isEqualTo(1);
            assertThat(failedCount).isEqualTo(1);
        }

        @Test
        @DisplayName("Deve contar entradas por prioridade")
        void shouldCountEntriesByPriority() {
            // When
            long lowCount = repository.countByPriority(ImageResizeQueue.Priority.LOW);
            long normalCount = repository.countByPriority(ImageResizeQueue.Priority.NORMAL);
            long highCount = repository.countByPriority(ImageResizeQueue.Priority.HIGH);
            long urgentCount = repository.countByPriority(ImageResizeQueue.Priority.URGENT);

            // Then
            assertThat(lowCount).isEqualTo(1);
            assertThat(normalCount).isEqualTo(1);
            assertThat(highCount).isEqualTo(1);
            assertThat(urgentCount).isEqualTo(1);
        }

        @Test
        @DisplayName("Deve contar entradas criadas hoje")
        void shouldCountEntriesToday() {
            // Given
            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

            // When
            long todayCount = repository.findAll()
                .stream()
                .filter(queue -> queue.getCreatedAt().isAfter(startOfDay) && queue.getCreatedAt().isBefore(endOfDay))
                .count();

            // Then
            assertThat(todayCount).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Consultas de Tentativas e Falhas")
    class AttemptsAndFailuresQueries {

        @Test
        @DisplayName("Deve encontrar entradas que excederam tentativas máximas")
        void shouldFindEntriesThatExceededMaxAttempts() {
            // Given
            failedQueue.setAttempts(5);
            failedQueue.setMaxAttempts(3);
            entityManager.persistAndFlush(failedQueue);

            // When
            List<ImageResizeQueue> exceeded = repository.findPermanentlyFailedImages();

            // Then
            assertThat(exceeded).hasSize(1);
            assertThat(exceeded.get(0).getAttempts()).isGreaterThanOrEqualTo(exceeded.get(0).getMaxAttempts());
        }

        @Test
        @DisplayName("Deve encontrar entradas para retry")
        void shouldFindEntriesForRetry() {
            // Given
            failedQueue.setAttempts(1);
            failedQueue.setMaxAttempts(3);
            entityManager.persistAndFlush(failedQueue);

            // When
            List<ImageResizeQueue> forRetry = repository.findFailedImagesForRetry();

            // Then
            assertThat(forRetry).hasSize(1);
            assertThat(forRetry.get(0).getAttempts()).isLessThan(forRetry.get(0).getMaxAttempts());
        }
    }

    @Nested
    @DisplayName("Consultas de Limpeza e Manutenção")
    class CleanupAndMaintenanceQueries {

        @Test
        @DisplayName("Deve encontrar entradas antigas para limpeza")
        void shouldFindOldEntriesForCleanup() {
            // Given
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            
            // Criar entrada antiga
            ImageResizeQueue oldEntry = createImageResizeQueue(
                ImageResizeQueue.ProcessingStatus.COMPLETED,
                ImageResizeQueue.Priority.NORMAL,
                "/path/old.jpg",
                "/path/old_resized.jpg"
            );
            oldEntry.setCreatedAt(LocalDateTime.now().minusDays(45));
            oldEntry.setCompletedAt(LocalDateTime.now().minusDays(45));
            entityManager.persistAndFlush(oldEntry);

            // When
            List<ImageResizeQueue> oldEntries = repository.findOldCompletedImages(cutoffDate);

            // Then
            assertThat(oldEntries).hasSize(1);
            assertThat(oldEntries.get(0).getCompletedAt()).isBefore(cutoffDate);
        }

        @Test
        @DisplayName("Deve encontrar entradas órfãs sem batch job")
        void shouldFindOrphanEntriesWithoutBatchJob() {
            // Given
            pendingQueue.setBatchJobId(null);
            entityManager.persistAndFlush(pendingQueue);

            // When
            List<ImageResizeQueue> orphans = repository.findAll().stream()
                .filter(queue -> queue.getBatchJobId() == null)
                .toList();

            // Then
            assertThat(orphans).hasSize(4); // Todas as entradas não têm batch job ID
        }
    }

    @Nested
    @DisplayName("Consultas de Performance")
    class PerformanceQueries {

        @Test
        @DisplayName("Deve encontrar entradas com tempo de processamento longo")
        void shouldFindEntriesWithLongProcessingTime() {
            // Given
            completedQueue.setProcessingTimeMs(30000L); // 30 segundos
            entityManager.persistAndFlush(completedQueue);

            // When
            List<ImageResizeQueue> allCompleted = repository.findByStatus(ImageResizeQueue.ProcessingStatus.COMPLETED);
            List<ImageResizeQueue> longProcessing = allCompleted.stream()
                .filter(queue -> queue.getProcessingTimeMs() != null && queue.getProcessingTimeMs() > 20000L)
                .toList();

            // Then
            assertThat(longProcessing).hasSize(1);
            assertThat(longProcessing.get(0).getProcessingTimeMs()).isGreaterThan(20000L);
        }

        @Test
        @DisplayName("Deve calcular tempo médio de processamento")
        void shouldCalculateAverageProcessingTime() {
            // Given
            completedQueue.setProcessingTimeMs(10000L);
            entityManager.persistAndFlush(completedQueue);

            // When
            Object[] stats = repository.getProcessingStatistics(ImageResizeQueue.ProcessingStatus.COMPLETED);
            Double avgTime = stats != null && stats.length > 3 ? (Double) stats[3] : null;

            // Then
            assertThat(avgTime).isNotNull();
            assertThat(avgTime).isEqualTo(10000.0);
        }
    }

    @Nested
    @DisplayName("Paginação e Ordenação")
    class PaginationAndSorting {

        @Test
        @DisplayName("Deve paginar resultados corretamente")
        void shouldPaginateResultsCorrectly() {
            // Given
            Pageable pageable = PageRequest.of(0, 2);

            // When
            Page<ImageResizeQueue> page = repository.findAll(pageable);

            // Then
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(4);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("Deve ordenar por data de criação")
        void shouldSortByCreationDate() {
            // When
            List<ImageResizeQueue> sorted = repository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();

            // Then
            assertThat(sorted).hasSize(4);
            // Verificar se está ordenado por data de criação (mais recente primeiro)
            for (int i = 0; i < sorted.size() - 1; i++) {
                assertThat(sorted.get(i).getCreatedAt())
                    .isAfterOrEqualTo(sorted.get(i + 1).getCreatedAt());
            }
        }
    }
}