package com.sistema.service;

import com.sistema.config.SmtpConfig;
import com.sistema.entity.EmailConfiguration;
import com.sistema.enums.EmailProvider;
import com.sistema.service.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Serviço SMTP dedicado para envio de emails.
 * Agora suporta configurações dinâmicas (Mailtrap, Gmail) através do EmailConfigurationService.
 */
@Service
public class SmtpService extends BaseService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SmtpConfig.SmtpConfiguration smtpConfiguration;

    @Autowired
    private EmailConfigurationService emailConfigurationService;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.email.async:true}")
    private boolean asyncEnabled;

    @Value("${app.email.retry.attempts:3}")
    private int retryAttempts;

    @Value("${app.email.retry.delay:1000}")
    private long retryDelay;

    /**
     * Obtém o JavaMailSender configurado dinamicamente.
     * Usa a configuração padrão do banco ou fallback para configuração estática.
     * 
     * @return JavaMailSender configurado
     */
    private JavaMailSender getConfiguredMailSender() {
        Optional<EmailConfiguration> configOpt = emailConfigurationService.getDefaultConfiguration();
        
        if (configOpt.isPresent()) {
            EmailConfiguration config = configOpt.get();
            logDebug(String.format("Usando configuração dinâmica: %s", config.getProviderDisplayName()));
            return createMailSender(config);
        } else {
            logDebug("Usando configuração estática (fallback)");
            return mailSender;
        }
    }

    /**
     * Cria um JavaMailSender baseado na configuração fornecida.
     * 
     * @param config configuração de email
     * @return JavaMailSender configurado
     */
    private JavaMailSender createMailSender(EmailConfiguration config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        mailSender.setHost(config.getHost());
        mailSender.setPort(config.getPort());
        mailSender.setUsername(config.getUsername());
        mailSender.setPassword(config.getPassword());
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        
        // Configurações específicas por provedor
        if (config.getProvider() == EmailProvider.GMAIL) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.trust", config.getHost());
            props.put("mail.debug", "false");
            logDebug("Configurações Gmail aplicadas");
        } else if (config.getProvider() == EmailProvider.MAILTRAP) {
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.ssl.enable", "false");
            props.put("mail.debug", "false");
            logDebug("Configurações Mailtrap aplicadas");
        }
        
        return mailSender;
    }

    /**
     * Obtém o username padrão para envio de emails.
     * 
     * @return username configurado
     */
    private String getDefaultFromEmail() {
        Optional<EmailConfiguration> configOpt = emailConfigurationService.getDefaultConfiguration();
        
        if (configOpt.isPresent()) {
            return configOpt.get().getUsername();
        } else {
            return smtpConfiguration.getUsername();
        }
    }

    /**
     * Envia email simples de forma síncrona.
     * 
     * @param to destinatário
     * @param subject assunto
     * @param text conteúdo do email
     * @return true se enviado com sucesso
     */
    public boolean sendSimpleEmail(String to, String subject, String text) {
        return sendSimpleEmail(null, to, subject, text);
    }

    /**
     * Envia email simples de forma síncrona.
     * 
     * @param from remetente (opcional)
     * @param to destinatário
     * @param subject assunto
     * @param text conteúdo do email
     * @return true se enviado com sucesso
     */
    public boolean sendSimpleEmail(String from, String to, String subject, String text) {
        if (!emailEnabled) {
            logWarn("Envio de email desabilitado. Email não enviado para: " + to);
            return false;
        }

        validateNotEmpty(to, "to");
        validateNotEmpty(subject, "subject");
        validateNotEmpty(text, "text");

        try {
            JavaMailSender configuredSender = getConfiguredMailSender();
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from != null ? from : getDefaultFromEmail());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setSentDate(new java.util.Date());

            return sendWithRetry(() -> {
                configuredSender.send(message);
                logInfo(String.format("Email simples enviado com sucesso para: %s usando configuração dinâmica", to));
                return true;
            });

        } catch (Exception e) {
            logError("Erro ao enviar email simples para: " + to, e);
            return false;
        }
    }

    /**
     * Envia email HTML de forma síncrona.
     * 
     * @param to destinatário
     * @param subject assunto
     * @param htmlContent conteúdo HTML
     * @return true se enviado com sucesso
     */
    public boolean sendHtmlEmail(String to, String subject, String htmlContent) {
        return sendHtmlEmail(null, to, subject, htmlContent);
    }

    /**
     * Envia email HTML de forma síncrona.
     * 
     * @param from remetente (opcional)
     * @param to destinatário
     * @param subject assunto
     * @param htmlContent conteúdo HTML
     * @return true se enviado com sucesso
     */
    public boolean sendHtmlEmail(String from, String to, String subject, String htmlContent) {
        if (!emailEnabled) {
            logWarn("Envio de email desabilitado. Email HTML não enviado para: " + to);
            return false;
        }

        validateNotEmpty(to, "to");
        validateNotEmpty(subject, "subject");
        validateNotEmpty(htmlContent, "htmlContent");

        try {
            JavaMailSender configuredSender = getConfiguredMailSender();
            
            MimeMessage message = configuredSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, smtpConfiguration.getDefaultEncoding());

            helper.setFrom(from != null ? from : getDefaultFromEmail());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setSentDate(new java.util.Date());

            return sendWithRetry(() -> {
                configuredSender.send(message);
                logInfo(String.format("Email HTML enviado com sucesso para: %s usando configuração dinâmica", to));
                return true;
            });

        } catch (MessagingException | MailException e) {
            logError("Erro ao enviar email HTML para: " + to, e);
            return false;
        }
    }

    /**
     * Envia email simples de forma assíncrona.
     * 
     * @param to destinatário
     * @param subject assunto
     * @param text conteúdo do email
     * @return CompletableFuture com resultado
     */
    public CompletableFuture<Boolean> sendSimpleEmailAsync(String to, String subject, String text) {
        return sendSimpleEmailAsync(null, to, subject, text);
    }

    /**
     * Envia email simples de forma assíncrona.
     * 
     * @param from remetente (opcional)
     * @param to destinatário
     * @param subject assunto
     * @param text conteúdo do email
     * @return CompletableFuture com resultado
     */
    public CompletableFuture<Boolean> sendSimpleEmailAsync(String from, String to, String subject, String text) {
        if (!asyncEnabled) {
            return CompletableFuture.completedFuture(sendSimpleEmail(from, to, subject, text));
        }

        return CompletableFuture.supplyAsync(() -> sendSimpleEmail(from, to, subject, text))
                .exceptionally(throwable -> {
                    logError("Erro no envio assíncrono de email simples para: " + to, (Exception) throwable);
                    return false;
                });
    }

    /**
     * Envia email HTML de forma assíncrona.
     * 
     * @param to destinatário
     * @param subject assunto
     * @param htmlContent conteúdo HTML
     * @return CompletableFuture com resultado
     */
    public CompletableFuture<Boolean> sendHtmlEmailAsync(String to, String subject, String htmlContent) {
        return sendHtmlEmailAsync(null, to, subject, htmlContent);
    }

    /**
     * Envia email HTML de forma assíncrona.
     * 
     * @param from remetente (opcional)
     * @param to destinatário
     * @param subject assunto
     * @param htmlContent conteúdo HTML
     * @return CompletableFuture com resultado
     */
    public CompletableFuture<Boolean> sendHtmlEmailAsync(String from, String to, String subject, String htmlContent) {
        if (!asyncEnabled) {
            return CompletableFuture.completedFuture(sendHtmlEmail(from, to, subject, htmlContent));
        }

        return CompletableFuture.supplyAsync(() -> sendHtmlEmail(from, to, subject, htmlContent))
                .exceptionally(throwable -> {
                    logError("Erro no envio assíncrono de email HTML para: " + to, (Exception) throwable);
                    return false;
                });
    }

    /**
     * Testa a conexão SMTP.
     * 
     * @return true se conexão bem-sucedida
     */
    public boolean testConnection() {
        try {
            logInfo("Testando conexão SMTP com configuração dinâmica...");
            
            // Obtém a configuração ativa
            String testEmail = getDefaultFromEmail();
            String subject = "Teste de Conexão SMTP - " + new java.util.Date();
            String content = "Este é um email de teste para verificar a conectividade SMTP com configuração dinâmica.";
            
            boolean result = sendSimpleEmail(testEmail, testEmail, subject, content);
            
            if (result) {
                logInfo("Teste de conexão SMTP realizado com sucesso com configuração dinâmica");
            } else {
                logWarn("Falha no teste de conexão SMTP com configuração dinâmica");
            }
            
            return result;
            
        } catch (Exception e) {
            logError("Erro durante teste de conexão SMTP", e);
            return false;
        }
    }

    /**
     * Obtém estatísticas do serviço SMTP.
     * 
     * @return SmtpStats com informações do serviço
     */
    public SmtpStats getSmtpStats() {
        return new SmtpStats(
                emailEnabled,
                asyncEnabled,
                smtpConfiguration.getHost(),
                smtpConfiguration.getPort(),
                smtpConfiguration.getProtocol(),
                smtpConfiguration.isAuth(),
                smtpConfiguration.isStarttlsEnable(),
                smtpConfiguration.isSslEnable(),
                retryAttempts,
                retryDelay,
                LocalDateTime.now()
        );
    }

    /**
     * Verifica se o serviço de email está habilitado.
     * 
     * @return true se habilitado
     */
    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    /**
     * Verifica se o envio assíncrono está habilitado.
     * 
     * @return true se habilitado
     */
    public boolean isAsyncEnabled() {
        return asyncEnabled;
    }

    /**
     * Retorna estatísticas do serviço SMTP.
     * 
     * @return Map com estatísticas do SMTP
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        SmtpStats smtpStats = getSmtpStats();
        
        stats.put("emailEnabled", smtpStats.isEmailEnabled());
        stats.put("asyncEnabled", smtpStats.isAsyncEnabled());
        stats.put("host", smtpStats.getHost());
        stats.put("port", smtpStats.getPort());
        stats.put("protocol", smtpStats.getProtocol());
        stats.put("auth", smtpStats.isAuth());
        stats.put("starttlsEnable", smtpStats.isStarttlsEnable());
        stats.put("sslEnable", smtpStats.isSslEnable());
        stats.put("retryAttempts", smtpStats.getRetryAttempts());
        stats.put("retryDelay", smtpStats.getRetryDelay());
        stats.put("timestamp", smtpStats.getTimestamp());
        stats.put("connectionTest", testConnection());
        
        return stats;
    }

    /**
     * Executa operação com retry em caso de falha.
     * 
     * @param operation operação a ser executada
     * @return resultado da operação
     */
    private boolean sendWithRetry(EmailOperation operation) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                logWarn("Tentativa " + attempt + " de " + retryAttempts + " falhou: " + e.getMessage());
                
                if (attempt < retryAttempts) {
                    try {
                        Thread.sleep(retryDelay * attempt); // Backoff exponencial
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        logError("Todas as tentativas de envio falharam", lastException);
        return false;
    }

    /**
     * Interface funcional para operações de email.
     */
    @FunctionalInterface
    private interface EmailOperation {
        boolean execute() throws Exception;
    }

    /**
     * Classe de estatísticas do SMTP.
     */
    public static class SmtpStats {
        private final boolean emailEnabled;
        private final boolean asyncEnabled;
        private final String host;
        private final int port;
        private final String protocol;
        private final boolean auth;
        private final boolean starttlsEnable;
        private final boolean sslEnable;
        private final int retryAttempts;
        private final long retryDelay;
        private final LocalDateTime timestamp;

        public SmtpStats(boolean emailEnabled, boolean asyncEnabled, String host, int port, 
                        String protocol, boolean auth, boolean starttlsEnable, boolean sslEnable,
                        int retryAttempts, long retryDelay, LocalDateTime timestamp) {
            this.emailEnabled = emailEnabled;
            this.asyncEnabled = asyncEnabled;
            this.host = host;
            this.port = port;
            this.protocol = protocol;
            this.auth = auth;
            this.starttlsEnable = starttlsEnable;
            this.sslEnable = sslEnable;
            this.retryAttempts = retryAttempts;
            this.retryDelay = retryDelay;
            this.timestamp = timestamp;
        }

        // Getters
        public boolean isEmailEnabled() { return emailEnabled; }
        public boolean isAsyncEnabled() { return asyncEnabled; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public String getProtocol() { return protocol; }
        public boolean isAuth() { return auth; }
        public boolean isStarttlsEnable() { return starttlsEnable; }
        public boolean isSslEnable() { return sslEnable; }
        public int getRetryAttempts() { return retryAttempts; }
        public long getRetryDelay() { return retryDelay; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}