package com.sistema.repository;

import com.sistema.entity.FileProcessingQueue;
import com.sistema.entity.FileProcessingQueue.OperationType;
import com.sistema.entity.FileProcessingQueue.Priority;
import com.sistema.entity.FileProcessingQueue.ProcessingStatus;
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
 * Repositório para operações com FileProcessingQueue.
 * Fornece métodos para gerenciar a fila de processamento de arquivos.
 */
@Repository
public interface FileProcessingQueueRepository extends JpaRepository<FileProcessingQueue, Long> {

    /**
     * Busca arquivos prontos para processamento.
     * 
     * @param status status dos arquivos
     * @param pageable configuração de paginação
     * @return página de arquivos prontos para processamento
     */
    Page<FileProcessingQueue> findByStatusOrderByPriorityDescCreatedAtAsc(ProcessingStatus status, Pageable pageable);

    /**
     * Busca arquivos por status.
     * 
     * @param status status do arquivo
     * @return lista de arquivos com o status especificado
     */
    List<FileProcessingQueue> findByStatus(ProcessingStatus status);

    /**
     * Busca arquivos por status com paginação.
     * 
     * @param status status do arquivo
     * @param pageable configuração de paginação
     * @return página de arquivos com o status especificado
     */
    Page<FileProcessingQueue> findByStatus(ProcessingStatus status, Pageable pageable);

    /**
     * Busca arquivos por tipo de operação com paginação.
     * 
     * @param operationType tipo de operação
     * @param pageable configuração de paginação
     * @return página de arquivos do tipo especificado
     */
    Page<FileProcessingQueue> findByOperationType(OperationType operationType, Pageable pageable);

    /**
     * Busca arquivos por tipo de operação.
     * 
     * @param operationType tipo de operação
     * @return lista de arquivos do tipo especificado
     */
    List<FileProcessingQueue> findByOperationType(OperationType operationType);

    /**
     * Busca arquivos por prioridade.
     * 
     * @param priority prioridade do arquivo
     * @return lista de arquivos com a prioridade especificada
     */
    List<FileProcessingQueue> findByPriority(Priority priority);

    /**
     * Busca arquivos por prioridade com paginação.
     * 
     * @param priority prioridade do arquivo
     * @param pageable configuração de paginação
     * @return página de arquivos com a prioridade especificada
     */
    Page<FileProcessingQueue> findByPriority(Priority priority, Pageable pageable);

    /**
     * Busca arquivos falhados que podem ser reprocessados.
     * 
     * @return lista de arquivos falhados para retry
     */
    @Query("SELECT fpq FROM FileProcessingQueue fpq WHERE fpq.status = :failedStatus AND fpq.attempts < fpq.maxAttempts")
    List<FileProcessingQueue> findFailedFilesForRetry(@Param("failedStatus") ProcessingStatus failedStatus);

    /**
     * Busca arquivos permanentemente falhados.
     * 
     * @return lista de arquivos permanentemente falhados
     */
    @Query("SELECT fpq FROM FileProcessingQueue fpq WHERE fpq.status = :failedStatus AND fpq.attempts >= fpq.maxAttempts")
    List<FileProcessingQueue> findPermanentlyFailedFiles(@Param("failedStatus") ProcessingStatus failedStatus);

