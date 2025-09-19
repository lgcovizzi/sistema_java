package com.sistema.service;

import com.sistema.entity.BatchJob;
import com.sistema.entity.EmailQueue;
import com.sistema.entity.ImageResizeQueue;
import com.sistema.entity.FileProcessingQueue;
import com.sistema.repository.BatchJobRepository;
import com.sistema.repository.EmailQueueRepository;
import com.sistema.repository.ImageResizeQueueRepository;
import com.sistema.repository.FileProcessingQueueRepository;
import com.sistema.service.base.BaseService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Serviço para gerenciar jobs do Spring Batch.
 * Fornece métodos para criar, executar e monitorar jobs.
 */
@Service
@Transactional
public class BatchJobService extends BaseService {

    @Autowired
    private BatchJobRepository batchJobRepository;

    @Autowired
    private EmailQueueRepository emailQueueRepository;

    @Autowired
    private ImageResizeQueueRepository imageResizeQueueRepository;

    @Autowired
    private FileProcessingQueueRepository fileProcessingQueueRepository;

    @Autowired
    @Qualifier("asyncJobLauncher")
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("emailProcessingJob")
    private Job emailProcessingJob;

    @Autowired
    @Qualifier("imageResizeJob")
    private Job imageResizeJob;

    @Autowired
    @Qualifier("fileProcessingJob")
    private Job fileProcessingJob;

    // ========== MÉTODOS PARA JOBS DE EMAIL ==========

    /**
     * Cria um novo job de processamento de emails.
     * 
     * @param createdBy usuário que criou o job
     * @return job criado
     */
    public BatchJob createEmailProcessingJob(String createdBy) {
        logInfo("Criando job de processamento de emails para usuário: " + createdBy);
        
        BatchJob job = new BatchJob(
            "Email Processing Job",
            BatchJob.JobType.EMAIL_PROCESSING,
            "{}",
            createdBy
        );
        
        return batchJobRepository.save(job);
    }

