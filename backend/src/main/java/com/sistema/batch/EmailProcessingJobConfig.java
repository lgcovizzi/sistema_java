package com.sistema.batch;

import com.sistema.entity.EmailQueue;
import com.sistema.repository.EmailQueueRepository;
import com.sistema.service.SmtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Configuração do job de processamento de emails.
 * Define o job Spring Batch para processar emails da fila.
 */
@Configuration
public class EmailProcessingJobConfig {

    private static final Logger logger = LoggerFactory.getLogger(EmailProcessingJobConfig.class);

    @Autowired
    private EmailQueueRepository emailQueueRepository;

    @Autowired
    private SmtpService smtpService;

    /**
     * Configura o reader para ler emails da fila.
     * 
     * @return ItemReader configurado
     */
    @Bean
    public ItemReader<EmailQueue> emailReader() {
        return new RepositoryItemReaderBuilder<EmailQueue>()
                .name("emailReader")
                .repository(emailQueueRepository)
                .methodName("findReadyToProcess")
                .arguments(LocalDateTime.now())
                .pageSize(10)
                .sorts(new java.util.HashMap<String, Sort.Direction>() {{
                    put("priority", Sort.Direction.DESC);
                    put("createdAt", Sort.Direction.ASC);
                }})
                .build();
    }

    /**
     * Configura o processor para processar emails.
     * 
     * @return ItemProcessor configurado
     */
    @Bean
    public ItemProcessor<EmailQueue, EmailQueue> emailProcessor() {
        return new ItemProcessor<EmailQueue, EmailQueue>() {
            @Override
            public EmailQueue process(EmailQueue email) throws Exception {
                logger.info("Processando email ID: {} para: {}", email.getId(), email.getRecipientEmail());
                
                try {
                    // Marca como processando
                    email.markAsProcessing();
                    email.incrementAttempts();
                    emailQueueRepository.save(email);
                    
                    // Verifica se o email está pronto para processamento
                    if (!email.isReadyToProcess() && !email.getStatus().equals(EmailQueue.EmailStatus.PROCESSING)) {
                        logger.debug("Email ID: {} não está pronto para processamento", email.getId());
                        return null; // Pula este item
                    }
                    
                    // Processa o email baseado no tipo
                    switch (email.getEmailType()) {
                        case VERIFICATION:
                            processVerificationEmail(email);
                            break;
                        case PASSWORD_RECOVERY:
                            processPasswordRecoveryEmail(email);
                            break;
                        case NOTIFICATION:
                            processNotificationEmail(email);
                            break;
                        case MARKETING:
                            processMarketingEmail(email);
                            break;
                        case SYSTEM:
                            processSystemEmail(email);
                            break;
                        case BULK:
                            processBulkEmail(email);
                            break;
                        default:
                            processGenericEmail(email);
                            break;
                    }
                    
                    logger.info("Email ID: {} processado com sucesso", email.getId());
                    return email;
                    
                } catch (Exception e) {
                    logger.error("Erro ao processar email ID: {}: {}", email.getId(), e.getMessage(), e);
                    
                    String errorMessage = "Erro no processamento: " + e.getMessage();
                    if (email.hasReachedMaxAttempts()) {
                        email.markAsFailed(errorMessage);
                        logger.error("Email ID: {} falhou permanentemente após {} tentativas", 
                                   email.getId(), email.getAttempts());
                    } else {
                        email.setStatus(EmailQueue.EmailStatus.PENDING);
                        email.setErrorMessage(errorMessage);
                        logger.warn("Email ID: {} falhará e será reprocessado. Tentativa: {}/{}", 
                                  email.getId(), email.getAttempts(), email.getMaxAttempts());
                    }
                    
                    return email;
                }
            }
            
            private void processVerificationEmail(EmailQueue email) throws Exception {
                if (email.getTemplateName() != null) {
                    smtpService.sendHtmlEmail(
                        email.getRecipientEmail(),
                        email.getRecipientName(),
                        email.getSubject(),
                        email.getContent()
                    );
                } else {
                    smtpService.sendSimpleEmail(
                        email.getRecipientEmail(),
                        email.getSubject(),
                        email.getContent()
                    );
                }
                email.markAsSent();
            }
            
            private void processPasswordRecoveryEmail(EmailQueue email) throws Exception {
                smtpService.sendHtmlEmail(
                    email.getRecipientEmail(),
                    email.getRecipientName(),
                    email.getSubject(),
                    email.getContent()
                );
                email.markAsSent();
            }
            
            private void processNotificationEmail(EmailQueue email) throws Exception {
                smtpService.sendSimpleEmail(
                    email.getRecipientEmail(),
                    email.getSubject(),
                    email.getContent()
                );
                email.markAsSent();
            }
            
            private void processMarketingEmail(EmailQueue email) throws Exception {
                smtpService.sendHtmlEmail(
                    email.getRecipientEmail(),
                    email.getRecipientName(),
                    email.getSubject(),
                    email.getContent()
                );
                email.markAsSent();
            }
            
            private void processSystemEmail(EmailQueue email) throws Exception {
                smtpService.sendSimpleEmail(
                    email.getRecipientEmail(),
                    email.getSubject(),
                    email.getContent()
                );
                email.markAsSent();
            }
            
            private void processBulkEmail(EmailQueue email) throws Exception {
                // Para emails em massa, usar envio assíncrono
                smtpService.sendHtmlEmailAsync(
                    email.getRecipientEmail(),
                    email.getRecipientName(),
                    email.getSubject(),
                    email.getContent()
                );
                email.markAsSent();
            }
            
            private void processGenericEmail(EmailQueue email) throws Exception {
                smtpService.sendSimpleEmail(
                    email.getRecipientEmail(),
                    email.getSubject(),
                    email.getContent()
                );
                email.markAsSent();
            }
        };
    }

    /**
     * Configura o writer para salvar emails processados.
     * 
     * @return ItemWriter configurado
     */
    @Bean
    public ItemWriter<EmailQueue> emailWriter() {
        return emails -> {
            for (EmailQueue email : emails) {
                try {
                    emailQueueRepository.save(email);
                    logger.debug("Email ID: {} salvo com status: {}", email.getId(), email.getStatus());
                } catch (Exception e) {
                    logger.error("Erro ao salvar email ID: {}: {}", email.getId(), e.getMessage(), e);
                }
            }
        };
    }

    /**
     * Configura o step de processamento de emails.
     * 
     * @param jobRepository repositório de jobs
     * @param transactionManager gerenciador de transações
     * @return Step configurado
     */
    @Bean
    public Step emailProcessingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("emailProcessingStep", jobRepository)
                .<EmailQueue, EmailQueue>chunk(10, transactionManager)
                .reader(emailReader())
                .processor(emailProcessor())
                .writer(emailWriter())
                .faultTolerant()
                .skipLimit(5)
                .skip(Exception.class)
                .build();
    }

    /**
     * Configura o job de processamento de emails.
     * 
     * @param jobRepository repositório de jobs
     * @param emailProcessingStep step de processamento
     * @return Job configurado
     */
    @Bean
    public Job emailProcessingJob(JobRepository jobRepository, Step emailProcessingStep) {
        return new JobBuilder("emailProcessingJob", jobRepository)
                .start(emailProcessingStep)
                .build();
    }
}