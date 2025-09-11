package com.sistema.java.service;

import com.sistema.java.model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.logging.Logger;

@Service
public class EmailService {
    
    private static final Logger logger = Logger.getLogger(EmailService.class.getName());
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.name:Sistema de Notícias}")
    private String appName;
    
    /**
     * Envia email de verificação para novo usuário
     */
    public void enviarEmailVerificacao(Usuario usuario) {
        try {
            String assunto = "Verificação de Email - " + appName;
            String linkVerificacao = baseUrl + "/verificar-email?token=" + usuario.getTokenVerificacao();
            
            Context context = new Context();
            context.setVariable("nomeUsuario", usuario.getNome());
            context.setVariable("linkVerificacao", linkVerificacao);
            context.setVariable("appName", appName);
            
            String conteudoHtml = templateEngine.process("email/verificacao", context);
            
            enviarEmailHtml(usuario.getEmail(), assunto, conteudoHtml);
            
            logger.info("Email de verificação enviado para: " + usuario.getEmail());
            
        } catch (Exception e) {
            logger.severe("Erro ao enviar email de verificação: " + e.getMessage());
            // Fallback para email simples
            enviarEmailVerificacaoSimples(usuario);
        }
    }
    
    /**
     * Envia email de reset de senha
     */
    public void enviarEmailResetSenha(Usuario usuario) {
        try {
            String assunto = "Reset de Senha - " + appName;
            String linkReset = baseUrl + "/reset-senha?token=" + usuario.getTokenResetSenha();
            
            Context context = new Context();
            context.setVariable("nomeUsuario", usuario.getNome());
            context.setVariable("linkReset", linkReset);
            context.setVariable("appName", appName);
            
            String conteudoHtml = templateEngine.process("email/reset-senha", context);
            
            enviarEmailHtml(usuario.getEmail(), assunto, conteudoHtml);
            
            logger.info("Email de reset de senha enviado para: " + usuario.getEmail());
            
        } catch (Exception e) {
            logger.severe("Erro ao enviar email de reset: " + e.getMessage());
            // Fallback para email simples
            enviarEmailResetSimples(usuario);
        }
    }
    
    /**
     * Envia email de boas-vindas
     */
    public void enviarEmailBoasVindas(Usuario usuario) {
        try {
            String assunto = "Bem-vindo ao " + appName + "!";
            
            Context context = new Context();
            context.setVariable("nomeUsuario", usuario.getNome());
            context.setVariable("appName", appName);
            context.setVariable("baseUrl", baseUrl);
            
            String conteudoHtml = templateEngine.process("email/boas-vindas", context);
            
            enviarEmailHtml(usuario.getEmail(), assunto, conteudoHtml);
            
            logger.info("Email de boas-vindas enviado para: " + usuario.getEmail());
            
        } catch (Exception e) {
            logger.severe("Erro ao enviar email de boas-vindas: " + e.getMessage());
        }
    }
    
    /**
     * Envia email de notificação de novo comentário
     */
    public void enviarNotificacaoComentario(Usuario autor, String tituloNoticia, String comentario) {
        try {
            String assunto = "Novo comentário em sua notícia - " + appName;
            
            Context context = new Context();
            context.setVariable("nomeAutor", autor.getNome());
            context.setVariable("tituloNoticia", tituloNoticia);
            context.setVariable("comentario", comentario);
            context.setVariable("appName", appName);
            context.setVariable("baseUrl", baseUrl);
            
            String conteudoHtml = templateEngine.process("email/novo-comentario", context);
            
            enviarEmailHtml(autor.getEmail(), assunto, conteudoHtml);
            
            logger.info("Notificação de comentário enviada para: " + autor.getEmail());
            
        } catch (Exception e) {
            logger.severe("Erro ao enviar notificação de comentário: " + e.getMessage());
        }
    }
    
    /**
     * Envia newsletter
     */
    public void enviarNewsletter(String email, String conteudo) {
        try {
            String assunto = "Newsletter - " + appName;
            
            Context context = new Context();
            context.setVariable("conteudo", conteudo);
            context.setVariable("appName", appName);
            context.setVariable("baseUrl", baseUrl);
            
            String conteudoHtml = templateEngine.process("email/newsletter", context);
            
            enviarEmailHtml(email, assunto, conteudoHtml);
            
            logger.info("Newsletter enviada para: " + email);
            
        } catch (Exception e) {
            logger.severe("Erro ao enviar newsletter: " + e.getMessage());
        }
    }
    
    // Métodos privados
    
    private void enviarEmailHtml(String para, String assunto, String conteudoHtml) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(para);
        helper.setSubject(assunto);
        helper.setText(conteudoHtml, true);
        
        mailSender.send(message);
    }
    
    private void enviarEmailSimples(String para, String assunto, String conteudo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(para);
        message.setSubject(assunto);
        message.setText(conteudo);
        
        mailSender.send(message);
    }
    
    private void enviarEmailVerificacaoSimples(Usuario usuario) {
        try {
            String assunto = "Verificação de Email - " + appName;
            String linkVerificacao = baseUrl + "/verificar-email?token=" + usuario.getTokenVerificacao();
            
            String conteudo = String.format(
                "Olá %s,\n\n" +
                "Obrigado por se cadastrar no %s!\n\n" +
                "Para verificar seu email, clique no link abaixo:\n" +
                "%s\n\n" +
                "Este link expira em 24 horas.\n\n" +
                "Se você não se cadastrou em nosso site, ignore este email.\n\n" +
                "Atenciosamente,\n" +
                "Equipe %s",
                usuario.getNome(), appName, linkVerificacao, appName
            );
            
            enviarEmailSimples(usuario.getEmail(), assunto, conteudo);
            
        } catch (Exception e) {
            logger.severe("Erro ao enviar email simples de verificação: " + e.getMessage());
        }
    }
    
    private void enviarEmailResetSimples(Usuario usuario) {
        try {
            String assunto = "Reset de Senha - " + appName;
            String linkReset = baseUrl + "/reset-senha?token=" + usuario.getTokenResetSenha();
            
            String conteudo = String.format(
                "Olá %s,\n\n" +
                "Você solicitou o reset de sua senha no %s.\n\n" +
                "Para criar uma nova senha, clique no link abaixo:\n" +
                "%s\n\n" +
                "Este link expira em 2 horas.\n\n" +
                "Se você não solicitou este reset, ignore este email.\n\n" +
                "Atenciosamente,\n" +
                "Equipe %s",
                usuario.getNome(), appName, linkReset, appName
            );
            
            enviarEmailSimples(usuario.getEmail(), assunto, conteudo);
            
        } catch (Exception e) {
            logger.severe("Erro ao enviar email simples de reset: " + e.getMessage());
        }
    }
}