    /**
     * Executa job de processamento de emails de forma assíncrona.
     * 
     * @param jobId ID do job
     * @return future com o job executado
     */
    @Async("batchTaskExecutor")
    public CompletableFuture<BatchJob> executeEmailProcessingJobAsync(Long jobId) {
        try {
            BatchJob result = executeEmailProcessingJob(jobId);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Executa job de processamento de emails.
     * 
     * @param jobId ID do job (opcional)
     * @return job executado
     */
    public BatchJob executeEmailProcessingJob(Long jobId) {
        BatchJob job;
        if (jobId != null) {
            job = batchJobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job não encontrado: " + jobId));
        } else {
            job = createEmailProcessingJob("system");
        }
        
        try {
            logInfo("Executando job de processamento de emails: " + job.getId());
            
            job.markAsStarted();
            batchJobRepository.save(job);
            
            updateEmailQueueWithJobId(job.getId());
            
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("jobId", job.getId())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            jobLauncher.run(emailProcessingJob, jobParameters);
            
            job.markAsCompleted();
            updateJobProgress(job);
            batchJobRepository.save(job);
            
            logInfo("Job de processamento de emails concluído: " + job.getId());
            return job;
            
        } catch (Exception e) {
            logError("Erro ao executar job de processamento de emails", e);
            job.markAsFailed(e.getMessage());
            batchJobRepository.save(job);
            throw new RuntimeException("Erro ao executar job: " + e.getMessage(), e);
        }
    }

    /**
     * Executa job de processamento de emails sem parâmetros.
     * 
     * @return job executado
     */
    public BatchJob executeEmailProcessingJob() {
        return executeEmailProcessingJob(null);
    }

    // ========== MÉTODOS PARA JOBS DE IMAGEM ==========

    /**
     * Executa job de redimensionamento de imagens.
     * 
     * @return job executado
     */
    public BatchJob executeImageResizeJob() {
        logInfo("Executando job de redimensionamento de imagens");
        
        BatchJob job = new BatchJob(
            "Image Resize Job",
            BatchJob.JobType.IMAGE_RESIZE,
            "{}",
            "system"
        );
        
        try {
            job.markAsStarted();
            batchJobRepository.save(job);
            
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("jobId", job.getId())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            jobLauncher.run(imageResizeJob, jobParameters);
            
            job.markAsCompleted();
            batchJobRepository.save(job);
            
            logInfo("Job de redimensionamento de imagens concluído: " + job.getId());
            return job;
            
        } catch (Exception e) {
            logError("Erro ao executar job de redimensionamento de imagens", e);
            job.markAsFailed(e.getMessage());
            batchJobRepository.save(job);
            throw new RuntimeException("Erro ao executar job: " + e.getMessage(), e);
        }
    }

    // ========== MÉTODOS PARA JOBS DE ARQUIVO ==========

    /**
     * Executa job de processamento de arquivos.
     * 
     * @return job executado
     */
    public BatchJob executeFileProcessingJob() {
        logInfo("Executando job de processamento de arquivos");
        
        BatchJob job = new BatchJob(
            "File Processing Job",
            BatchJob.JobType.FILE_LISTING,
            "{}",
            "system"
        );
        
        try {
            job.markAsStarted();
            batchJobRepository.save(job);
            
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("jobId", job.getId())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
            
            jobLauncher.run(fileProcessingJob, jobParameters);
            
            job.markAsCompleted();
            batchJobRepository.save(job);
            
            logInfo("Job de processamento de arquivos concluído: " + job.getId());
            return job;
            
        } catch (Exception e) {
            logError("Erro ao executar job de processamento de arquivos", e);
            job.markAsFailed(e.getMessage());
            batchJobRepository.save(job);
            throw new RuntimeException("Erro ao executar job: " + e.getMessage(), e);
        }
    }

    // ========== MÉTODOS DE BUSCA E CONSULTA ==========

    /**
     * Busca job por ID.
     * 
     * @param jobId ID do job
     * @return job encontrado
     */
    public Optional<BatchJob> findById(Long jobId) {
        return batchJobRepository.findById(jobId);
    }

    /**
     * Busca todos os jobs com paginação.
     * 
     * @param pageable configuração de paginação
     * @return página de jobs
     */
    public Page<BatchJob> findAll(Pageable pageable) {
        return batchJobRepository.findAll(pageable);
    }

    /**
     * Busca jobs por tipo com paginação.
     * 
     * @param jobType tipo do job
     * @param pageable configuração de paginação
     * @return página de jobs
     */
    public Page<BatchJob> findByType(BatchJob.JobType jobType, Pageable pageable) {
        return batchJobRepository.findByJobType(jobType, pageable);
    }

    /**
     * Busca jobs por status com paginação.
     * 
     * @param status status do job
     * @param pageable configuração de paginação
     * @return página de jobs
     */
    public Page<BatchJob> findByStatus(BatchJob.JobStatus status, Pageable pageable) {
        return batchJobRepository.findByStatus(status, pageable);
    }

    /**
     * Busca jobs por tipo e status com paginação.
     * 
     * @param jobType tipo do job
     * @param status status do job
     * @param pageable configuração de paginação
     * @return página de jobs
     */
    public Page<BatchJob> findByTypeAndStatus(BatchJob.JobType jobType, BatchJob.JobStatus status, Pageable pageable) {
        return batchJobRepository.findByJobTypeAndStatus(jobType, status, pageable);
    }

    /**
     * Cancela um job em execução.
     * 
     * @param jobId ID do job
     * @return true se cancelado com sucesso
     */
    @Transactional
    public boolean cancelJob(Long jobId) {
        Optional<BatchJob> jobOpt = batchJobRepository.findById(jobId);
        if (jobOpt.isPresent()) {
            BatchJob job = jobOpt.get();
            if (job.getStatus() == BatchJob.JobStatus.RUNNING || job.getStatus() == BatchJob.JobStatus.PENDING) {
                job.setStatus(BatchJob.JobStatus.CANCELLED);
                job.setCompletedAt(LocalDateTime.now());
                job.setErrorMessage("Job cancelado pelo usuário");
                batchJobRepository.save(job);
                logInfo("Job " + jobId + " cancelado com sucesso");
                return true;
            }
        }
        return false;
    }

    /**
     * Remove jobs antigos.
     * 
     * @param cutoffDate data limite
     * @return número de jobs removidos
     */
    @Transactional
    public int deleteOldJobs(LocalDateTime cutoffDate) {
        logInfo("Removendo jobs antigos anteriores a: " + cutoffDate);
        List<BatchJob> oldJobs = batchJobRepository.findByCreatedAtBefore(cutoffDate);
        int count = oldJobs.size();
        batchJobRepository.deleteAll(oldJobs);
        logInfo("Removidos " + count + " jobs antigos");
        return count;
    }

    // ========== MÉTODOS PARA GERENCIAR FILAS ==========

    /**
     * Adiciona email à fila.
     * 
     * @param email email para adicionar
     * @return email adicionado
     */
    public EmailQueue addEmailToQueue(EmailQueue email) {
        validateNotNull(email, "Email não pode ser nulo");
        
        if (email.getCreatedAt() == null) {
            email.setCreatedAt(LocalDateTime.now());
        }
        if (email.getStatus() == null) {
            email.setStatus(EmailQueue.EmailStatus.PENDING);
        }
        
        return emailQueueRepository.save(email);
    }

    /**
     * Adiciona múltiplos emails à fila.
     * 
     * @param emails lista de emails
     * @param createdBy usuário que criou
     * @return lista de emails adicionados
     */
    public List<EmailQueue> addEmailsToQueue(List<EmailQueue> emails, String createdBy) {
        validateNotNull(emails, "Lista de emails não pode ser nula");
        
        emails.forEach(email -> {
            if (email.getCreatedAt() == null) {
                email.setCreatedAt(LocalDateTime.now());
            }
            if (email.getStatus() == null) {
                email.setStatus(EmailQueue.EmailStatus.PENDING);
            }
        });
        
        return emailQueueRepository.saveAll(emails);
    }

    /**
     * Busca jobs por status.
     * 
     * @param status status do job
     * @return lista de jobs
     */
    public List<BatchJob> findJobsByStatus(BatchJob.JobStatus status) {
        return batchJobRepository.findByStatus(status);
    }

    /**
     * Busca jobs por tipo.
     * 
     * @param jobType tipo do job
     * @return lista de jobs
     */
    public List<BatchJob> findJobsByType(BatchJob.JobType jobType) {
        return batchJobRepository.findByJobType(jobType);
    }

    /**
     * Busca jobs paginados.
     * 
     * @param pageable configuração de paginação
     * @return página de jobs
     */
    public Page<BatchJob> findAllJobs(Pageable pageable) {
        return batchJobRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * Busca jobs de um usuário.
     * 
     * @param createdBy usuário
     * @param pageable configuração de paginação
     * @return página de jobs
     */
    public Page<BatchJob> findJobsByUser(String createdBy, Pageable pageable) {
        return batchJobRepository.findByCreatedBy(createdBy, pageable);
    }

    /**
     * Busca emails da fila por status.
     * 
     * @param status status do email
     * @return lista de emails
     */
    public List<EmailQueue> findEmailsByStatus(EmailQueue.EmailStatus status) {
        return emailQueueRepository.findByStatus(status);
    }

    /**
     * Busca emails prontos para processamento.
     * 
     * @param pageable configuração de paginação
     * @return página de emails
     */
    public Page<EmailQueue> findEmailsReadyToProcess(Pageable pageable) {
        return emailQueueRepository.findReadyToProcess(LocalDateTime.now(), pageable);
    }

    /**
     * Obtém estatísticas de jobs.
     * 
     * @return mapa com estatísticas
     */
    public Map<String, Object> getJobStatistics() {
        logInfo("Obtendo estatísticas de jobs");
        
        long totalJobs = batchJobRepository.count();
        long pendingJobs = batchJobRepository.countByStatus(BatchJob.JobStatus.PENDING);
        long runningJobs = batchJobRepository.countByStatus(BatchJob.JobStatus.RUNNING);
        long completedJobs = batchJobRepository.countByStatus(BatchJob.JobStatus.COMPLETED);
        long failedJobs = batchJobRepository.countByStatus(BatchJob.JobStatus.FAILED);
        
        long totalEmails = emailQueueRepository.count();
        long pendingEmails = emailQueueRepository.countByStatus(EmailQueue.EmailStatus.PENDING);
        long processingEmails = emailQueueRepository.countByStatus(EmailQueue.EmailStatus.PROCESSING);
        long sentEmails = emailQueueRepository.countByStatus(EmailQueue.EmailStatus.SENT);
        long failedEmails = emailQueueRepository.countByStatus(EmailQueue.EmailStatus.FAILED);

        long totalImages = imageResizeQueueRepository.count();
        long pendingImages = imageResizeQueueRepository.countByStatus(ImageResizeQueue.ProcessingStatus.PENDING);
        long processingImages = imageResizeQueueRepository.countByStatus(ImageResizeQueue.ProcessingStatus.PROCESSING);
        long completedImages = imageResizeQueueRepository.countByStatus(ImageResizeQueue.ProcessingStatus.COMPLETED);
        long failedImages = imageResizeQueueRepository.countByStatus(ImageResizeQueue.ProcessingStatus.FAILED);

        long totalFiles = fileProcessingQueueRepository.count();
        long pendingFiles = fileProcessingQueueRepository.countByStatus(FileProcessingQueue.ProcessingStatus.PENDING);
        long processingFiles = fileProcessingQueueRepository.countByStatus(FileProcessingQueue.ProcessingStatus.PROCESSING);
        long completedFiles = fileProcessingQueueRepository.countByStatus(FileProcessingQueue.ProcessingStatus.COMPLETED);
        long failedFiles = fileProcessingQueueRepository.countByStatus(FileProcessingQueue.ProcessingStatus.FAILED);
        
        Map<String, Object> jobStats = new HashMap<>();
        jobStats.put("total", totalJobs);
        jobStats.put("pending", pendingJobs);
        jobStats.put("running", runningJobs);
        jobStats.put("completed", completedJobs);
        jobStats.put("failed", failedJobs);
        
        Map<String, Object> emailStats = new HashMap<>();
        emailStats.put("total", totalEmails);
        emailStats.put("pending", pendingEmails);
        emailStats.put("processing", processingEmails);
        emailStats.put("sent", sentEmails);
        emailStats.put("failed", failedEmails);
        
        Map<String, Object> imageStats = new HashMap<>();
        imageStats.put("total", totalImages);
        imageStats.put("pending", pendingImages);
        imageStats.put("processing", processingImages);
        imageStats.put("completed", completedImages);
        imageStats.put("failed", failedImages);
        
        Map<String, Object> fileStats = new HashMap<>();
        fileStats.put("total", totalFiles);
        fileStats.put("pending", pendingFiles);
        fileStats.put("processing", processingFiles);
        fileStats.put("completed", completedFiles);
        fileStats.put("failed", failedFiles);
        
        Map<String, Object> result = new HashMap<>();
        result.put("jobs", jobStats);
        result.put("emails", emailStats);
        result.put("images", imageStats);
        result.put("files", fileStats);
        
        return result;
    }

    /**
     * Limpa jobs e emails antigos.
     * 
     * @param daysOld número de dias para considerar antigo
     */
    @Transactional
    public void cleanupOldData(int daysOld) {
        logInfo("Limpando dados antigos de " + daysOld + " dias");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        
        // Remove emails antigos
        emailQueueRepository.deleteOldEmails(cutoffDate);
        
        // Remove imagens antigas
        imageResizeQueueRepository.deleteOldImages(cutoffDate);
        
        // Remove arquivos antigos
        fileProcessingQueueRepository.deleteOldFiles(
            FileProcessingQueue.ProcessingStatus.COMPLETED,
            FileProcessingQueue.ProcessingStatus.FAILED,
            cutoffDate);
        
        // Remove jobs antigos
        batchJobRepository.deleteByCreatedAtBefore(cutoffDate);
        
        logInfo("Limpeza de dados antigos concluída");
    }

    /**
     * Marca emails travados como falha.
     */
    @Transactional
    public void markStuckEmailsAsFailed() {
        logInfo("Marcando emails travados como falha");
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
        String errorMessage = "Email marcado como falha devido a timeout de processamento";
        
        emailQueueRepository.markStuckEmailsAsFailed(cutoffTime, errorMessage);
        
        logInfo("Emails travados marcados como falha");
    }

    /**
     * Atualiza emails da fila com ID do job.
     * 
     * @param jobId ID do job
     */
    private void updateEmailQueueWithJobId(Long jobId) {
        List<EmailQueue> readyEmails = emailQueueRepository.findTopPendingEmails(
            LocalDateTime.now(), 
            Pageable.ofSize(1000)
        );
        
        readyEmails.forEach(email -> email.setBatchJobId(jobId));
        emailQueueRepository.saveAll(readyEmails);
    }

    /**
     * Atualiza progresso do job.
     * 
     * @param job job para atualizar
     */
    private void updateJobProgress(BatchJob job) {
        if (job.getId() != null) {
            long sentEmails = emailQueueRepository.countByStatus(EmailQueue.EmailStatus.SENT);
            job.setProcessedItems((int) sentEmails);
        }
    }
}