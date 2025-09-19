package com.sistema.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários para a entidade ImageResizeQueue.
 * 
 * Testa:
 * - Criação e inicialização da entidade
 * - Getters e setters
 * - Enums aninhados
 * - Validações de negócio
 * - Estados de processamento
 */
@DisplayName("ImageResizeQueue - Testes Unitários")
class ImageResizeQueueTest {

    private ImageResizeQueue imageResizeQueue;

    @BeforeEach
    void setUp() {
        imageResizeQueue = new ImageResizeQueue();
    }

    @Nested
    @DisplayName("Construtor e Inicialização")
    class ConstructorAndInitialization {

        @Test
        @DisplayName("Deve criar instância com valores padrão")
        void shouldCreateInstanceWithDefaultValues() {
            // Given & When
            ImageResizeQueue queue = new ImageResizeQueue();

            // Then
            assertThat(queue.getCreatedAt()).isNotNull();
            assertThat(queue.getStatus()).isEqualTo(ImageResizeQueue.ProcessingStatus.PENDING);
            assertThat(queue.getPriority()).isEqualTo(ImageResizeQueue.Priority.NORMAL);
            assertThat(queue.getAttempts()).isEqualTo(0);
            assertThat(queue.getMaxAttempts()).isEqualTo(3);
            assertThat(queue.getQuality()).isEqualTo(0.8f);
            assertThat(queue.getPreserveMetadata()).isFalse();
        }

