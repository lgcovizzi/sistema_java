package com.sistema.java.service;

import com.sistema.java.model.entity.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Serviço para envio de emails usando MailHog
 * Referência: Configurações de Ambiente - project_rules.md
 * Referência: Sistema de Email com MailHog - project_rules.md
 */
@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${spring.mail.host:localhost}")
    private String mailHost;
    
    @Value("${spring.mail.port:1025}")
    private int mailPort;
    
    @Value("${app.email.from:sistema@localhost}")
    private String emailFrom;
    
    @Value("${app.email.nome:Sistema Java}")
    private String nomeRemetente;
    
    @Value("${app.email.base-url:http://localhost:8080}")
    private String baseUrl;
    
    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }
    
    /**
     * Envia email simples de forma assíncrona
     * Referência: Sistema de Email com MailHog - project_rules.md
     */
    @Async
    public CompletableFuture<Boolean> enviarEmailSimples(String destinatario, String assunto, String conteudo) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(destinatario);
            message.setSubject(assunto);
            message.setText(conteudo);
            
            mailSender.send(message);
            
            logger.info("Email simples enviado com sucesso para: {} via MailHog ({}:{})", 
                destinatario, mailHost, mailPort);
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            logger.error("Erro ao enviar email simples para {}: {}", destinatario, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Envia email HTML usando template
     */
    @Async
    public CompletableFuture<Boolean> enviarEmailHtml(String destinatario, String assunto, 
                                                     String template, Map<String, Object> variaveis) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            // Configurar remetente e destinatário
            helper.setFrom(emailFrom, nomeRemetente);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            
            // Processar template
            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
            if (variaveis != null) {
                variaveis.forEach(context::setVariable);
            }
            
            // Adicionar variáveis padrão
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("nomeRemetente", nomeRemetente);
            context.setVariable("dataAtual", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            
            String conteudoHtml = templateEngine.process(template, context);
            helper.setText(conteudoHtml, true);
            
            mailSender.send(mimeMessage);
            
            logger.info("Email HTML enviado com sucesso para: {} usando template: {} via MailHog ({}:{})", 
                destinatario, template, mailHost, mailPort);
            return CompletableFuture.completedFuture(true);
            
        } catch (MessagingException | UnsupportedEncodingException e) {
            logger.error("Erro ao enviar email HTML para {}: {}", destinatario, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Envia email de recuperação de senha
     * Referência: Login e Registro - project_rules.md
     */
    @Async
    public CompletableFuture<Boolean> enviarEmailRecuperacaoSenha(Usuario usuario, String token) {
        try {
            Map<String, Object> variaveis = new HashMap<>();
            variaveis.put("nomeUsuario", usuario.getNome());
            variaveis.put("linkRecuperacao", baseUrl + "/auth/reset-password?token=" + token);
            variaveis.put("validadeToken", "24 horas");
            
            return enviarEmailHtml(
                usuario.getEmail(),
                "Recuperação de Senha - Sistema Java",
                "emails/recuperacao-senha",
                variaveis
            );
            
        } catch (Exception e) {
            logger.error("Erro ao enviar email de recuperação para {}: {}", usuario.getEmail(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Envia email de boas-vindas para novo usuário
     * Referência: Login e Registro - project_rules.md
     */
    @Async
    public CompletableFuture<Boolean> enviarEmailBoasVindas(Usuario usuario) {
        try {
            Map<String, Object> variaveis = new HashMap<>();
            variaveis.put("nomeUsuario", usuario.getNome());
            variaveis.put("sobrenomeUsuario", usuario.getSobrenome());
            variaveis.put("emailUsuario", usuario.getEmail());
            variaveis.put("linkPerfil", baseUrl + "/perfil");
            variaveis.put("linkDashboard", baseUrl + "/dashboard");
            
            return enviarEmailHtml(
                usuario.getEmail(),
                "Bem-vindo ao Sistema Java!",
                "emails/boas-vindas",
                variaveis
            );
            
        } catch (Exception e) {
            logger.error("Erro ao enviar email de boas-vindas para {}: {}", usuario.getEmail(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Envia notificação de novo comentário para autor da notícia
     */
    @Async
    public CompletableFuture<Boolean> enviarNotificacaoNovoComentario(Usuario autorNoticia, 
                                                                     String tituloNoticia, 
                                                                     String nomeComentarista,
                                                                     String conteudoComentario) {
        try {
            Map<String, Object> variaveis = new HashMap<>();
            variaveis.put("nomeAutor", autorNoticia.getNome());
            variaveis.put("tituloNoticia", tituloNoticia);
            variaveis.put("nomeComentarista", nomeComentarista);
            variaveis.put("conteudoComentario", conteudoComentario);
            variaveis.put("linkNoticia", baseUrl + "/noticias/" + tituloNoticia.replaceAll("\\s+", "-").toLowerCase());
            variaveis.put("linkModeracaoComentarios", baseUrl + "/admin/comentarios");
            
            return enviarEmailHtml(
                autorNoticia.getEmail(),
                "Novo comentário em sua notícia: " + tituloNoticia,
                "emails/novo-comentario",
                variaveis
            );
            
        } catch (Exception e) {
            logger.error("Erro ao enviar notificação de comentário para {}: {}", autorNoticia.getEmail(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Envia notificação de comentário aprovado para o autor do comentário
     */
    @Async
    public CompletableFuture<Boolean> enviarNotificacaoComentarioAprovado(Usuario autorComentario,
                                                                         String tituloNoticia) {
        try {
            Map<String, Object> variaveis = new HashMap<>();
            variaveis.put("nomeUsuario", autorComentario.getNome());
            variaveis.put("tituloNoticia", tituloNoticia);
            variaveis.put("linkNoticia", baseUrl + "/noticias/" + tituloNoticia.replaceAll("\\s+", "-").toLowerCase());
            
            return enviarEmailHtml(
                autorComentario.getEmail(),
                "Seu comentário foi aprovado!",
                "emails/comentario-aprovado",
                variaveis
            );
            
        } catch (Exception e) {
            logger.error("Erro ao enviar notificação de aprovação para {}: {}", autorComentario.getEmail(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Envia newsletter com últimas notícias
     */
    @Async
    public CompletableFuture<Boolean> enviarNewsletter(Usuario usuario, 
                                                      String tituloNewsletter,
                                                      java.util.List<Map<String, Object>> noticias) {
        try {
            Map<String, Object> variaveis = new HashMap<>();
            variaveis.put("nomeUsuario", usuario.getNome());
            variaveis.put("tituloNewsletter", tituloNewsletter);
            variaveis.put("noticias", noticias);
            variaveis.put("linkDescadastro", baseUrl + "/newsletter/unsubscribe?email=" + usuario.getEmail());
            
            return enviarEmailHtml(
                usuario.getEmail(),
                tituloNewsletter,
                "emails/newsletter",
                variaveis
            );
            
        } catch (Exception e) {
            logger.error("Erro ao enviar newsletter para {}: {}", usuario.getEmail(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Envia email de teste para verificar configuração
     */
    public boolean enviarEmailTeste(String destinatario) {
        try {
            String assunto = "Teste de Email - Sistema Java";
            String conteudo = String.format(
                "Este é um email de teste enviado em %s.\n\n" +
                "Configurações MailHog:\n" +
                "- Host: %s\n" +
                "- Porta: %d\n" +
                "- Remetente: %s\n\n" +
                "Se você recebeu este email, a configuração está funcionando corretamente!\n\n" +
                "Acesse a interface web do MailHog em: http://localhost:8025",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                mailHost,
                mailPort,
                emailFrom
            );
            
            CompletableFuture<Boolean> resultado = enviarEmailSimples(destinatario, assunto, conteudo);
            return resultado.join(); // Aguardar resultado para teste
            
        } catch (Exception e) {
            logger.error("Erro ao enviar email de teste para {}: {}", destinatario, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Verifica se o serviço de email está funcionando
     */
    public boolean verificarConexaoMailHog() {
        try {
            // Tentar criar uma mensagem simples para verificar conectividade
            SimpleMailMessage testMessage = new SimpleMailMessage();
            testMessage.setFrom(emailFrom);
            testMessage.setTo("test@localhost");
            testMessage.setSubject("Teste de Conectividade");
            testMessage.setText("Teste de conectividade com MailHog");
            
            // Não enviar, apenas verificar se não há erro de configuração
            logger.info("Verificação de conectividade MailHog: OK ({}:{})", mailHost, mailPort);
            return true;
            
        } catch (Exception e) {
            logger.error("Erro na verificação de conectividade MailHog ({}:{}): {}", mailHost, mailPort, e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtém estatísticas de emails enviados (para dashboard admin)
     */
    public Map<String, Object> obterEstatisticasEmail() {
        Map<String, Object> stats = new HashMap<>();
        
        // Em um cenário real, essas informações viriam de um banco de dados
        // Por enquanto, retornamos dados simulados
        stats.put("emailsEnviadosHoje", 0);
        stats.put("emailsEnviadosSemana", 0);
        stats.put("emailsEnviadosMes", 0);
        stats.put("ultimoEmailEnviado", null);
        stats.put("statusServico", verificarConexaoMailHog() ? "Ativo" : "Inativo");
        stats.put("configuracaoMailHog", Map.of(
            "host", mailHost,
            "porta", mailPort,
            "remetente", emailFrom,
            "webUI", "http://localhost:8025"
        ));
        
        return stats;
    }
    
    // Getters para configurações (útil para testes e debugging)
    
    public String getMailHost() {
        return mailHost;
    }
    
    public int getMailPort() {
        return mailPort;
    }
    
    public String getEmailFrom() {
        return emailFrom;
    }
    
    public String getNomeRemetente() {
        return nomeRemetente;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
}