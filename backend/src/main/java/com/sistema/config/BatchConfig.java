package com.sistema.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuração base do Spring Batch.
 * Configura o executor de jobs e o pool de threads para processamento assíncrono.
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    /**
     * Configura o executor de tarefas para jobs do Spring Batch.
     * 
     * @return TaskExecutor configurado
     */
    @Bean(name = "batchTaskExecutor")
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("batch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Configura o JobLauncher para execução assíncrona de jobs.
     * 
     * @param jobRepository repositório de jobs
     * @param taskExecutor executor de tarefas
     * @return JobLauncher configurado
     * @throws Exception se houver erro na configuração
     */
    @Bean
    public JobLauncher asyncJobLauncher(JobRepository jobRepository, TaskExecutor batchTaskExecutor) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(batchTaskExecutor);
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
}