    /**
     * Busca arquivos presos em processamento.
     * 
     * @param cutoffTime tempo limite
     * @return lista de arquivos presos
     */
    @Query("SELECT fpq FROM FileProcessingQueue fpq WHERE fpq.status = :processingStatus AND fpq.startedAt < :cutoffTime")
    List<FileProcessingQueue> findStuckFiles(@Param("processingStatus") ProcessingStatus processingStatus, @Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Marca arquivos presos como falhados.
     * 
     * @param failedStatus status de falha
     * @param processingStatus status de processamento
     * @param timeoutDate data limite
     * @return número de arquivos marcados como falhados
     */
    @Modifying
    @Query("UPDATE FileProcessingQueue fpq SET fpq.status = :failedStatus, fpq.completedAt = CURRENT_TIMESTAMP " +
           "WHERE fpq.status = :processingStatus AND fpq.startedAt < :timeoutDate")
    int markStuckFilesAsFailed(@Param("failedStatus") ProcessingStatus failedStatus,
                              @Param("processingStatus") ProcessingStatus processingStatus,
                              @Param("timeoutDate") LocalDateTime timeoutDate);

    /**
     * Conta arquivos por status.
     * 
     * @param status status do arquivo
     * @return número de arquivos com o status especificado
     */
    long countByStatus(ProcessingStatus status);

    /**
     * Conta arquivos por tipo de operação.
     * 
     * @param operationType tipo de operação
     * @return número de arquivos do tipo especificado
     */
    long countByOperationType(OperationType operationType);

    /**
     * Conta arquivos por prioridade.
     * 
     * @param priority prioridade do arquivo
     * @return número de arquivos com a prioridade especificada
     */
    long countByPriority(Priority priority);

    /**
     * Busca arquivos criados por um usuário específico.
     * 
     * @param createdBy usuário que criou o arquivo
     * @param pageable configuração de paginação
     * @return página de arquivos criados pelo usuário
     */
    Page<FileProcessingQueue> findByCreatedBy(String createdBy, Pageable pageable);

    /**
     * Busca arquivos de um job específico.
     * 
     * @param batchJobId ID do job
     * @return lista de arquivos do job
     */
    List<FileProcessingQueue> findByBatchJobId(Long batchJobId);

    /**
     * Busca arquivos por caminho de origem.
     * 
     * @param sourcePath caminho de origem
     * @return lista de arquivos com o caminho especificado
     */
    List<FileProcessingQueue> findBySourcePath(String sourcePath);

    /**
     * Busca arquivos por padrão de arquivo.
     * 
     * @param filePattern padrão de arquivo
     * @return lista de arquivos com o padrão especificado
     */
    List<FileProcessingQueue> findByFilePattern(String filePattern);

    /**
     * Busca arquivos por formato de compressão.
     * 
     * @param compressionFormat formato de compressão
     * @return lista de arquivos com o formato especificado
     */
    List<FileProcessingQueue> findByCompressionFormat(String compressionFormat);

    /**
     * Busca arquivos que incluem subdiretórios.
     * 
     * @param includeSubdirectories se inclui subdiretórios
     * @return lista de arquivos que incluem ou não subdiretórios
     */
    List<FileProcessingQueue> findByIncludeSubdirectories(Boolean includeSubdirectories);

    /**
     * Busca arquivos por faixa de contagem de arquivos.
     * 
     * @param minCount contagem mínima
     * @param maxCount contagem máxima
     * @return lista de arquivos na faixa especificada
     */
    @Query("SELECT fpq FROM FileProcessingQueue fpq WHERE fpq.fileCount BETWEEN :minCount AND :maxCount")
    List<FileProcessingQueue> findByFileCountRange(@Param("minCount") Long minCount, 
                                                  @Param("maxCount") Long maxCount);

    /**
     * Busca arquivos por faixa de tamanho total.
     * 
     * @param minSize tamanho mínimo em bytes
     * @param maxSize tamanho máximo em bytes
     * @return lista de arquivos na faixa especificada
     */
    @Query("SELECT fpq FROM FileProcessingQueue fpq WHERE fpq.totalSizeBytes BETWEEN :minSize AND :maxSize")
    List<FileProcessingQueue> findByTotalSizeRange(@Param("minSize") Long minSize, 
                                                   @Param("maxSize") Long maxSize);

    /**
     * Remove registros antigos de arquivos processados.
     * 
     * @param completedStatus status completado
     * @param failedStatus status falhado
     * @param cutoffDate data limite para remoção
     * @return número de registros removidos
     */
    @Modifying
    @Query("DELETE FROM FileProcessingQueue fpq WHERE fpq.status IN (:completedStatus, :failedStatus) " +
           "AND fpq.completedAt < :cutoffDate")
    int deleteOldFiles(@Param("completedStatus") ProcessingStatus completedStatus, 
                      @Param("failedStatus") ProcessingStatus failedStatus,
                      @Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Busca estatísticas de arquivos por status.
     * 
     * @return lista com contagem de arquivos por status
     */
    @Query("SELECT fpq.status, COUNT(fpq) FROM FileProcessingQueue fpq GROUP BY fpq.status")
    List<Object[]> getFileStatisticsByStatus();

    /**
     * Busca estatísticas de arquivos por tipo de operação.
     * 
     * @return lista com contagem de arquivos por tipo de operação
     */
    @Query("SELECT fpq.operationType, COUNT(fpq) FROM FileProcessingQueue fpq GROUP BY fpq.operationType")
    List<Object[]> getFileStatisticsByOperationType();

    /**
     * Busca estatísticas de processamento.
     * 
     * @return estatísticas de processamento
     */
    @Query("SELECT AVG(fpq.processingTimeMs), MIN(fpq.processingTimeMs), MAX(fpq.processingTimeMs), " +
           "SUM(fpq.fileCount), SUM(fpq.totalSizeBytes), COUNT(fpq) " +
           "FROM FileProcessingQueue fpq WHERE fpq.status = :completedStatus")
    Object[] getProcessingStatistics(@Param("completedStatus") ProcessingStatus completedStatus);

    /**
     * Busca arquivos pendentes ordenados por prioridade e data de criação.
     * 
     * @param status status pendente
     * @param pageable configuração de paginação
     * @return lista de arquivos pendentes
     */
    List<FileProcessingQueue> findTopByStatusOrderByPriorityDescCreatedAtAsc(ProcessingStatus status, Pageable pageable);



    /**
     * Busca arquivos por status e tipo de operação.
     * 
     * @param status status do arquivo
     * @param operationType tipo de operação
     * @return lista de arquivos com o status e tipo especificados
     */
    List<FileProcessingQueue> findByStatusAndOperationType(ProcessingStatus status, OperationType operationType);

    /**
     * Busca arquivos por prioridade e status.
     * 
     * @param priority prioridade do arquivo
     * @param status status do arquivo
     * @return lista de arquivos com a prioridade e status especificados
     */
    List<FileProcessingQueue> findByPriorityAndStatus(Priority priority, ProcessingStatus status);

    /**
     * Busca arquivos para deletar fonte.
     * 
     * @param status status completado
     * @param cutoffDate data limite
     * @return lista de arquivos para deletar fonte
     */
    @Query("SELECT fpq FROM FileProcessingQueue fpq WHERE fpq.status = :status " +
           "AND fpq.completedAt < :cutoffDate AND fpq.deleteSourceAfterProcessing = true")
    List<FileProcessingQueue> findFilesToDeleteSource(@Param("status") ProcessingStatus status,
                                                      @Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Busca arquivos por nível de compressão.
     * 
     * @param compressionLevel nível de compressão
     * @return lista de arquivos com o nível especificado
     */
    List<FileProcessingQueue> findByCompressionLevel(Integer compressionLevel);

    /**
     * Busca arquivos por preservação de estrutura de diretório.
     * 
     * @param preserveDirectoryStructure se preserva estrutura
     * @return lista de arquivos que preservam ou não a estrutura
     */
    List<FileProcessingQueue> findByPreserveDirectoryStructure(Boolean preserveDirectoryStructure);

    /**
     * Busca arquivos criados em um período.
     * 
     * @param startDate data inicial
     * @param endDate data final
     * @return lista de arquivos criados no período
     */
    @Query("SELECT fpq FROM FileProcessingQueue fpq WHERE fpq.createdAt BETWEEN :startDate AND :endDate")
    List<FileProcessingQueue> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Busca arquivos por faixa de tempo de processamento.
     * 
     * @param minTime tempo mínimo em ms
     * @param maxTime tempo máximo em ms
     * @return lista de arquivos na faixa especificada
     */
    @Query("SELECT fpq FROM FileProcessingQueue fpq WHERE fpq.processingTimeMs BETWEEN :minTime AND :maxTime")
    List<FileProcessingQueue> findByProcessingTimeRange(@Param("minTime") Long minTime, 
                                                        @Param("maxTime") Long maxTime);

    /**
     * Busca arquivos antigos completados ou falhados.
     * 
     * @param completedStatus status completado
     * @param failedStatus status falhado
     * @param cutoffDate data limite
     * @return lista de arquivos antigos
     */
    @Query("SELECT fpq FROM FileProcessingQueue fpq WHERE fpq.status IN (:completedStatus, :failedStatus) " +
           "AND fpq.completedAt < :cutoffDate")
    List<FileProcessingQueue> findOldCompletedFiles(@Param("completedStatus") ProcessingStatus completedStatus,
                                                    @Param("failedStatus") ProcessingStatus failedStatus,
                                                    @Param("cutoffDate") LocalDateTime cutoffDate);
}