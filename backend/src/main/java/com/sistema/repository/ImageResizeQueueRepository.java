package com.sistema.repository;

import com.sistema.entity.ImageResizeQueue;
import com.sistema.entity.ImageResizeQueue.ProcessingStatus;
import com.sistema.entity.ImageResizeQueue.Priority;
import com.sistema.entity.ImageResizeQueue.ResizeMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositório para operações com ImageResizeQueue.
 * Fornece métodos para gerenciar a fila de redimensionamento de imagens.
 */
@Repository
public interface ImageResizeQueueRepository extends JpaRepository<ImageResizeQueue, Long> {

    /**
     * Busca imagens prontas para processamento.
     * 
     * @param pageable configuração de paginação
     * @return página de imagens prontas para processamento
     */
    @Query("SELECT irq FROM ImageResizeQueue irq WHERE irq.status = 'PENDING' " +
           "ORDER BY irq.priority DESC, irq.createdAt ASC")
    Page<ImageResizeQueue> findReadyToProcess(Pageable pageable);

    /**
     * Busca imagens por status.
     * 
     * @param status status da imagem
     * @return lista de imagens com o status especificado
     */
    List<ImageResizeQueue> findByStatus(ProcessingStatus status);

    /**
     * Busca imagens por status com paginação.
     * 
     * @param status status da imagem
     * @param pageable configuração de paginação
     * @return página de imagens com o status especificado
     */
    Page<ImageResizeQueue> findByStatus(ProcessingStatus status, Pageable pageable);

    /**
     * Busca imagens por prioridade.
     * 
     * @param priority prioridade da imagem
     * @return lista de imagens com a prioridade especificada
     */
    List<ImageResizeQueue> findByPriority(Priority priority);

    /**
     * Busca imagens por prioridade com paginação.
     * 
     * @param priority prioridade da imagem
     * @param pageable configuração de paginação
     * @return página de imagens com a prioridade especificada
     */
    Page<ImageResizeQueue> findByPriority(Priority priority, Pageable pageable);

    /**
     * Busca imagens por modo de redimensionamento.
     * 
     * @param resizeMode modo de redimensionamento
     * @return lista de imagens com o modo especificado
     */
    List<ImageResizeQueue> findByResizeMode(ResizeMode resizeMode);

    /**
     * Busca imagens que falharam e ainda podem ser reprocessadas.
     * 
     * @return lista de imagens que podem ser reprocessadas
     */
    @Query("SELECT irq FROM ImageResizeQueue irq WHERE irq.status = 'FAILED' AND irq.attempts < irq.maxAttempts")
    List<ImageResizeQueue> findFailedImagesForRetry();

    /**
     * Busca imagens que excederam o número máximo de tentativas.
     * 
     * @return lista de imagens que não podem mais ser reprocessadas
     */
    @Query("SELECT irq FROM ImageResizeQueue irq WHERE irq.status = 'FAILED' AND irq.attempts >= irq.maxAttempts")
    List<ImageResizeQueue> findPermanentlyFailedImages();