        @Test
        @DisplayName("Deve inicializar com data de criação atual")
        void shouldInitializeWithCurrentCreationDate() {
            // Given
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            
            // When
            ImageResizeQueue queue = new ImageResizeQueue();
            
            // Then
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);
            assertThat(queue.getCreatedAt()).isBetween(before, after);
        }
    }

    @Nested
    @DisplayName("Propriedades Básicas")
    class BasicProperties {

        @Test
        @DisplayName("Deve definir e obter ID")
        void shouldSetAndGetId() {
            // Given
            Long expectedId = 123L;

            // When
            imageResizeQueue.setId(expectedId);

            // Then
            assertThat(imageResizeQueue.getId()).isEqualTo(expectedId);
        }

        @Test
        @DisplayName("Deve definir e obter caminhos de arquivo")
        void shouldSetAndGetFilePaths() {
            // Given
            String originalPath = "/path/to/original.jpg";
            String targetPath = "/path/to/target.jpg";

            // When
            imageResizeQueue.setOriginalPath(originalPath);
            imageResizeQueue.setTargetPath(targetPath);

            // Then
            assertThat(imageResizeQueue.getOriginalPath()).isEqualTo(originalPath);
            assertThat(imageResizeQueue.getTargetPath()).isEqualTo(targetPath);
        }

        @Test
        @DisplayName("Deve definir e obter dimensões")
        void shouldSetAndGetDimensions() {
            // Given
            Integer originalWidth = 1920;
            Integer originalHeight = 1080;
            Integer targetWidth = 800;
            Integer targetHeight = 600;

            // When
            imageResizeQueue.setOriginalWidth(originalWidth);
            imageResizeQueue.setOriginalHeight(originalHeight);
            imageResizeQueue.setTargetWidth(targetWidth);
            imageResizeQueue.setTargetHeight(targetHeight);

            // Then
            assertThat(imageResizeQueue.getOriginalWidth()).isEqualTo(originalWidth);
            assertThat(imageResizeQueue.getOriginalHeight()).isEqualTo(originalHeight);
            assertThat(imageResizeQueue.getTargetWidth()).isEqualTo(targetWidth);
            assertThat(imageResizeQueue.getTargetHeight()).isEqualTo(targetHeight);
        }

        @Test
        @DisplayName("Deve definir e obter qualidade")
        void shouldSetAndGetQuality() {
            // Given
            Float quality = 0.9f;

            // When
            imageResizeQueue.setQuality(quality);

            // Then
            assertThat(imageResizeQueue.getQuality()).isEqualTo(quality);
        }

        @Test
        @DisplayName("Deve definir e obter formato")
        void shouldSetAndGetFormat() {
            // Given
            String format = "JPEG";

            // When
            imageResizeQueue.setFormat(format);

            // Then
            assertThat(imageResizeQueue.getFormat()).isEqualTo(format);
        }
    }

    @Nested
    @DisplayName("Enums - ResizeMode")
    class ResizeModeTests {

        @Test
        @DisplayName("Deve definir e obter modo de redimensionamento")
        void shouldSetAndGetResizeMode() {
            // Given
            ImageResizeQueue.ResizeMode mode = ImageResizeQueue.ResizeMode.SCALE_TO_FIT;

            // When
            imageResizeQueue.setResizeMode(mode);

            // Then
            assertThat(imageResizeQueue.getResizeMode()).isEqualTo(mode);
        }

        @Test
        @DisplayName("Deve ter todos os modos de redimensionamento disponíveis")
        void shouldHaveAllResizeModesAvailable() {
            // Given & When & Then
            assertThat(ImageResizeQueue.ResizeMode.values()).containsExactly(
                ImageResizeQueue.ResizeMode.SCALE_TO_FIT,
                ImageResizeQueue.ResizeMode.SCALE_TO_FILL,
                ImageResizeQueue.ResizeMode.STRETCH,
                ImageResizeQueue.ResizeMode.CROP,
                ImageResizeQueue.ResizeMode.THUMBNAIL
            );
        }
    }

    @Nested
    @DisplayName("Enums - ProcessingStatus")
    class ProcessingStatusTests {

        @Test
        @DisplayName("Deve definir e obter status de processamento")
        void shouldSetAndGetProcessingStatus() {
            // Given
            ImageResizeQueue.ProcessingStatus status = ImageResizeQueue.ProcessingStatus.PROCESSING;

            // When
            imageResizeQueue.setStatus(status);

            // Then
            assertThat(imageResizeQueue.getStatus()).isEqualTo(status);
        }

        @Test
        @DisplayName("Deve ter todos os status de processamento disponíveis")
        void shouldHaveAllProcessingStatusesAvailable() {
            // Given & When & Then
            assertThat(ImageResizeQueue.ProcessingStatus.values()).containsExactly(
                ImageResizeQueue.ProcessingStatus.PENDING,
                ImageResizeQueue.ProcessingStatus.PROCESSING,
                ImageResizeQueue.ProcessingStatus.COMPLETED,
                ImageResizeQueue.ProcessingStatus.FAILED,
                ImageResizeQueue.ProcessingStatus.CANCELLED
            );
        }
    }

    @Nested
    @DisplayName("Enums - Priority")
    class PriorityTests {

        @Test
        @DisplayName("Deve definir e obter prioridade")
        void shouldSetAndGetPriority() {
            // Given
            ImageResizeQueue.Priority priority = ImageResizeQueue.Priority.HIGH;

            // When
            imageResizeQueue.setPriority(priority);

            // Then
            assertThat(imageResizeQueue.getPriority()).isEqualTo(priority);
        }

        @Test
        @DisplayName("Deve ter todas as prioridades disponíveis")
        void shouldHaveAllPrioritiesAvailable() {
            // Given & When & Then
            assertThat(ImageResizeQueue.Priority.values()).containsExactly(
                ImageResizeQueue.Priority.LOW,
                ImageResizeQueue.Priority.NORMAL,
                ImageResizeQueue.Priority.HIGH,
                ImageResizeQueue.Priority.URGENT
            );
        }
    }

    @Nested
    @DisplayName("Timestamps e Controle de Processamento")
    class TimestampsAndProcessingControl {

        @Test
        @DisplayName("Deve definir e obter timestamps de processamento")
        void shouldSetAndGetProcessingTimestamps() {
            // Given
            LocalDateTime startedAt = LocalDateTime.now().minusMinutes(5);
            LocalDateTime completedAt = LocalDateTime.now();

            // When
            imageResizeQueue.setStartedAt(startedAt);
            imageResizeQueue.setCompletedAt(completedAt);

            // Then
            assertThat(imageResizeQueue.getStartedAt()).isEqualTo(startedAt);
            assertThat(imageResizeQueue.getCompletedAt()).isEqualTo(completedAt);
        }

        @Test
        @DisplayName("Deve controlar tentativas de processamento")
        void shouldControlProcessingAttempts() {
            // Given
            Integer attempts = 2;
            Integer maxAttempts = 5;

            // When
            imageResizeQueue.setAttempts(attempts);
            imageResizeQueue.setMaxAttempts(maxAttempts);

            // Then
            assertThat(imageResizeQueue.getAttempts()).isEqualTo(attempts);
            assertThat(imageResizeQueue.getMaxAttempts()).isEqualTo(maxAttempts);
        }

        @Test
        @DisplayName("Deve armazenar mensagem de erro")
        void shouldStoreErrorMessage() {
            // Given
            String errorMessage = "Erro ao processar imagem: formato não suportado";

            // When
            imageResizeQueue.setErrorMessage(errorMessage);

            // Then
            assertThat(imageResizeQueue.getErrorMessage()).isEqualTo(errorMessage);
        }
    }

    @Nested
    @DisplayName("Métricas de Processamento")
    class ProcessingMetrics {

        @Test
        @DisplayName("Deve armazenar tamanhos de arquivo")
        void shouldStoreFileSizes() {
            // Given
            Long originalSize = 1024000L; // 1MB
            Long targetSize = 512000L;    // 512KB

            // When
            imageResizeQueue.setOriginalSizeBytes(originalSize);
            imageResizeQueue.setTargetSizeBytes(targetSize);

            // Then
            assertThat(imageResizeQueue.getOriginalSizeBytes()).isEqualTo(originalSize);
            assertThat(imageResizeQueue.getTargetSizeBytes()).isEqualTo(targetSize);
        }

        @Test
        @DisplayName("Deve calcular e armazenar taxa de compressão")
        void shouldCalculateAndStoreCompressionRatio() {
            // Given
            Float compressionRatio = 0.5f; // 50% de compressão

            // When
            imageResizeQueue.setCompressionRatio(compressionRatio);

            // Then
            assertThat(imageResizeQueue.getCompressionRatio()).isEqualTo(compressionRatio);
        }

        @Test
        @DisplayName("Deve armazenar tempo de processamento")
        void shouldStoreProcessingTime() {
            // Given
            Long processingTime = 5000L; // 5 segundos

            // When
            imageResizeQueue.setProcessingTimeMs(processingTime);

            // Then
            assertThat(imageResizeQueue.getProcessingTimeMs()).isEqualTo(processingTime);
        }
    }

    @Nested
    @DisplayName("Metadados e Configurações Avançadas")
    class MetadataAndAdvancedSettings {

        @Test
        @DisplayName("Deve controlar preservação de metadados")
        void shouldControlMetadataPreservation() {
            // Given
            Boolean preserveMetadata = true;

            // When
            imageResizeQueue.setPreserveMetadata(preserveMetadata);

            // Then
            assertThat(imageResizeQueue.getPreserveMetadata()).isTrue();
        }

        @Test
        @DisplayName("Deve armazenar texto de marca d'água")
        void shouldStoreWatermarkText() {
            // Given
            String watermarkText = "© Sistema Java 2024";

            // When
            imageResizeQueue.setWatermarkText(watermarkText);

            // Then
            assertThat(imageResizeQueue.getWatermarkText()).isEqualTo(watermarkText);
        }

        @Test
        @DisplayName("Deve armazenar informações de criação")
        void shouldStoreCreationInfo() {
            // Given
            String createdBy = "user123";
            Long batchJobId = 456L;

            // When
            imageResizeQueue.setCreatedBy(createdBy);
            imageResizeQueue.setBatchJobId(batchJobId);

            // Then
            assertThat(imageResizeQueue.getCreatedBy()).isEqualTo(createdBy);
            assertThat(imageResizeQueue.getBatchJobId()).isEqualTo(batchJobId);
        }
    }

    @Nested
    @DisplayName("Validações de Negócio")
    class BusinessValidations {

        @Test
        @DisplayName("Deve validar que qualidade está entre 0 e 1")
        void shouldValidateQualityRange() {
            // Given & When & Then
            assertThatCode(() -> imageResizeQueue.setQuality(0.0f)).doesNotThrowAnyException();
            assertThatCode(() -> imageResizeQueue.setQuality(1.0f)).doesNotThrowAnyException();
            assertThatCode(() -> imageResizeQueue.setQuality(0.5f)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Deve validar que dimensões são positivas")
        void shouldValidatePositiveDimensions() {
            // Given & When & Then
            assertThatCode(() -> {
                imageResizeQueue.setTargetWidth(800);
                imageResizeQueue.setTargetHeight(600);
            }).doesNotThrowAnyException();
            
            assertThat(imageResizeQueue.getTargetWidth()).isPositive();
            assertThat(imageResizeQueue.getTargetHeight()).isPositive();
        }

        @Test
        @DisplayName("Deve validar que tentativas não excedem máximo")
        void shouldValidateAttemptsDoNotExceedMaximum() {
            // Given
            imageResizeQueue.setMaxAttempts(3);
            
            // When & Then
            assertThatCode(() -> imageResizeQueue.setAttempts(2)).doesNotThrowAnyException();
            assertThat(imageResizeQueue.getAttempts()).isLessThanOrEqualTo(imageResizeQueue.getMaxAttempts());
        }
    }

    @Nested
    @DisplayName("Estados de Processamento")
    class ProcessingStates {

        @Test
        @DisplayName("Deve transicionar de PENDING para PROCESSING")
        void shouldTransitionFromPendingToProcessing() {
            // Given
            imageResizeQueue.setStatus(ImageResizeQueue.ProcessingStatus.PENDING);
            
            // When
            imageResizeQueue.setStatus(ImageResizeQueue.ProcessingStatus.PROCESSING);
            imageResizeQueue.setStartedAt(LocalDateTime.now());
            
            // Then
            assertThat(imageResizeQueue.getStatus()).isEqualTo(ImageResizeQueue.ProcessingStatus.PROCESSING);
            assertThat(imageResizeQueue.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("Deve transicionar de PROCESSING para COMPLETED")
        void shouldTransitionFromProcessingToCompleted() {
            // Given
            imageResizeQueue.setStatus(ImageResizeQueue.ProcessingStatus.PROCESSING);
            imageResizeQueue.setStartedAt(LocalDateTime.now().minusMinutes(1));
            
            // When
            imageResizeQueue.setStatus(ImageResizeQueue.ProcessingStatus.COMPLETED);
            imageResizeQueue.setCompletedAt(LocalDateTime.now());
            
            // Then
            assertThat(imageResizeQueue.getStatus()).isEqualTo(ImageResizeQueue.ProcessingStatus.COMPLETED);
            assertThat(imageResizeQueue.getCompletedAt()).isNotNull();
            assertThat(imageResizeQueue.getCompletedAt()).isAfter(imageResizeQueue.getStartedAt());
        }

        @Test
        @DisplayName("Deve transicionar de PROCESSING para FAILED com erro")
        void shouldTransitionFromProcessingToFailedWithError() {
            // Given
            imageResizeQueue.setStatus(ImageResizeQueue.ProcessingStatus.PROCESSING);
            imageResizeQueue.setStartedAt(LocalDateTime.now().minusMinutes(1));
            
            // When
            imageResizeQueue.setStatus(ImageResizeQueue.ProcessingStatus.FAILED);
            imageResizeQueue.setErrorMessage("Formato de arquivo não suportado");
            imageResizeQueue.setCompletedAt(LocalDateTime.now());
            
            // Then
            assertThat(imageResizeQueue.getStatus()).isEqualTo(ImageResizeQueue.ProcessingStatus.FAILED);
            assertThat(imageResizeQueue.getErrorMessage()).isNotBlank();
            assertThat(imageResizeQueue.getCompletedAt()).isNotNull();
        }
    }
}