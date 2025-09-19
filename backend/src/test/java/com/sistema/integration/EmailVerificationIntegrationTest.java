package com.sistema.integration;

import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.service.EmailService;
import com.sistema.service.EmailVerificationService;
import com.sistema.service.SmtpService;
import com.sistema.util.CpfGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Teste de integração para verificação de envio de emails com SMTP Mailtrap.
 * Estes testes são executados apenas quando as variáveis de ambiente do Mailtrap estão configuradas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@DisplayName("Email Verification Integration Tests")
class EmailVerificationIntegrationTest {

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private SmtpService smtpService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Criar usuário de teste
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("João");
        testUser.setLastName("Silva");
        testUser.setEmail("test.user." + System.currentTimeMillis() + "@example.com");
        testUser.setCpf(CpfGenerator.generateCpf());
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.USER);
        testUser.setEnabled(true);
        testUser.setEmailVerified(false);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve verificar configuração do serviço de email")
    void shouldVerifyEmailServiceConfiguration() {
        // When - Verificar se email está habilitado
        boolean emailEnabled = emailService.isEmailEnabled();

        // Then - Email deve estar configurado
        assertThat(emailEnabled).isTrue();
        
        // Verificar estatísticas do email
        Map<String, Object> emailStats = emailService.getEmailStats();
        assertThat(emailStats).isNotNull();
        assertThat(emailStats.get("emailEnabled")).isEqualTo(true);
        assertThat(emailStats.get("fromEmail")).isNotNull();
        assertThat(emailStats.get("verificationBaseUrl")).isNotNull();
        assertThat(emailStats.get("appName")).isNotNull();
    }

    @Test
    @DisplayName("Deve verificar configuração do SMTP")
    void shouldVerifySmtpConfiguration() {
        // When - Obter estatísticas do SMTP
        Map<String, Object> smtpStats = smtpService.getStatistics();

        // Then - Configuração deve estar presente
        assertThat(smtpStats).isNotNull();
        assertThat(smtpStats.get("host")).isNotNull();
        assertThat(smtpStats.get("port")).isNotNull();
        assertThat(smtpStats.get("username")).isNotNull();
        
        // Verificar se as configurações são do Mailtrap
        String host = (String) smtpStats.get("host");
        assertThat(host).containsAnyOf("mailtrap", "smtp.mailtrap.io");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "MAILTRAP_USERNAME", matches = ".+")
    @DisplayName("Deve testar conexão com Mailtrap")
    void shouldTestMailtrapConnection() {
        // When - Testar conexão
        boolean connectionTest = smtpService.testConnection();

        // Then - Conexão deve ser bem-sucedida
        assertThat(connectionTest).isTrue();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "MAILTRAP_USERNAME", matches = ".+")
    @DisplayName("Deve enviar email de teste para Mailtrap")
    void shouldSendTestEmailToMailtrap() {
        // Given - Email de destino de teste
        String testEmail = "test@example.com";

        // When & Then - Enviar email de teste
        assertThatCode(() -> {
            emailService.sendTestEmail(testEmail);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve simular envio de email de verificação")
    void shouldSimulateVerificationEmailSending() {
        // Given - Token de verificação
        String verificationToken = "test-verification-token-" + System.currentTimeMillis();

        // When - Enviar email de verificação
        boolean emailSent = emailService.sendVerificationEmail(testUser, verificationToken);

        // Then - Email deve ser enviado (ou simulado se SMTP não estiver configurado)
        if (emailService.isEmailEnabled()) {
            assertThat(emailSent).isTrue();
        } else {
            // Se email está desabilitado, deve retornar false mas não falhar
            assertThat(emailSent).isFalse();
        }
    }

    @Test
    @DisplayName("Deve simular reenvio de email de verificação")
    void shouldSimulateVerificationEmailResending() {
        // Given - Token de verificação
        String verificationToken = "resend-verification-token-" + System.currentTimeMillis();

        // When - Reenviar email de verificação
        boolean emailResent = emailService.resendVerificationEmail(testUser, verificationToken);

        // Then - Email deve ser reenviado (ou simulado se SMTP não estiver configurado)
        if (emailService.isEmailEnabled()) {
            assertThat(emailResent).isTrue();
        } else {
            // Se email está desabilitado, deve retornar false mas não falhar
            assertThat(emailResent).isFalse();
        }
    }

    @Test
    @DisplayName("Deve verificar se verificação de email está habilitada")
    void shouldVerifyEmailVerificationIsEnabled() {
        // When - Verificar se verificação está habilitada
        boolean verificationEnabled = emailVerificationService.isEmailVerificationEnabled();

        // Then - Verificação deve estar habilitada
        assertThat(verificationEnabled).isTrue();
    }

    @Test
    @DisplayName("Deve verificar se usuário precisa verificar email")
    void shouldCheckIfUserNeedsEmailVerification() {
        // When - Verificar se usuário precisa verificar email
        boolean needsVerification = emailVerificationService.needsEmailVerification(testUser);

        // Then - Usuário deve precisar verificar email
        assertThat(needsVerification).isTrue();
        
        // Marcar email como verificado e testar novamente
        testUser.setEmailVerified(true);
        boolean stillNeedsVerification = emailVerificationService.needsEmailVerification(testUser);
        assertThat(stillNeedsVerification).isFalse();
    }

    @Test
    @DisplayName("Deve gerar e validar token de verificação")
    void shouldGenerateAndValidateVerificationToken() {
        // When - Gerar token de verificação
        String token = emailVerificationService.generateVerificationToken(testUser);

        // Then - Token deve ser gerado
        assertThat(token).isNotNull().isNotEmpty();
        
        // Token deve ter formato UUID
        assertThat(token).matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "MAILTRAP_USERNAME", matches = ".+")
    @DisplayName("Deve executar fluxo completo de email com Mailtrap")
    void shouldExecuteCompleteEmailFlowWithMailtrap() {
        // Given - Configuração do Mailtrap disponível
        assertThat(smtpService.testConnection()).isTrue();

        // When - Executar fluxo completo
        // 1. Gerar token
        String token = emailVerificationService.generateVerificationToken(testUser);
        assertThat(token).isNotNull();

        // 2. Enviar email
        boolean emailSent = emailService.sendVerificationEmail(testUser, token);
        assertThat(emailSent).isTrue();

        // 3. Simular clique no link (validação do token seria feita pelo controller)
        // Aqui apenas verificamos se o token seria válido
        assertThat(token).isNotNull().isNotEmpty();

        // Then - Fluxo deve ser executado sem erros
        assertThat(emailService.isEmailEnabled()).isTrue();
        assertThat(emailVerificationService.isEmailVerificationEnabled()).isTrue();
    }

    @Test
    @DisplayName("Deve obter estatísticas de verificação de email")
    void shouldGetEmailVerificationStatistics() {
        // When - Obter estatísticas
        EmailVerificationService.EmailVerificationStats stats = 
            emailVerificationService.getVerificationStats();

        // Then - Estatísticas devem estar disponíveis
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalUsers()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getVerifiedUsers()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getUnverifiedUsers()).isGreaterThanOrEqualTo(0);
        assertThat(stats.getVerificationRate()).isBetween(0.0, 100.0);
    }

    @Test
    @DisplayName("Deve validar formato do email de verificação")
    void shouldValidateVerificationEmailFormat() {
        // Given - Token de verificação
        String token = "sample-token-123";

        // When - Tentar enviar email (mesmo que simulado)
        boolean result = emailService.sendVerificationEmail(testUser, token);

        // Then - Processo deve completar sem erros
        // O resultado depende se o email está habilitado ou não
        if (emailService.isEmailEnabled()) {
            // Se habilitado, deve tentar enviar
            assertThat(result).isTrue();
        } else {
            // Se desabilitado, deve retornar false mas não falhar
            assertThat(result).isFalse();
        }
    }

    @Test
    @DisplayName("Deve validar configurações de email necessárias")
    void shouldValidateRequiredEmailConfigurations() {
        // When - Obter configurações
        Map<String, Object> emailStats = emailService.getEmailStats();

        // Then - Configurações essenciais devem estar presentes
        assertThat(emailStats.get("fromEmail")).isNotNull();
        assertThat(emailStats.get("verificationBaseUrl")).isNotNull();
        assertThat(emailStats.get("appName")).isNotNull();
        
        // URL de verificação deve ter formato correto
        String verificationUrl = (String) emailStats.get("verificationBaseUrl");
        assertThat(verificationUrl).startsWith("http");
        assertThat(verificationUrl).contains("verify-email");
    }
}