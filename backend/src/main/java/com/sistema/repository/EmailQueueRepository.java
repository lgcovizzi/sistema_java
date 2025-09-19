package com.sistema.repository;

import com.sistema.entity.EmailQueue;
import com.sistema.entity.EmailQueue.EmailStatus;
import com.sistema.entity.EmailQueue.EmailType;
import com.sistema.entity.EmailQueue.Priority;
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
 * Repositório para operações com EmailQueue.
 * Fornece métodos para gerenciar a fila de emails.
 */
@Repository
public interface EmailQueueRepository extends JpaRepository<EmailQueue, Long> {

    /**
     * Busca emails prontos para processamento.
     * 
     * @param pageable configuração de paginação
     * @return página de emails prontos para envio
     */
    @Query("SELECT eq FROM EmailQueue eq WHERE eq.status = com.sistema.entity.EmailQueue$EmailStatus.PENDING AND " +
           "(eq.scheduledAt IS NULL OR eq.scheduledAt <= :now) " +
           "ORDER BY eq.priority DESC, eq.createdAt ASC")
    Page<EmailQueue> findReadyToProcess(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * Busca emails por status.
     * 
     * @param status status do email
     * @return lista de emails com o status especificado
     */
    List<EmailQueue> findByStatus(EmailStatus status);

    /**
     * Busca emails por tipo.
     * 
     * @param emailType tipo do email
     * @return lista de emails do tipo especificado
     */
    List<EmailQueue> findByEmailType(EmailType emailType);

    /**
     * Busca emails por prioridade.
     * 
     * @param priority prioridade do email
     * @return lista de emails com a prioridade especificada
     */
    List<EmailQueue> findByPriority(Priority priority);

    /**
     * Busca emails agendados para um período específico.
     * 
     * @param startDate data de início
     * @param endDate data de fim
     * @return lista de emails agendados no período
     */
    List<EmailQueue> findByScheduledAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Busca emails que falharam e ainda podem ser reprocessados.
     * 
     * @return lista de emails que podem ser reprocessados
     */
    @Query("SELECT eq FROM EmailQueue eq WHERE eq.status = com.sistema.entity.EmailQueue$EmailStatus.FAILED AND eq.attempts < eq.maxAttempts")
    List<EmailQueue> findFailedEmailsForRetry();

    /**
     * Busca emails que excederam o número máximo de tentativas.
     * 
     * @return lista de emails que não podem mais ser reprocessados
     */
    @Query("SELECT eq FROM EmailQueue eq WHERE eq.status = com.sistema.entity.EmailQueue$EmailStatus.FAILED AND eq.attempts >= eq.maxAttempts")
    List<EmailQueue> findPermanentlyFailedEmails();

    /**
     * Busca emails em processamento há muito tempo (possivelmente travados).
     * 
     * @param cutoffTime tempo limite
     * @return lista de emails que podem estar travados
     */
    @Query("SELECT eq FROM EmailQueue eq WHERE eq.status = com.sistema.entity.EmailQueue$EmailStatus.PROCESSING AND eq.createdAt < :cutoffTime")
    List<EmailQueue> findStuckEmails(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Conta emails por status.
     * 
     * @param status status do email
     * @return número de emails com o status especificado
     */
    long countByStatus(EmailStatus status);

    /**
     * Conta emails por prioridade.
     * 
     * @param priority prioridade do email
     * @return número de emails com a prioridade especificada
     */
    long countByPriority(Priority priority);

    /**
     * Conta emails por prioridade e status.
     * 
     * @param priority prioridade do email
     * @param status status do email
     * @return número de emails com a prioridade e status especificados
     */
    long countByPriorityAndStatus(Priority priority, EmailStatus status);

    /**
     * Conta emails por tipo.
     * 
     * @param emailType tipo do email
     * @return número de emails do tipo especificado
     */
    long countByEmailType(EmailType emailType);

    /**
     * Busca emails criados por um usuário específico.
     * 
     * @param createdBy usuário que criou o email
     * @param pageable configuração de paginação
     * @return página de emails criados pelo usuário
     */
    Page<EmailQueue> findByCreatedBy(String createdBy, Pageable pageable);

    /**
     * Busca emails de um job específico.
     * 
     * @param batchJobId ID do job
     * @return lista de emails do job
     */
    List<EmailQueue> findByBatchJobId(Long batchJobId);

    /**
     * Busca emails por destinatário.
     * 
     * @param recipientEmail email do destinatário
     * @return lista de emails para o destinatário
     */
    List<EmailQueue> findByRecipientEmail(String recipientEmail);

    /**
     * Busca emails antigos por data e status.
     * 
     * @param cutoffDate data limite
     * @param statuses lista de status
     * @return lista de emails antigos
     */
    List<EmailQueue> findByCreatedAtBeforeAndStatusIn(LocalDateTime cutoffDate, List<EmailStatus> statuses);

    /**
     * Remove emails antigos baseado na data de criação.
     * 
     * @param cutoffDate data limite para remoção
     */
    @Modifying
    @Query("DELETE FROM EmailQueue eq WHERE eq.createdAt < :cutoffDate AND eq.status IN (com.sistema.entity.EmailQueue$EmailStatus.SENT, com.sistema.entity.EmailQueue$EmailStatus.FAILED)")
    void deleteOldEmails(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Remove emails antigos processados.
     * 
     * @param cutoffDate data limite para remoção
     */
    @Modifying
    @Query("DELETE FROM EmailQueue eq WHERE eq.createdAt < :cutoffDate AND eq.status IN (com.sistema.entity.EmailQueue$EmailStatus.SENT, com.sistema.entity.EmailQueue$EmailStatus.FAILED)")
    void deleteOldProcessedEmails(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Atualiza status de emails travados para falha.
     * 
     * @param cutoffTime tempo limite para considerar email travado
     * @param errorMessage mensagem de erro a ser definida
     */
    @Modifying
    @Query("UPDATE EmailQueue eq SET eq.status = com.sistema.entity.EmailQueue$EmailStatus.FAILED, eq.errorMessage = :errorMessage " +
           "WHERE eq.status = com.sistema.entity.EmailQueue$EmailStatus.PROCESSING AND eq.createdAt < :cutoffTime")
    void markStuckEmailsAsFailed(@Param("cutoffTime") LocalDateTime cutoffTime, 
                                @Param("errorMessage") String errorMessage);

    /**
     * Reseta emails falhados para nova tentativa.
     */
    @Modifying
    @Query("UPDATE EmailQueue eq SET eq.status = com.sistema.entity.EmailQueue$EmailStatus.PENDING, eq.errorMessage = null " +
           "WHERE eq.status = com.sistema.entity.EmailQueue$EmailStatus.FAILED AND eq.attempts < eq.maxAttempts")
    void resetFailedEmailsForRetry();

    /**
     * Busca estatísticas de emails por status.
     * 
     * @return lista com contagem de emails por status
     */
    @Query("SELECT eq.status, COUNT(eq) FROM EmailQueue eq GROUP BY eq.status")
    List<Object[]> getEmailStatisticsByStatus();

    /**
     * Busca estatísticas de emails por tipo.
     * 
     * @return lista com contagem de emails por tipo
     */
    @Query("SELECT eq.emailType, COUNT(eq) FROM EmailQueue eq GROUP BY eq.emailType")
    List<Object[]> getEmailStatisticsByType();

    /**
     * Busca emails pendentes ordenados por prioridade e data de criação.
     * 
     * @param limit número máximo de emails
     * @return lista de emails pendentes
     */
    @Query("SELECT eq FROM EmailQueue eq WHERE eq.status = com.sistema.entity.EmailQueue$EmailStatus.PENDING AND " +
           "(eq.scheduledAt IS NULL OR eq.scheduledAt <= :now) " +
           "ORDER BY eq.priority DESC, eq.createdAt ASC")
    List<EmailQueue> findTopPendingEmails(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * Busca próximos emails pendentes para envio.
     * 
     * @param status status pendente
     * @param pageable configuração de paginação
     * @return lista de emails pendentes
     */
    List<EmailQueue> findTopByStatusOrderByPriorityDescCreatedAtAsc(EmailStatus status, Pageable pageable);

    /**
     * Conta emails prontos para processamento.
     * 
     * @param now data/hora atual
     * @return número de emails prontos para processamento
     */
    @Query("SELECT COUNT(eq) FROM EmailQueue eq WHERE eq.status = com.sistema.entity.EmailQueue$EmailStatus.PENDING AND " +
           "(eq.scheduledAt IS NULL OR eq.scheduledAt <= :now)")
    long countReadyToProcess(@Param("now") LocalDateTime now);

    /**
     * Marca emails presos como falhados.
     * 
     * @param failedStatus status de falha
     * @param processingStatus status de processamento
     * @param timeoutDate data limite
     * @return número de emails marcados como falhados
     */
    @Modifying
    @Query("UPDATE EmailQueue eq SET eq.status = :failedStatus, eq.sentAt = CURRENT_TIMESTAMP " +
           "WHERE eq.status = :processingStatus AND eq.createdAt < :timeoutDate")
    int markStuckEmailsAsFailed(@Param("failedStatus") EmailStatus failedStatus,
                               @Param("processingStatus") EmailStatus processingStatus,
                               @Param("timeoutDate") LocalDateTime timeoutDate);



    /**
     * Remove emails antigos processados.
     * 
     * @param sentStatus status enviado
     * @param failedStatus status falhado
     * @param cutoffDate data limite
     * @return número de registros removidos
     */
    @Modifying
    @Query("DELETE FROM EmailQueue eq WHERE eq.status IN (:sentStatus, :failedStatus) " +
           "AND eq.sentAt < :cutoffDate")
    int deleteOldEmails(@Param("sentStatus") EmailStatus sentStatus,
                       @Param("failedStatus") EmailStatus failedStatus,
                       @Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Busca estatísticas de envio.
     * 
     * @param sentStatus status enviado
     * @return estatísticas de envio
     */
    @Query("SELECT COUNT(eq), AVG(eq.attempts), SUM(CASE WHEN eq.status = :sentStatus THEN 1 ELSE 0 END) " +
           "FROM EmailQueue eq")
    Object[] getSendingStatistics(@Param("sentStatus") EmailStatus sentStatus);
}