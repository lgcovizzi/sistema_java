package com.sistema.service;

import com.sistema.config.SmtpConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmtpService Tests")
class SmtpServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SmtpConfig.SmtpConfiguration smtpConfiguration;

    @Mock
    private EmailConfigurationService emailConfigurationService;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private SmtpService smtpService;

    @BeforeEach
    void setUp() {
        // Configurar propriedades via reflection
        ReflectionTestUtils.setField(smtpService, "emailEnabled", true);
        ReflectionTestUtils.setField(smtpService, "asyncEnabled", true);
        ReflectionTestUtils.setField(smtpService, "retryAttempts", 3);
        ReflectionTestUtils.setField(smtpService, "retryDelay", 1000L);

        // Configurar mock do SmtpConfiguration - usando @Lenient pois nem todos os testes usam esses mocks
        lenient().when(smtpConfiguration.getUsername()).thenReturn("noreply@sistema.com");
        lenient().when(smtpConfiguration.getHost()).thenReturn("localhost");
        lenient().when(smtpConfiguration.getPort()).thenReturn(587);
        lenient().when(smtpConfiguration.getProtocol()).thenReturn("smtp");
        lenient().when(smtpConfiguration.isAuth()).thenReturn(true);
        lenient().when(smtpConfiguration.isStarttlsEnable()).thenReturn(true);
        lenient().when(smtpConfiguration.isSslEnable()).thenReturn(false);

        // Configurar comportamento do EmailConfigurationService
        lenient().when(emailConfigurationService.getDefaultConfiguration()).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Deve enviar email simples com sucesso")
    void shouldSendSimpleEmailSuccessfully() {
        // Given
        String to = "test@email.com";
        String subject = "Teste";
        String text = "Conteúdo do teste";

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        boolean result = smtpService.sendSimpleEmail(to, subject, text);

        // Then
        assertThat(result).isTrue();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve enviar email simples com remetente customizado")
    void shouldSendSimpleEmailWithCustomFrom() {
        // Given
        String from = "custom@email.com";
        String to = "test@email.com";
        String subject = "Teste";
        String text = "Conteúdo do teste";

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        boolean result = smtpService.sendSimpleEmail(from, to, subject, text);

        // Then
        assertThat(result).isTrue();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve retornar false quando email está desabilitado")
    void shouldReturnFalseWhenEmailIsDisabled() {
        // Given
        ReflectionTestUtils.setField(smtpService, "emailEnabled", false);
        String to = "test@email.com";
        String subject = "Teste";
        String text = "Conteúdo do teste";

        // When
        boolean result = smtpService.sendSimpleEmail(to, subject, text);

        // Then
        assertThat(result).isFalse();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando destinatário é nulo")
    void shouldThrowExceptionWhenToIsNull() {
        // Given
        String to = null;
        String subject = "Teste";
        String text = "Conteúdo do teste";

        // When & Then
        assertThatThrownBy(() -> smtpService.sendSimpleEmail(to, subject, text))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("to");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando assunto é vazio")
    void shouldThrowExceptionWhenSubjectIsEmpty() {
        // Given
        String to = "test@email.com";
        String subject = "";
        String text = "Conteúdo do teste";

        // When & Then
        assertThatThrownBy(() -> smtpService.sendSimpleEmail(to, subject, text))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subject");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando texto é nulo")
    void shouldThrowExceptionWhenTextIsNull() {
        // Given
        String to = "test@email.com";
        String subject = "Teste";
        String text = null;

        // When & Then
        assertThatThrownBy(() -> smtpService.sendSimpleEmail(to, subject, text))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("text");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve retornar false quando ocorre MailException")
    void shouldReturnFalseWhenMailExceptionOccurs() {
        // Given
        String to = "test@email.com";
        String subject = "Teste";
        String text = "Conteúdo do teste";

        doThrow(new MailException("Erro de envio") {}).when(mailSender).send(any(SimpleMailMessage.class));

        // When
        boolean result = smtpService.sendSimpleEmail(to, subject, text);

        // Then
        assertThat(result).isFalse();
        verify(mailSender, atLeast(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve enviar email HTML com sucesso")
    void shouldSendHtmlEmailSuccessfully() throws MessagingException {
        // Given
        String to = "test@email.com";
        String subject = "Teste HTML";
        String htmlContent = "<h1>Teste</h1>";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When
        boolean result = smtpService.sendHtmlEmail(to, subject, htmlContent);

        // Then
        assertThat(result).isTrue();
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Deve enviar email HTML com remetente customizado")
    void shouldSendHtmlEmailWithCustomFrom() throws MessagingException {
        // Given
        String from = "custom@email.com";
        String to = "test@email.com";
        String subject = "Teste HTML";
        String htmlContent = "<h1>Teste</h1>";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When
        boolean result = smtpService.sendHtmlEmail(from, to, subject, htmlContent);

        // Then
        assertThat(result).isTrue();
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Deve retornar false quando email HTML está desabilitado")
    void shouldReturnFalseWhenHtmlEmailIsDisabled() {
        // Given
        ReflectionTestUtils.setField(smtpService, "emailEnabled", false);
        String to = "test@email.com";
        String subject = "Teste HTML";
        String htmlContent = "<h1>Teste</h1>";

        // When
        boolean result = smtpService.sendHtmlEmail(to, subject, htmlContent);

        // Then
        assertThat(result).isFalse();
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Deve retornar false quando ocorre MailException no HTML")
    void shouldReturnFalseWhenMailExceptionOccursInHtml() throws MessagingException {
        // Given
        String to = "test@email.com";
        String subject = "Teste HTML";
        String htmlContent = "<h1>Teste</h1>";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailException("Erro de envio") {}).when(mailSender).send(any(MimeMessage.class));

        // When
        boolean result = smtpService.sendHtmlEmail(to, subject, htmlContent);

        // Then
        assertThat(result).isFalse();
        verify(mailSender).createMimeMessage();
        verify(mailSender, atLeast(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Deve enviar email simples assíncrono com sucesso")
    void shouldSendSimpleEmailAsyncSuccessfully() throws ExecutionException, InterruptedException {
        // Given
        String to = "test@email.com";
        String subject = "Teste Async";
        String text = "Conteúdo do teste";

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        CompletableFuture<Boolean> future = smtpService.sendSimpleEmailAsync(to, subject, text);
        Boolean result = future.get();

        // Then
        assertThat(result).isTrue();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve enviar email simples assíncrono com remetente customizado")
    void shouldSendSimpleEmailAsyncWithCustomFrom() throws ExecutionException, InterruptedException {
        // Given
        String from = "custom@email.com";
        String to = "test@email.com";
        String subject = "Teste Async";
        String text = "Conteúdo do teste";

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        CompletableFuture<Boolean> future = smtpService.sendSimpleEmailAsync(from, to, subject, text);
        Boolean result = future.get();

        // Then
        assertThat(result).isTrue();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve usar envio síncrono quando async está desabilitado")
    void shouldUseSyncWhenAsyncIsDisabled() throws ExecutionException, InterruptedException {
        // Given
        ReflectionTestUtils.setField(smtpService, "asyncEnabled", false);
        String to = "test@email.com";
        String subject = "Teste";
        String text = "Conteúdo do teste";

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        CompletableFuture<Boolean> future = smtpService.sendSimpleEmailAsync(to, subject, text);
        Boolean result = future.get();

        // Then
        assertThat(result).isTrue();
        assertThat(future.isDone()).isTrue();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve enviar email HTML assíncrono com sucesso")
    void shouldSendHtmlEmailAsyncSuccessfully() throws ExecutionException, InterruptedException, MessagingException {
        // Given
        String to = "test@email.com";
        String subject = "Teste HTML Async";
        String htmlContent = "<h1>Teste</h1>";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When
        CompletableFuture<Boolean> future = smtpService.sendHtmlEmailAsync(to, subject, htmlContent);
        Boolean result = future.get();

        // Then
        assertThat(result).isTrue();
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Deve testar conexão com sucesso")
    void shouldTestConnectionSuccessfully() {
        // Given
        // O método testConnection usa sendSimpleEmail que usa mailSender.send()

        // When
        boolean result = smtpService.testConnection();

        // Then
        assertThat(result).isTrue();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve retornar false quando teste de conexão falha")
    void shouldReturnFalseWhenConnectionTestFails() {
        // Given
        doThrow(new RuntimeException("Connection failed")).when(mailSender).send(any(SimpleMailMessage.class));

        // When
        boolean result = smtpService.testConnection();

        // Then
        assertThat(result).isFalse();
        // O sistema de retry tenta 3 vezes por padrão
        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve obter estatísticas SMTP")
    void shouldGetSmtpStats() {
        // When
        SmtpService.SmtpStats stats = smtpService.getSmtpStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.isEmailEnabled()).isTrue();
        assertThat(stats.isAsyncEnabled()).isTrue();
        assertThat(stats.getHost()).isEqualTo("localhost");
        assertThat(stats.getPort()).isEqualTo(587);
        assertThat(stats.getProtocol()).isEqualTo("smtp");
        assertThat(stats.isAuth()).isTrue();
        assertThat(stats.isStarttlsEnable()).isTrue();
        assertThat(stats.isSslEnable()).isFalse();
        assertThat(stats.getRetryAttempts()).isEqualTo(3);
        assertThat(stats.getRetryDelay()).isEqualTo(1000L);
        assertThat(stats.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Deve obter estatísticas como mapa")
    void shouldGetStatisticsAsMap() {
        // When
        Map<String, Object> stats = smtpService.getStatistics();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.get("emailEnabled")).isEqualTo(true);
        assertThat(stats.get("asyncEnabled")).isEqualTo(true);
        assertThat(stats.get("host")).isEqualTo("localhost");
        assertThat(stats.get("port")).isEqualTo(587);
        assertThat(stats.get("protocol")).isEqualTo("smtp");
        assertThat(stats.get("auth")).isEqualTo(true);
        assertThat(stats.get("starttlsEnable")).isEqualTo(true);
        assertThat(stats.get("sslEnable")).isEqualTo(false);
        assertThat(stats.get("retryAttempts")).isEqualTo(3);
        assertThat(stats.get("retryDelay")).isEqualTo(1000L);
        assertThat(stats.get("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("Deve verificar se email está habilitado")
    void shouldCheckIfEmailIsEnabled() {
        // When
        boolean result = smtpService.isEmailEnabled();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Deve verificar se async está habilitado")
    void shouldCheckIfAsyncIsEnabled() {
        // When
        boolean result = smtpService.isAsyncEnabled();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Deve fazer retry em caso de falha temporária")
    void shouldRetryOnTemporaryFailure() {
        // Given
        String to = "test@email.com";
        String subject = "Teste";
        String text = "Conteúdo do teste";

        // Simular falha nas duas primeiras tentativas e sucesso na terceira
        doThrow(new MailException("Erro temporário") {})
                .doThrow(new MailException("Erro temporário") {})
                .doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));

        // When
        boolean result = smtpService.sendSimpleEmail(to, subject, text);

        // Then
        assertThat(result).isTrue();
        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve falhar após esgotar tentativas de retry")
    void shouldFailAfterExhaustingRetryAttempts() {
        // Given
        String to = "test@email.com";
        String subject = "Teste";
        String text = "Conteúdo do teste";

        doThrow(new MailException("Erro persistente") {}).when(mailSender).send(any(SimpleMailMessage.class));

        // When
        boolean result = smtpService.sendSimpleEmail(to, subject, text);

        // Then
        assertThat(result).isFalse();
        verify(mailSender, times(3)).send(any(SimpleMailMessage.class)); // 3 tentativas
    }

    @Test
    @DisplayName("Deve validar entrada vazia")
    void shouldValidateEmptyInput() {
        // Given
        String to = "   ";
        String subject = "Teste";
        String text = "Conteúdo";

        // When & Then
        assertThatThrownBy(() -> smtpService.sendSimpleEmail(to, subject, text))
                .isInstanceOf(IllegalArgumentException.class);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve tratar exceção assíncrona")
    void shouldHandleAsyncException() throws ExecutionException, InterruptedException {
        // Given
        String to = "test@email.com";
        String subject = "Teste";
        String text = "Conteúdo do teste";

        doThrow(new MailException("Erro async") {}).when(mailSender).send(any(SimpleMailMessage.class));

        // When
        CompletableFuture<Boolean> future = smtpService.sendSimpleEmailAsync(to, subject, text);
        Boolean result = future.get();

        // Then
        assertThat(result).isFalse();
        verify(mailSender, atLeast(1)).send(any(SimpleMailMessage.class));
    }
}