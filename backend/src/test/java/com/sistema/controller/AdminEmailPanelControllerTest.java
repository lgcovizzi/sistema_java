package com.sistema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.entity.EmailConfiguration;
import com.sistema.entity.EmailProvider;
import com.sistema.service.EmailConfigurationService;
import com.sistema.service.SmtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes unitários para o painel administrativo de configuração de email.
 * Testa os endpoints específicos do painel de administração.
 */
@WebMvcTest(AdminEmailPanelController.class)
@DisplayName("Testes do Painel Administrativo de Email")
class AdminEmailPanelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmailConfigurationService emailConfigurationService;

    @MockBean
    private SmtpService smtpService;

    @Autowired
    private ObjectMapper objectMapper;

    private EmailConfiguration mailtrapConfig;
    private EmailConfiguration gmailConfig;

    @BeforeEach
    void setUp() {
        mailtrapConfig = createMailtrapConfiguration();
        gmailConfig = createGmailConfiguration();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve renderizar página do painel de email")
    void shouldRenderEmailPanelPage() throws Exception {
        // Given
        when(emailConfigurationService.findAll()).thenReturn(Arrays.asList(mailtrapConfig, gmailConfig));
        when(emailConfigurationService.findDefaultConfiguration()).thenReturn(Optional.of(mailtrapConfig));

        // When & Then
        mockMvc.perform(get("/admin/email-panel"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/email-panel"))
                .andExpect(model().attributeExists("configurations"))
                .andExpect(model().attributeExists("defaultConfiguration"))
                .andExpect(model().attributeExists("providers"));

        verify(emailConfigurationService).findAll();
        verify(emailConfigurationService).findDefaultConfiguration();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve obter status atual do painel")
    void shouldGetCurrentPanelStatus() throws Exception {
        // Given
        when(emailConfigurationService.findDefaultConfiguration()).thenReturn(Optional.of(mailtrapConfig));
        when(emailConfigurationService.countActiveConfigurations()).thenReturn(2L);
        when(smtpService.isConfigured()).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/admin/email-panel/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status.currentProvider").value("MAILTRAP"))
                .andExpect(jsonPath("$.status.isConfigured").value(true))
                .andExpect(jsonPath("$.status.activeConfigurations").value(2))
                .andExpect(jsonPath("$.status.defaultConfiguration").exists());

        verify(emailConfigurationService).findDefaultConfiguration();
        verify(emailConfigurationService).countActiveConfigurations();
        verify(smtpService).isConfigured();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve alternar provedor de email")
    void shouldSwitchEmailProvider() throws Exception {
        // Given
        when(emailConfigurationService.findByProvider(EmailProvider.GMAIL)).thenReturn(Optional.of(gmailConfig));
        when(emailConfigurationService.setAsDefault(anyLong())).thenReturn(gmailConfig);

        // When & Then
        mockMvc.perform(post("/api/admin/email-panel/switch-provider")
                        .with(csrf())
                        .param("provider", "GMAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Provedor alterado para GMAIL com sucesso"))
                .andExpect(jsonPath("$.configuration.provider").value("GMAIL"));

        verify(emailConfigurationService).findByProvider(EmailProvider.GMAIL);
        verify(emailConfigurationService).setAsDefault(gmailConfig.getId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve falhar ao alternar para provedor inexistente")
    void shouldFailToSwitchToNonExistentProvider() throws Exception {
        // Given
        when(emailConfigurationService.findByProvider(EmailProvider.GMAIL)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/admin/email-panel/switch-provider")
                        .with(csrf())
                        .param("provider", "GMAIL"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Configuração para provedor GMAIL não encontrada"));

        verify(emailConfigurationService).findByProvider(EmailProvider.GMAIL);
        verify(emailConfigurationService, never()).setAsDefault(anyLong());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve testar configuração atual")
    void shouldTestCurrentConfiguration() throws Exception {
        // Given
        when(emailConfigurationService.findDefaultConfiguration()).thenReturn(Optional.of(mailtrapConfig));
        when(smtpService.testConnection(any(EmailConfiguration.class))).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/admin/email-panel/test-configuration")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Teste de conexão realizado com sucesso"))
                .andExpect(jsonPath("$.testResult.connectionSuccessful").value(true))
                .andExpect(jsonPath("$.testResult.provider").value("MAILTRAP"));

        verify(emailConfigurationService).findDefaultConfiguration();
        verify(smtpService).testConnection(mailtrapConfig);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve falhar teste quando não há configuração padrão")
    void shouldFailTestWhenNoDefaultConfiguration() throws Exception {
        // Given
        when(emailConfigurationService.findDefaultConfiguration()).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/admin/email-panel/test-configuration")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Nenhuma configuração padrão encontrada"));

        verify(emailConfigurationService).findDefaultConfiguration();
        verify(smtpService, never()).testConnection(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve enviar email de teste")
    void shouldSendTestEmail() throws Exception {
        // Given
        String testEmail = "test@example.com";
        when(emailConfigurationService.findDefaultConfiguration()).thenReturn(Optional.of(mailtrapConfig));
        when(smtpService.sendTestEmail(eq(testEmail), any(EmailConfiguration.class))).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/admin/email-panel/send-test-email")
                        .with(csrf())
                        .param("email", testEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email de teste enviado com sucesso"))
                .andExpect(jsonPath("$.testResult.emailSent").value(true))
                .andExpect(jsonPath("$.testResult.recipientEmail").value(testEmail));

        verify(emailConfigurationService).findDefaultConfiguration();
        verify(smtpService).sendTestEmail(testEmail, mailtrapConfig);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve validar email inválido no teste")
    void shouldValidateInvalidEmailInTest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/admin/email-panel/send-test-email")
                        .with(csrf())
                        .param("email", "invalid-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email inválido"));

        verify(smtpService, never()).sendTestEmail(anyString(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve obter configurações rápidas")
    void shouldGetQuickConfigurations() throws Exception {
        // Given
        List<EmailConfiguration> configs = Arrays.asList(mailtrapConfig, gmailConfig);
        when(emailConfigurationService.findAll()).thenReturn(configs);

        // When & Then
        mockMvc.perform(get("/api/admin/email-panel/quick-configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.configurations").isArray())
                .andExpect(jsonPath("$.configurations.length()").value(2))
                .andExpect(jsonPath("$.configurations[0].provider").exists())
                .andExpect(jsonPath("$.configurations[0].enabled").exists())
                .andExpect(jsonPath("$.configurations[0].default").exists());

        verify(emailConfigurationService).findAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve alternar status enabled de configuração")
    void shouldToggleConfigurationEnabled() throws Exception {
        // Given
        Long configId = 1L;
        EmailConfiguration updatedConfig = createMailtrapConfiguration();
        updatedConfig.setEnabled(false);
        
        when(emailConfigurationService.toggleEnabled(configId)).thenReturn(updatedConfig);

        // When & Then
        mockMvc.perform(post("/api/admin/email-panel/toggle-enabled")
                        .with(csrf())
                        .param("configId", configId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Status da configuração alterado com sucesso"))
                .andExpect(jsonPath("$.configuration.enabled").value(false));

        verify(emailConfigurationService).toggleEnabled(configId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve obter histórico de configurações")
    void shouldGetConfigurationHistory() throws Exception {
        // Given
        when(emailConfigurationService.getConfigurationHistory()).thenReturn(Arrays.asList(
                "2024-01-01 10:00 - Configuração MAILTRAP criada",
                "2024-01-01 11:00 - Configuração GMAIL criada",
                "2024-01-01 12:00 - GMAIL definido como padrão"
        ));

        // When & Then
        mockMvc.perform(get("/api/admin/email-panel/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.history").isArray())
                .andExpect(jsonPath("$.history.length()").value(3));

        verify(emailConfigurationService).getConfigurationHistory();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve obter estatísticas do painel")
    void shouldGetPanelStatistics() throws Exception {
        // Given
        when(emailConfigurationService.countTotalConfigurations()).thenReturn(2L);
        when(emailConfigurationService.countActiveConfigurations()).thenReturn(1L);
        when(emailConfigurationService.countByProvider(EmailProvider.MAILTRAP)).thenReturn(1L);
        when(emailConfigurationService.countByProvider(EmailProvider.GMAIL)).thenReturn(1L);

        // When & Then
        mockMvc.perform(get("/api/admin/email-panel/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statistics.totalConfigurations").value(2))
                .andExpect(jsonPath("$.statistics.activeConfigurations").value(1))
                .andExpect(jsonPath("$.statistics.providerCounts.MAILTRAP").value(1))
                .andExpect(jsonPath("$.statistics.providerCounts.GMAIL").value(1));

        verify(emailConfigurationService).countTotalConfigurations();
        verify(emailConfigurationService).countActiveConfigurations();
        verify(emailConfigurationService).countByProvider(EmailProvider.MAILTRAP);
        verify(emailConfigurationService).countByProvider(EmailProvider.GMAIL);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve criar configuração rápida")
    void shouldCreateQuickConfiguration() throws Exception {
        // Given
        EmailConfiguration newConfig = createGmailConfiguration();
        when(emailConfigurationService.createQuickConfiguration(eq(EmailProvider.GMAIL), anyString()))
                .thenReturn(newConfig);

        // When & Then
        mockMvc.perform(post("/api/admin/email-panel/quick-create")
                        .with(csrf())
                        .param("provider", "GMAIL")
                        .param("description", "Configuração rápida Gmail"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Configuração criada com sucesso"))
                .andExpect(jsonPath("$.configuration.provider").value("GMAIL"));

        verify(emailConfigurationService).createQuickConfiguration(EmailProvider.GMAIL, "Configuração rápida Gmail");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve exportar configurações")
    void shouldExportConfigurations() throws Exception {
        // Given
        List<EmailConfiguration> configs = Arrays.asList(mailtrapConfig, gmailConfig);
        when(emailConfigurationService.findAll()).thenReturn(configs);

        // When & Then
        mockMvc.perform(get("/api/admin/email-panel/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=email-configurations.json"))
                .andExpect(jsonPath("$.configurations").isArray())
                .andExpect(jsonPath("$.configurations.length()").value(2))
                .andExpect(jsonPath("$.exportDate").exists());

        verify(emailConfigurationService).findAll();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Deve negar acesso para usuários não admin")
    void shouldDenyAccessForNonAdminUsers() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/email-panel"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/email-panel/status"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/email-panel/switch-provider")
                        .with(csrf())
                        .param("provider", "GMAIL"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve negar acesso para usuários não autenticados")
    void shouldDenyAccessForUnauthenticatedUsers() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/email-panel"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/email-panel/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve validar parâmetros obrigatórios")
    void shouldValidateRequiredParameters() throws Exception {
        // Teste sem provider
        mockMvc.perform(post("/api/admin/email-panel/switch-provider")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        // Teste sem configId
        mockMvc.perform(post("/api/admin/email-panel/toggle-enabled")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        // Teste sem email
        mockMvc.perform(post("/api/admin/email-panel/send-test-email")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve tratar erros de serviço adequadamente")
    void shouldHandleServiceErrorsAppropriately() throws Exception {
        // Given
        when(emailConfigurationService.findDefaultConfiguration())
                .thenThrow(new RuntimeException("Erro interno do serviço"));

        // When & Then
        mockMvc.perform(post("/api/admin/email-panel/test-configuration")
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Erro interno do servidor"));
    }

    /**
     * Métodos auxiliares para criar configurações de teste
     */
    private EmailConfiguration createMailtrapConfiguration() {
        EmailConfiguration config = new EmailConfiguration();
        config.setId(1L);
        config.setProvider(EmailProvider.MAILTRAP);
        config.setHost("sandbox.smtp.mailtrap.io");
        config.setPort(2525);
        config.setUsername("test_user");
        config.setPassword("test_password");
        config.setEnabled(true);
        config.setDefault(true);
        config.setDescription("Configuração Mailtrap de teste");
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        return config;
    }

    private EmailConfiguration createGmailConfiguration() {
        EmailConfiguration config = new EmailConfiguration();
        config.setId(2L);
        config.setProvider(EmailProvider.GMAIL);
        config.setHost("smtp.gmail.com");
        config.setPort(587);
        config.setUsername("test@gmail.com");
        config.setPassword("app_password");
        config.setEnabled(true);
        config.setDefault(false);
        config.setDescription("Configuração Gmail de teste");
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        return config;
    }
}