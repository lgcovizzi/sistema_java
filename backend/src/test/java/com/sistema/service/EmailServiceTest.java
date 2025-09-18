package com.sistema.service;

import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @Mock
    private SmtpService smtpService;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailService emailService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("João");
        testUser.setLastName("Silva");
        testUser.setEmail("joao@email.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.USER);
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());

        // Configurar propriedades via reflection
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@sistema.com");
        ReflectionTestUtils.setField(emailService, "verificationBaseUrl", "http://localhost:8080/api/auth/verify-email");
        ReflectionTestUtils.setField(emailService, "appName", "Sistema Java");
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
    }

    @Test
    @DisplayName("Deve enviar email simples com sucesso")
    void shouldSendSimpleEmailSuccessfully() {
        // Given
        String to = "test@email.com";
        String subject = "Teste";
        String text = "Conteúdo do teste";

        when(smtpService.sendSimpleEmail(to, subject, text)).thenReturn(true);

        // When
        assertThatCode(() -> emailService.sendSimpleEmail(to, subject, text))
                .doesNotThrowAnyException();

        // Then
        verify(smtpService).sendSimpleEmail(to, subject, text);
    }

    @Test
    @DisplayName("Deve lançar exceção quando destinatário é nulo")
    void shouldThrowExceptionWhenToIsNull() {
        // Given
        String to = null;
        String subject = "Teste";
        String text = "Conteúdo do teste";

        // When & Then
        assertThatThrownBy(() -> emailService.sendSimpleEmail(to, subject, text))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("to");

        verify(smtpService, never()).sendSimpleEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção quando assunto é vazio")
    void shouldThrowExceptionWhenSubjectIsEmpty() {
        // Given
        String to = "test@email.com";
        String subject = "";
        String text = "Conteúdo do teste";

        // When & Then
        assertThatThrownBy(() -> emailService.sendSimpleEmail(to, subject, text))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subject");

        verify(smtpService, never()).sendSimpleEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção quando texto é nulo")
    void shouldThrowExceptionWhenTextIsNull() {
        // Given
        String to = "test@email.com";
        String subject = "Teste";
        String text = null;

        // When & Then
        assertThatThrownBy(() -> emailService.sendSimpleEmail(to, subject, text))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("text");

        verify(smtpService, never()).sendSimpleEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve propagar exceção do SmtpService")
    void shouldPropagateSmtpServiceException() {
        // Given
        String to = "test@email.com";
        String subject = "Teste";
        String text = "Conteúdo do teste";

        when(smtpService.sendSimpleEmail(to, subject, text)).thenThrow(new RuntimeException("Erro SMTP"));

        // When & Then
        assertThatThrownBy(() -> emailService.sendSimpleEmail(to, subject, text))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha ao enviar email");

        verify(smtpService).sendSimpleEmail(to, subject, text);
    }

    @Test
    @DisplayName("Deve enviar email HTML com sucesso")
    void shouldSendHtmlEmailSuccessfully() {
        // Given
        String to = "test@email.com";
        String subject = "Teste HTML";
        String htmlContent = "<h1>Teste</h1>";

        when(smtpService.sendHtmlEmail(to, subject, htmlContent)).thenReturn(true);

        // When
        assertThatCode(() -> emailService.sendHtmlEmail(to, subject, htmlContent))
                .doesNotThrowAnyException();

        // Then
        verify(smtpService).sendHtmlEmail(to, subject, htmlContent);
    }

    @Test
    @DisplayName("Deve lançar exceção quando conteúdo HTML é nulo")
    void shouldThrowExceptionWhenHtmlContentIsNull() {
        // Given
        String to = "test@email.com";
        String subject = "Teste HTML";
        String htmlContent = null;

        // When & Then
        assertThatThrownBy(() -> emailService.sendHtmlEmail(to, subject, htmlContent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("htmlContent");

        verify(smtpService, never()).sendHtmlEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve enviar email de teste com sucesso")
    void shouldSendTestEmailSuccessfully() {
        // Given
        String to = "test@email.com";

        when(smtpService.sendSimpleEmail(eq(to), eq("Teste de Configuração - Sistema Java"), anyString())).thenReturn(true);

        // When
        assertThatCode(() -> emailService.sendTestEmail(to))
                .doesNotThrowAnyException();

        // Then
        verify(smtpService).sendSimpleEmail(eq(to), eq("Teste de Configuração - Sistema Java"), anyString());
    }

    @Test
    @DisplayName("Deve enviar email de boas-vindas com sucesso")
    void shouldSendWelcomeEmailSuccessfully() {
        // Given
        String to = "test@email.com";
        String name = "João Silva";

        when(smtpService.sendSimpleEmail(eq(to), eq("Bem-vindo ao Sistema Java!"), anyString())).thenReturn(true);

        // When
        assertThatCode(() -> emailService.sendWelcomeEmail(to, name))
                .doesNotThrowAnyException();

        // Then
        verify(smtpService).sendSimpleEmail(eq(to), eq("Bem-vindo ao Sistema Java!"), contains(name));
    }

    @Test
    @DisplayName("Deve enviar email de recuperação de senha com sucesso")
    void shouldSendPasswordResetEmailSuccessfully() {
        // Given
        String resetToken = "abc123token";
        String expectedHtmlContent = "<html>Password reset content</html>";

        when(templateEngine.process(eq("password-reset"), any(Context.class))).thenReturn(expectedHtmlContent);
        doNothing().when(smtpService).sendHtmlEmail(eq(testUser.getEmail()), anyString(), eq(expectedHtmlContent));

        // When
        assertThatCode(() -> emailService.sendPasswordResetEmail(testUser, resetToken))
                .doesNotThrowAnyException();

        // Then
        verify(templateEngine).process(eq("password-reset"), any(Context.class));
        verify(smtpService).sendHtmlEmail(eq(testUser.getEmail()), contains("Recuperação de Senha"), eq(expectedHtmlContent));
    }

    @Test
    @DisplayName("Deve enviar email de verificação com sucesso")
    void shouldSendVerificationEmailSuccessfully() {
        // Given
        String verificationToken = "verification123";
        String htmlTemplate = "<html><body>Verificação: verification123</body></html>";

        when(templateEngine.process(eq("email/verification"), any(Context.class)))
                .thenReturn(htmlTemplate);
        when(smtpService.sendHtmlEmail(eq(testUser.getEmail()), anyString(), eq(htmlTemplate))).thenReturn(true);

        // When
        assertThatCode(() -> emailService.sendVerificationEmail(testUser, verificationToken))
                .doesNotThrowAnyException();

        // Then
        verify(templateEngine).process(eq("email/verification"), any(Context.class));
        verify(smtpService).sendHtmlEmail(eq(testUser.getEmail()), contains("Verificação"), eq(htmlTemplate));
    }

    @Test
    @DisplayName("Deve enviar email de verificação com template personalizado")
    void shouldSendVerificationEmailWithCustomTemplate() {
        // Given
        String verificationToken = "verification123";
        String htmlTemplate = "<html><body>Verificação: verification123</body></html>";

        when(templateEngine.process(eq("email/verification"), any(Context.class)))
                .thenReturn(htmlTemplate);
        when(smtpService.sendHtmlEmail(eq(testUser.getEmail()), anyString(), eq(htmlTemplate))).thenReturn(true);

        // When
        boolean result = emailService.sendVerificationEmail(testUser, verificationToken);

        // Then
        assertThat(result).isTrue();
        verify(templateEngine).process(eq("email/verification"), any(Context.class));
        verify(smtpService).sendHtmlEmail(eq(testUser.getEmail()), contains("Verificação"), eq(htmlTemplate));
    }

    @Test
    @DisplayName("Deve testar conexão de email com sucesso")
    void shouldTestEmailConnectionSuccessfully() {
        // Given
        when(smtpService.testConnection()).thenReturn(true);

        // When
        boolean result = emailService.testEmailConnection();

        // Then
        assertThat(result).isTrue();
        verify(smtpService).testConnection();
    }

    @Test
    @DisplayName("Deve retornar false quando teste de conexão falha")
    void shouldReturnFalseWhenConnectionTestFails() {
        // Given
        when(smtpService.testConnection()).thenReturn(false);

        // When
        boolean result = emailService.testEmailConnection();

        // Then
        assertThat(result).isFalse();
        verify(smtpService).testConnection();
    }

    @Test
    @DisplayName("Deve retornar false quando email está desabilitado")
    void shouldReturnFalseWhenEmailIsDisabled() {
        // Given
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);

        // When
        boolean result = emailService.testEmailConnection();

        // Then
        assertThat(result).isFalse();
        verify(smtpService, never()).testConnection();
    }

    @Test
    @DisplayName("Deve tratar exceção no teste de conexão")
    void shouldHandleExceptionInConnectionTest() {
        // Given
        when(smtpService.testConnection()).thenThrow(new RuntimeException("Erro de conexão"));

        // When
        boolean result = emailService.testEmailConnection();

        // Then
        assertThat(result).isFalse();
        verify(smtpService).testConnection();
    }

    @Test
    @DisplayName("Deve verificar se email está habilitado")
    void shouldCheckIfEmailIsEnabled() {
        // When
        boolean result = emailService.isEmailEnabled();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Deve obter estatísticas do email")
    void shouldGetEmailStats() {
        // Given
        Map<String, Object> smtpStats = Map.of("host", "localhost", "port", 587);
        when(smtpService.getStatistics()).thenReturn(smtpStats);
        when(smtpService.testConnection()).thenReturn(true);

        // When
        Map<String, Object> stats = emailService.getEmailStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.get("emailEnabled")).isEqualTo(true);
        assertThat(stats.get("fromEmail")).isEqualTo("noreply@sistema.com");
        assertThat(stats.get("verificationBaseUrl")).isEqualTo("http://localhost:8080/api/auth/verify-email");
        assertThat(stats.get("appName")).isEqualTo("Sistema Java");
        assertThat(stats.get("connectionTest")).isEqualTo(true);
        assertThat(stats.get("smtpStats")).isEqualTo(smtpStats);

        verify(smtpService).getStatistics();
        verify(smtpService).testConnection();
    }

    @Test
    @DisplayName("Deve tratar erro ao obter estatísticas SMTP")
    void shouldHandleErrorWhenGettingSmtpStats() {
        // Given
        when(smtpService.getStatistics()).thenThrow(new RuntimeException("Erro SMTP"));
        when(smtpService.testConnection()).thenReturn(true);

        // When
        Map<String, Object> stats = emailService.getEmailStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.get("emailEnabled")).isEqualTo(true);
        assertThat(stats.get("smtpStats")).isEqualTo("Erro ao obter estatísticas");

        verify(smtpService).getStatistics();
    }

    @Test
    @DisplayName("Deve validar entrada vazia para sendSimpleEmail")
    void shouldValidateEmptyInputForSendSimpleEmail() {
        // Given
        String to = "   ";
        String subject = "Teste";
        String text = "Conteúdo";

        // When & Then
        assertThatThrownBy(() -> emailService.sendSimpleEmail(to, subject, text))
                .isInstanceOf(IllegalArgumentException.class);

        verify(smtpService, never()).sendSimpleEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve validar entrada vazia para sendHtmlEmail")
    void shouldValidateEmptyInputForSendHtmlEmail() {
        // Given
        String to = "test@email.com";
        String subject = "   ";
        String htmlContent = "<h1>Teste</h1>";

        // When & Then
        assertThatThrownBy(() -> emailService.sendHtmlEmail(to, subject, htmlContent))
                .isInstanceOf(IllegalArgumentException.class);

        verify(smtpService, never()).sendHtmlEmail(anyString(), anyString(), anyString());
    }
}