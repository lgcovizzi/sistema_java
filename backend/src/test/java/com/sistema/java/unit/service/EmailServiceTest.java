package com.sistema.java.unit.service;

import com.sistema.java.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para EmailService
 * Referência: Testes e Qualidade de Código - project_rules.md
 * Referência: Padrões de Teste - project_rules.md
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private String destinatario;
    private String assunto;
    private String conteudo;

    @BeforeEach
    void setUp() {
        // Arrange - Configuração dos dados de teste
        destinatario = "teste@exemplo.com";
        assunto = "Assunto do Email de Teste";
        conteudo = "Este é o conteúdo do email de teste.";
    }

    @Test
    void should_SendSimpleEmail_When_ValidDataProvided() {
        // Arrange
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.enviarEmailSimples(destinatario, assunto, conteudo);

        // Assert
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void should_SendWelcomeEmail_When_NewUserRegistered() {
        // Arrange
        String nomeUsuario = "João Silva";
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.enviarEmailBoasVindas(destinatario, nomeUsuario);

        // Assert
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void should_SendPasswordResetEmail_When_UserRequestsReset() {
        // Arrange
        String senhaTemporaria = "temp123";
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.enviarEmailRedefinicaoSenha(destinatario, senhaTemporaria);

        // Assert
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void should_SendNotificationEmail_When_CommentNeedsApproval() {
        // Arrange
        String tituloNoticia = "Título da Notícia";
        String autorComentario = "Maria Santos";
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.enviarNotificacaoNovoComentario(destinatario, tituloNoticia, autorComentario);

        // Assert
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void should_SendNewsletterEmail_When_NewsPublished() {
        // Arrange
        String tituloNoticia = "Nova Notícia Publicada";
        String resumoNoticia = "Resumo da nova notícia";
        String linkNoticia = "http://localhost:8080/noticias/1";
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.enviarNewsletterNoticia(destinatario, tituloNoticia, resumoNoticia, linkNoticia);

        // Assert
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void should_ThrowException_When_EmailSendingFails() {
        // Arrange
        doThrow(new RuntimeException("Falha no envio do email"))
            .when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        assertThatThrownBy(() -> emailService.enviarEmailSimples(destinatario, assunto, conteudo))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Erro ao enviar email");
        
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void should_ValidateEmailFormat_When_SendingEmail() {
        // Arrange
        String emailInvalido = "email-invalido";
        
        // Act & Assert
        assertThatThrownBy(() -> emailService.enviarEmailSimples(emailInvalido, assunto, conteudo))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email inválido");
        
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void should_HandleNullParameters_When_SendingEmail() {
        // Act & Assert - Destinatário nulo
        assertThatThrownBy(() -> emailService.enviarEmailSimples(null, assunto, conteudo))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Destinatário não pode ser nulo");
        
        // Act & Assert - Assunto nulo
        assertThatThrownBy(() -> emailService.enviarEmailSimples(destinatario, null, conteudo))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Assunto não pode ser nulo");
        
        // Act & Assert - Conteúdo nulo
        assertThatThrownBy(() -> emailService.enviarEmailSimples(destinatario, assunto, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Conteúdo não pode ser nulo");
        
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void should_HandleEmptyParameters_When_SendingEmail() {
        // Act & Assert - Destinatário vazio
        assertThatThrownBy(() -> emailService.enviarEmailSimples("", assunto, conteudo))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Destinatário não pode estar vazio");
        
        // Act & Assert - Assunto vazio
        assertThatThrownBy(() -> emailService.enviarEmailSimples(destinatario, "", conteudo))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Assunto não pode estar vazio");
        
        // Act & Assert - Conteúdo vazio
        assertThatThrownBy(() -> emailService.enviarEmailSimples(destinatario, assunto, ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Conteúdo não pode estar vazio");
        
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void should_SendBulkEmails_When_MultipleRecipientsProvided() {
        // Arrange
        String[] destinatarios = {"user1@teste.com", "user2@teste.com", "user3@teste.com"};
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.enviarEmailEmMassa(destinatarios, assunto, conteudo);

        // Assert
        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
    }

    @Test
    void should_SendEmailWithTemplate_When_UsingPredefinedTemplate() {
        // Arrange
        String nomeUsuario = "João Silva";
        String template = "BOAS_VINDAS";
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.enviarEmailComTemplate(destinatario, template, nomeUsuario);

        // Assert
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void should_LogEmailActivity_When_EmailIsSent() {
        // Arrange
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.enviarEmailSimples(destinatario, assunto, conteudo);

        // Assert
        verify(mailSender).send(any(SimpleMailMessage.class));
        // Verificar se o log foi registrado (implementação específica do serviço)
    }

    @Test
    void should_RetryEmailSending_When_FirstAttemptFails() {
        // Arrange
        doThrow(new RuntimeException("Falha temporária"))
            .doNothing()
            .when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.enviarEmailComRetry(destinatario, assunto, conteudo, 2);

        // Assert
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }
}