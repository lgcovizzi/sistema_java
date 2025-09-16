package com.sistema.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Serviço para envio de emails usando Mailtrap
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Envia um email simples
     * 
     * @param to Destinatário
     * @param subject Assunto
     * @param text Corpo do email
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@sistema.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            logger.info("Email enviado com sucesso para: {}", to);
        } catch (Exception e) {
            logger.error("Erro ao enviar email para: {}", to, e);
            throw new RuntimeException("Falha ao enviar email", e);
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
     * Envia email de recuperação de senha
     * 
     * @param to Email do usuário
     * @param resetToken Token de recuperação
     */
    public void sendPasswordResetEmail(String to, String resetToken) {
        String subject = "Recuperação de Senha - Sistema Java";
        String text = String.format(
            "Você solicitou a recuperação de sua senha.\n\n" +
            "Use o token abaixo para redefinir sua senha:\n" +
            "%s\n\n" +
            "Este token é válido por 24 horas.\n\n" +
            "Se você não solicitou esta recuperação, ignore este email.\n\n" +
            "Atenciosamente,\n" +
            "Equipe Sistema Java",
            resetToken
        );
        
        sendSimpleEmail(to, subject, text);
    }
}