    /**
     * Busca imagens em processamento há muito tempo (possivelmente travadas).
     * 
     * @param cutoffTime tempo limite
     * @return lista de imagens que podem estar travadas
     */
    @Query("SELECT irq FROM ImageResizeQueue irq WHERE irq.status = 'PROCESSING' AND irq.startedAt < :cutoffTime")
    List<ImageResizeQueue> findStuckImages(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Conta imagens por status.
     * 
     * @param status status da imagem
     * @return número de imagens com o status especificado
     */
    long countByStatus(ProcessingStatus status);

    /**
     * Conta imagens por prioridade.
     * 
     * @param priority prioridade da imagem
     * @return número de imagens com a prioridade especificada
     */
    long countByPriority(Priority priority);

    /**
     * Conta imagens por modo de redimensionamento.
     * 
     * @param resizeMode modo de redimensionamento
     * @return número de imagens com o modo especificado
     */
    long countByResizeMode(ResizeMode resizeMode);

    /**
     * Busca imagens criadas por um usuário específico.
     * 
     * @param createdBy usuário que criou a imagem
     * @param pageable configuração de paginação
     * @return página de imagens criadas pelo usuário
     */
    Page<ImageResizeQueue> findByCreatedBy(String createdBy, Pageable pageable);

    /**
     * Busca imagens de um job específico.
     * 
     * @param batchJobId ID do job
     * @return lista de imagens do job
     */
    List<ImageResizeQueue> findByBatchJobId(Long batchJobId);

    /**
     * Busca imagens por caminho original.
     * 
     * @param originalPath caminho original da imagem
     * @return lista de imagens com o caminho especificado
     */
    List<ImageResizeQueue> findByOriginalPath(String originalPath);

    /**
     * Busca imagens por dimensões de destino.
     * 
     * @param targetWidth largura de destino
     * @param targetHeight altura de destino
     * @return lista de imagens com as dimensões especificadas
     */
    List<ImageResizeQueue> findByTargetWidthAndTargetHeight(Integer targetWidth, Integer targetHeight);

    /**
     * Remove imagens antigas que foram processadas.
     * 
     * @param cutoffDate data limite para remoção
     */
    @Modifying
    @Query("DELETE FROM ImageResizeQueue irq WHERE irq.createdAt < :cutoffDate AND irq.status IN ('COMPLETED', 'FAILED')")
    void deleteOldImages(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Busca imagens antigas que foram processadas.
     * 
     * @param cutoffDate data limite para busca
     * @return lista de imagens antigas processadas
     */
    @Query("SELECT irq FROM ImageResizeQueue irq WHERE irq.createdAt < :cutoffDate AND irq.status IN ('COMPLETED', 'FAILED')")
    List<ImageResizeQueue> findOldCompletedImages(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Atualiza status de imagens travadas para falha.
     * 
     * @param cutoffTime tempo limite
     * @param errorMessage mensagem de erro
     */
    @Modifying
    @Query("UPDATE ImageResizeQueue irq SET irq.status = 'FAILED', irq.errorMessage = :errorMessage, irq.completedAt = :now " +
           "WHERE irq.status = 'PROCESSING' AND irq.startedAt < :cutoffTime")
    void markStuckImagesAsFailed(@Param("cutoffTime") LocalDateTime cutoffTime, 
                                @Param("errorMessage") String errorMessage,
                                @Param("now") LocalDateTime now);

    /**
     * Busca estatísticas de imagens por status.
     * 
     * @return lista com contagem de imagens por status
     */
    @Query("SELECT irq.status, COUNT(irq) FROM ImageResizeQueue irq GROUP BY irq.status")
    List<Object[]> getImageStatisticsByStatus();

    /**
     * Busca estatísticas de imagens por modo de redimensionamento.
     * 
     * @return lista com contagem de imagens por modo
     */
    @Query("SELECT irq.resizeMode, COUNT(irq) FROM ImageResizeQueue irq GROUP BY irq.resizeMode")
    List<Object[]> getImageStatisticsByResizeMode();

    /**
     * Busca estatísticas de compressão.
     * 
     * @return estatísticas de compressão
     */
    @Query("SELECT AVG(irq.compressionRatio), MIN(irq.compressionRatio), MAX(irq.compressionRatio), " +
           "AVG(irq.processingTimeMs), SUM(irq.originalSizeBytes), SUM(irq.targetSizeBytes) " +
           "FROM ImageResizeQueue irq WHERE irq.status = 'COMPLETED'")
    Object[] getCompressionStatistics();

    /**
     * Busca imagens pendentes ordenadas por prioridade e data de criação.
     * 
     * @param pageable configuração de paginação
     * @return lista de imagens pendentes
     */
    @Query("SELECT irq FROM ImageResizeQueue irq WHERE irq.status = 'PENDING' " +
           "ORDER BY irq.priority DESC, irq.createdAt ASC")
    List<ImageResizeQueue> findTopPendingImages(Pageable pageable);

    /**
     * Busca próximas imagens pendentes para processamento.
     * 
     * @param status status pendente
     * @param pageable configuração de paginação
     * @return lista de imagens pendentes
     */
    List<ImageResizeQueue> findTopByStatusOrderByPriorityDescCreatedAtAsc(ProcessingStatus status, Pageable pageable);

    /**
     * Conta imagens prontas para processamento.
     * 
     * @return número de imagens prontas para processamento
     */
    @Query("SELECT COUNT(irq) FROM ImageResizeQueue irq WHERE irq.status = 'PENDING'")
    long countReadyToProcess();

    /**
     * Marca imagens presas como falhadas.
     * 
     * @param failedStatus status de falha
     * @param processingStatus status de processamento
     * @param timeoutDate data limite
     * @return número de imagens marcadas como falhadas
     */
    @Modifying
    @Query("UPDATE ImageResizeQueue irq SET irq.status = :failedStatus, irq.completedAt = CURRENT_TIMESTAMP " +
           "WHERE irq.status = :processingStatus AND irq.startedAt < :timeoutDate")
    int markStuckImagesAsFailed(@Param("failedStatus") ProcessingStatus failedStatus,
                               @Param("processingStatus") ProcessingStatus processingStatus,
                               @Param("timeoutDate") LocalDateTime timeoutDate);

    /**
     * Busca imagens por formato de destino.
     * 
     * @param format formato da imagem
     * @return lista de imagens com o formato especificado
     */
    List<ImageResizeQueue> findByFormat(String format);

    /**
     * Busca imagens com watermark.
     * 
     * @return lista de imagens que têm watermark
     */
    @Query("SELECT irq FROM ImageResizeQueue irq WHERE irq.watermarkText IS NOT NULL AND irq.watermarkText != ''")
    List<ImageResizeQueue> findImagesWithWatermark();

    /**
     * Busca imagens por faixa de qualidade.
     * 
     * @param minQuality qualidade mínima
     * @param maxQuality qualidade máxima
     * @return lista de imagens na faixa de qualidade
     */
    @Query("SELECT irq FROM ImageResizeQueue irq WHERE irq.quality BETWEEN :minQuality AND :maxQuality")
    List<ImageResizeQueue> findByQualityRange(@Param("minQuality") Float minQuality, 
                                             @Param("maxQuality") Float maxQuality);

    /**
     * Remove imagens antigas processadas.
     * 
     * @param completedStatus status completado
     * @param failedStatus status falhado
     * @param cutoffDate data limite
     * @return número de registros removidos
     */
    @Modifying
    @Query("DELETE FROM ImageResizeQueue irq WHERE irq.status IN (:completedStatus, :failedStatus) " +
           "AND irq.completedAt < :cutoffDate")
    int deleteOldImages(@Param("completedStatus") ProcessingStatus completedStatus,
                       @Param("failedStatus") ProcessingStatus failedStatus,
                       @Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Busca estatísticas de processamento.
     * 
     * @param completedStatus status completado
     * @return estatísticas de processamento
     */
    @Query("SELECT AVG(irq.processingTimeMs), MIN(irq.processingTimeMs), MAX(irq.processingTimeMs), " +
           "COUNT(irq), SUM(irq.originalSizeBytes), SUM(irq.targetSizeBytes) " +
           "FROM ImageResizeQueue irq WHERE irq.status = :completedStatus")
    Object[] getProcessingStatistics(@Param("completedStatus") ProcessingStatus completedStatus);
}