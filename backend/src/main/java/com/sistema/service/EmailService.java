package com.sistema.service;

import com.sistema.entity.User;
import com.sistema.service.base.BaseService;
// import com.sistema.telemetry.metrics.CustomMetricsService;
// import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Serviço para envio de emails.
 * Responsável por enviar emails de verificação e outros tipos de notificação.
 * Agora usa SmtpService desacoplado para envio real dos emails.
 */
@Service
public class EmailService extends BaseService {

    @Autowired
    private SmtpService smtpService;

    @Autowired
    private TemplateEngine templateEngine;
    
    // @Autowired
    // private CustomMetricsService customMetricsService;

    @Value("${spring.mail.username:noreply@sistema.com}")
    private String fromEmail;

    @Value("${app.email.verification.url:http://localhost:8080/api/auth/verify-email}")
    private String verificationBaseUrl;

    @Value("${app.name:Sistema Java}")
    private String appName;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    /**
     * Envia email de verificação para o usuário.
     * 
     * @param user usuário para enviar email
     * @param verificationToken token de verificação
     * @return true se email foi enviado com sucesso
     */
    public boolean sendVerificationEmail(User user, String verificationToken) {
        // Timer.Sample emailTimer = customMetricsService.startEmailTimer();
        
        validateNotNull(user, "user");
        validateNotEmpty(verificationToken, "verificationToken");
        validateNotEmpty(user.getEmail(), "user.email");

        if (!emailEnabled) {
            logInfo("Envio de email está desabilitado. Email de verificação não enviado para: " + user.getEmail());
            // emailTimer.stop();
            return false;
        }

        try {
            String verificationUrl = buildVerificationUrl(verificationToken);
            String subject = "Verificação de Email - " + appName;
            String htmlContent = buildVerificationEmailHtml(user, verificationUrl, false);

            sendHtmlEmail(user.getEmail(), subject, htmlContent);
            
            // Registrar métricas de sucesso
            // customMetricsService.recordEmailSent();
            // emailTimer.stop();
            
            logInfo("Email de verificação enviado para: " + user.getEmail());
            return true;

        } catch (Exception e) {
            // Registrar métricas de falha
            // customMetricsService.recordEmailError();
            // emailTimer.stop();
            
            logError("Erro ao enviar email de verificação para " + user.getEmail() + ": " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Reenvia email de verificação para o usuário.
     * 
     * @param user usuário para reenviar email
     * @param verificationToken novo token de verificação
     * @return true se email foi reenviado com sucesso
     */
    public boolean resendVerificationEmail(User user, String verificationToken) {
        validateNotNull(user, "user");
        validateNotEmpty(verificationToken, "verificationToken");

        if (!emailEnabled) {
            logWarn("Email está desabilitado. Reenvio de email de verificação não será enviado para: " + user.getEmail());
            return false;
        }

        try {
            String verificationUrl = buildVerificationUrl(verificationToken);
            String htmlContent = buildVerificationEmailHtml(user, verificationUrl, true);
            
            sendHtmlEmail(user.getEmail(), "Novo Link de Verificação - " + appName, htmlContent);
            logInfo("Email de verificação reenviado para: " + user.getEmail());
            return true;
        } catch (Exception e) {
            logError("Erro ao reenviar email de verificação para " + user.getEmail() + ": " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Envia um email simples usando SmtpService
     * 
     * @param to Destinatário
     * @param subject Assunto
     * @param text Corpo do email
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        validateNotEmpty(to, "to");
        validateNotEmpty(subject, "subject");
        validateNotEmpty(text, "text");

        try {
            smtpService.sendSimpleEmail(to, subject, text);
            logInfo("Email enviado com sucesso para: " + to);
        } catch (Exception e) {
            logError("Erro ao enviar email para " + to, e);
            throw new RuntimeException("Falha ao enviar email", e);
        }
    }

    /**
     * Envia email HTML usando SmtpService.
     * 
     * @param to destinatário
     * @param subject assunto
     * @param htmlContent conteúdo HTML
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        validateNotEmpty(to, "to");
        validateNotEmpty(subject, "subject");
        validateNotEmpty(htmlContent, "htmlContent");

        try {
            smtpService.sendHtmlEmail(to, subject, htmlContent);
            logInfo("Email HTML enviado com sucesso para: " + to);
        } catch (Exception e) {
            logError("Erro ao enviar email HTML para " + to, e);
            throw new RuntimeException("Falha ao enviar email HTML", e);
        }
    }

    /**
     * Envia email de teste para verificar configuração do Mailtrap
     * 
     * @param to Destinatário do teste
     */
    public void sendTestEmail(String to) {
        String subject = "Teste de Configuração - Sistema Java";
        String text = "Este é um email de teste para verificar a configuração do Mailtrap.\n\n" +
                     "Se você recebeu este email, a configuração está funcionando corretamente!\n\n" +
                     "Sistema Java - " + java.time.LocalDateTime.now();
        
        sendSimpleEmail(to, subject, text);
    }

    /**
     * Envia email de boas-vindas
     * 
     * @param to Email do usuário
     * @param name Nome do usuário
     */
    public void sendWelcomeEmail(String to, String name) {
        String subject = "Bem-vindo ao Sistema Java!";
        String text = String.format(
            "Olá %s,\n\n" +
            "Bem-vindo ao Sistema Java!\n\n" +
            "Sua conta foi criada com sucesso. Agora você pode acessar todas as funcionalidades do sistema.\n\n" +
            "Se você tiver alguma dúvida, não hesite em entrar em contato conosco.\n\n" +
            "Atenciosamente,\n" +
            "Equipe Sistema Java",
            name
        );
        
        sendSimpleEmail(to, subject, text);
    }

    /**
     * Envia email de recuperação de senha usando template HTML
     * 
     * @param user Usuário que solicitou a recuperação
     * @param resetToken Token de recuperação
     * @return true se email foi enviado com sucesso
     */
    public boolean sendPasswordResetEmail(User user, String resetToken) {
        validateNotNull(user, "user");
        validateNotEmpty(resetToken, "resetToken");

        if (!emailEnabled) {
            logWarn("Email desabilitado - não enviando email de recuperação para: " + user.getEmail());
            return false;
        }

        try {
            String htmlContent = buildPasswordResetEmailHtml(user, resetToken);
            String subject = "🔐 Recuperação de Senha - " + appName;

            sendHtmlEmail(user.getEmail(), subject, htmlContent);
            logInfo("Email de recuperação de senha enviado para: " + user.getEmail());
            return true;

        } catch (Exception e) {
            logError("Erro ao enviar email de recuperação para: " + user.getEmail(), e);
            return false;
        }
    }

    /**
     * Envia email simples de forma assíncrona
     * 
     * @param to Destinatário
     * @param subject Assunto
     * @param text Corpo do email
     * @return CompletableFuture com resultado do envio
     */
    public CompletableFuture<Boolean> sendSimpleEmailAsync(String to, String subject, String text) {
        validateNotEmpty(to, "to");
        validateNotEmpty(subject, "subject");
        validateNotEmpty(text, "text");

        return smtpService.sendSimpleEmailAsync(to, subject, text)
            .thenApply(success -> {
                if (success) {
                    logInfo("Email assíncrono enviado com sucesso para: " + to);
                } else {
                    logWarn("Falha no envio de email assíncrono para: " + to);
                }
                return success;
            })
            .exceptionally(throwable -> {
                logError("Erro no envio de email assíncrono para " + to, (Exception) throwable);
                return false;
            });
    }

    /**
     * Envia email HTML de forma assíncrona
     * 
     * @param to destinatário
     * @param subject assunto
     * @param htmlContent conteúdo HTML
     * @return CompletableFuture com resultado do envio
     */
    public CompletableFuture<Boolean> sendHtmlEmailAsync(String to, String subject, String htmlContent) {
        validateNotEmpty(to, "to");
        validateNotEmpty(subject, "subject");
        validateNotEmpty(htmlContent, "htmlContent");

        return smtpService.sendHtmlEmailAsync(to, subject, htmlContent)
            .thenApply(success -> {
                if (success) {
                    logInfo("Email HTML assíncrono enviado com sucesso para: " + to);
                } else {
                    logWarn("Falha no envio de email HTML assíncrono para: " + to);
                }
                return success;
            })
            .exceptionally(throwable -> {
                logError("Erro no envio de email HTML assíncrono para " + to, (Exception) throwable);
                return false;
            });
    }

    /**
     * Constrói URL de verificação.
     * 
     * @param token token de verificação
     * @return URL completa de verificação
     */
    private String buildVerificationUrl(String token) {
        return verificationBaseUrl + "?token=" + token;
    }

    /**
     * Constrói texto do email de verificação.
     * 
     * @param user usuário
     * @param verificationUrl URL de verificação
     * @return texto do email
     */
    private String buildVerificationEmailText(User user, String verificationUrl) {
        return String.format(
            "Olá %s,\n\n" +
            "Obrigado por se cadastrar no %s!\n\n" +
            "Para ativar sua conta, clique no link abaixo:\n" +
            "%s\n\n" +
            "Este link é válido por 24 horas.\n\n" +
            "Se você não se cadastrou em nosso sistema, ignore este email.\n\n" +
            "Atenciosamente,\n" +
            "Equipe %s",
            user.getFullName(),
            appName,
            verificationUrl,
            appName
        );
    }

    /**
     * Constrói HTML do email de verificação usando template.
     * 
     * @param user usuário
     * @param verificationUrl URL de verificação
     * @param isResend se é reenvio
     * @return HTML do email
     */
    private String buildVerificationEmailHtml(User user, String verificationUrl, boolean isResend) {
        try {
            Context context = new Context();
            context.setVariable("firstName", getFirstName(user));
            context.setVariable("appName", appName);
            context.setVariable("verificationUrl", verificationUrl);
            context.setVariable("expiryHours", "24");
            
            String templateName = isResend ? "email-verification-resend" : "email-verification";
            return templateEngine.process(templateName, context);
        } catch (Exception e) {
            logWarn("Erro ao processar template HTML, usando texto simples: " + e.getMessage());
            // Fallback para texto simples se template falhar
            return buildVerificationEmailText(user, verificationUrl);
        }
    }

    /**
     * Extrai nome completo do usuário.
     * 
     * @param user usuário
     * @return nome completo ou primeiro nome se sobrenome não disponível
     */
    private String getFirstName(User user) {
        // Usar o método getFullName() da entidade User que já combina firstName e lastName
        if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
            return user.getFullName();
        }
        
        // Fallback para firstName se getFullName() não funcionar
        if (user.getFirstName() != null && !user.getFirstName().trim().isEmpty()) {
            return user.getFirstName();
        }
        
        return "Usuário";
    }

    /**
     * Constrói o HTML do email de recuperação de senha usando template Thymeleaf
     */
    private String buildPasswordResetEmailHtml(User user, String token) {
        Context context = new Context();
        context.setVariable("firstName", getFirstName(user));
        context.setVariable("token", token);
        context.setVariable("appName", appName);
        context.setVariable("expiryHours", "1");
        
        return templateEngine.process("password-reset", context);
    }

    /**
     * Testa conectividade do serviço de email.
     * 
     * @return true se conexão está funcionando
     */
    public boolean testEmailConnection() {
        if (!emailEnabled) {
            logInfo("Serviço de email está desabilitado");
            return false;
        }

        try {
            boolean connectionOk = smtpService.testConnection();
            if (connectionOk) {
                logInfo("Conexão com servidor de email testada com sucesso");
            } else {
                logWarn("Teste de conexão com servidor de email falhou");
            }
            return connectionOk;
        } catch (Exception e) {
            logError("Erro ao testar conexão com servidor de email: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Verifica se envio de email está habilitado.
     * 
     * @return true se habilitado
     */
    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    /**
     * Obtém estatísticas do serviço de email.
     * 
     * @return mapa com estatísticas
     */
    public Map<String, Object> getEmailStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("emailEnabled", emailEnabled);
        stats.put("fromEmail", fromEmail);
        stats.put("verificationBaseUrl", verificationBaseUrl);
        stats.put("appName", appName);
        stats.put("connectionTest", testEmailConnection());
        
        // Incluir estatísticas do SmtpService
        try {
            Map<String, Object> smtpStats = smtpService.getStatistics();
            stats.put("smtpStats", smtpStats);
        } catch (Exception e) {
            logWarn("Erro ao obter estatísticas do SMTP: " + e.getMessage());
            stats.put("smtpStats", "Erro ao obter estatísticas");
        }
        
        return stats;
    }
}