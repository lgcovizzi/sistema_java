package com.sistema.repository;

import com.sistema.entity.BatchJob;
import com.sistema.entity.BatchJob.JobStatus;
import com.sistema.entity.BatchJob.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para operações com BatchJob.
 * Fornece métodos para consultar e gerenciar jobs do Spring Batch.
 */
@Repository
public interface BatchJobRepository extends JpaRepository<BatchJob, Long> {

    /**
     * Busca jobs por status.
     * 
     * @param status status do job
     * @return lista de jobs com o status especificado
     */
    List<BatchJob> findByStatus(JobStatus status);

    /**
     * Busca jobs por tipo.
     * 
     * @param jobType tipo do job
     * @return lista de jobs do tipo especificado
     */
    List<BatchJob> findByJobType(JobType jobType);

    /**
     * Busca jobs por tipo e status.
     * 
     * @param jobType tipo do job
     * @param status status do job
     * @return lista de jobs que atendem aos critérios
     */
    List<BatchJob> findByJobTypeAndStatus(JobType jobType, JobStatus status);

    /**
     * Busca jobs por tipo com paginação.
     * 
     * @param jobType tipo do job
     * @param pageable configuração de paginação
     * @return página de jobs do tipo especificado
     */
    Page<BatchJob> findByJobType(JobType jobType, Pageable pageable);

    /**
     * Busca jobs por status com paginação.
     * 
     * @param status status do job
     * @param pageable configuração de paginação
     * @return página de jobs com o status especificado
     */
    Page<BatchJob> findByStatus(JobStatus status, Pageable pageable);

    /**
     * Busca jobs por tipo e status com paginação.
     * 
     * @param jobType tipo do job
     * @param status status do job
     * @param pageable configuração de paginação
     * @return página de jobs que atendem aos critérios
     */
    Page<BatchJob> findByJobTypeAndStatus(JobType jobType, JobStatus status, Pageable pageable);

    /**
     * Busca jobs criados por um usuário específico.
     * 
     * @param createdBy usuário que criou o job
     * @param pageable configuração de paginação
     * @return página de jobs criados pelo usuário
     */
    Page<BatchJob> findByCreatedBy(String createdBy, Pageable pageable);

    /**
     * Busca jobs criados em um período específico.
     * 
     * @param startDate data de início
     * @param endDate data de fim
     * @return lista de jobs criados no período
     */
    List<BatchJob> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Busca jobs em execução há mais de um tempo específico.
     * 
     * @param status status do job (RUNNING)
     * @param cutoffTime tempo limite
     * @return lista de jobs que podem estar travados
     */
    @Query("SELECT bj FROM BatchJob bj WHERE bj.status = :status AND bj.startedAt < :cutoffTime")
    List<BatchJob> findStuckJobs(@Param("status") JobStatus status, @Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Conta jobs por status.
     * 
     * @param status status do job
     * @return número de jobs com o status especificado
     */
    long countByStatus(JobStatus status);

    /**
     * Conta jobs por tipo.
     * 
     * @param jobType tipo do job
     * @return número de jobs do tipo especificado
     */
    long countByJobType(JobType jobType);

    /**
     * Busca o último job de um tipo específico.
     * 
     * @param jobType tipo do job
     * @return último job do tipo especificado
     */
    Optional<BatchJob> findFirstByJobTypeOrderByCreatedAtDesc(JobType jobType);

    /**
     * Busca jobs ordenados por data de criação (mais recentes primeiro).
     * 
     * @param pageable configuração de paginação
     * @return página de jobs ordenados por data
     */
    Page<BatchJob> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Busca jobs que falharam e têm mensagem de erro.
     * 
     * @param status status do job (FAILED)
     * @return lista de jobs com erros
     */
    @Query("SELECT bj FROM BatchJob bj WHERE bj.status = :status AND bj.errorMessage IS NOT NULL")
    List<BatchJob> findJobsWithErrors(@Param("status") JobStatus status);

    /**
     * Busca estatísticas de jobs por tipo.
     * 
     * @return lista com contagem de jobs por tipo
     */
    @Query("SELECT bj.jobType, COUNT(bj) FROM BatchJob bj GROUP BY bj.jobType")
    List<Object[]> getJobStatisticsByType();

    /**
     * Busca estatísticas de jobs por status.
     * 
     * @return lista com contagem de jobs por status
     */
    @Query("SELECT bj.status, COUNT(bj) FROM BatchJob bj GROUP BY bj.status")
    List<Object[]> getJobStatisticsByStatus();

    /**
     * Busca jobs criados antes de uma data específica.
     * 
     * @param cutoffDate data limite para busca
     * @return lista de jobs criados antes da data especificada
     */
    List<BatchJob> findByCreatedAtBefore(LocalDateTime cutoffDate);

    /**
     * Remove jobs antigos baseado na data de criação.
     * 
     * @param cutoffDate data limite para remoção
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);

    /**
     * Busca jobs pendentes ordenados por data de criação.
     * 
     * @param status status do job (PENDING)
     * @return lista de jobs pendentes ordenados
     */
    @Query("SELECT bj FROM BatchJob bj WHERE bj.status = :status ORDER BY bj.createdAt ASC")
    List<BatchJob> findPendingJobsOrderedByCreation(@Param("status") JobStatus status);
}