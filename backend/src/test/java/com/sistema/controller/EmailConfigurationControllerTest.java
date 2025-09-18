package com.sistema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.entity.EmailConfiguration;
import com.sistema.enums.EmailProvider;
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
 * Testes unitários para EmailConfigurationController.
 * Verifica todos os endpoints REST e suas respostas.
 */
@WebMvcTest(EmailConfigurationController.class)
@DisplayName("Testes do EmailConfigurationController")
class EmailConfigurationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailConfigurationService emailConfigurationService;

    @MockBean
    private SmtpService smtpService;

    private EmailConfiguration mailtrapConfig;
    private EmailConfiguration gmailConfig;
    private List<EmailConfiguration> allConfigurations;

    @BeforeEach
    void setUp() {
        // Configuração Mailtrap
        mailtrapConfig = new EmailConfiguration();
        mailtrapConfig.setId(1L);
        mailtrapConfig.setProvider(EmailProvider.MAILTRAP);
        mailtrapConfig.setHost("sandbox.smtp.mailtrap.io");
        mailtrapConfig.setPort(2525);
        mailtrapConfig.setUsername("test_user");
        mailtrapConfig.setPassword("test_password");
        mailtrapConfig.setEnabled(true);
        mailtrapConfig.setDefault(true);
        mailtrapConfig.setDescription("Configuração de desenvolvimento");
        mailtrapConfig.setCreatedAt(LocalDateTime.now());
        mailtrapConfig.setUpdatedAt(LocalDateTime.now());

        // Configuração Gmail
        gmailConfig = new EmailConfiguration();
        gmailConfig.setId(2L);
        gmailConfig.setProvider(EmailProvider.GMAIL);
        gmailConfig.setHost("smtp.gmail.com");
        gmailConfig.setPort(587);
        gmailConfig.setUsername("test@gmail.com");
        gmailConfig.setPassword("app_password");
        gmailConfig.setEnabled(false);
        gmailConfig.setDefault(false);
        gmailConfig.setDescription("Configuração de produção");
        gmailConfig.setCreatedAt(LocalDateTime.now());
        gmailConfig.setUpdatedAt(LocalDateTime.now());

        allConfigurations = Arrays.asList(mailtrapConfig, gmailConfig);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve listar todas as configurações")
    void shouldGetAllConfigurations() throws Exception {
        // Given
        when(emailConfigurationService.getAllConfigurations()).thenReturn(allConfigurations);
        when(emailConfigurationService.getDefaultConfiguration()).thenReturn(Optional.of(mailtrapConfig));

        // When & Then
        mockMvc.perform(get("/api/admin/email-config"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.configurations").isArray())
                .andExpect(jsonPath("$.configurations.length()").value(2))
                .andExpect(jsonPath("$.defaultConfiguration.id").value(1))
                .andExpect(jsonPath("$.totalConfigurations").value(2));

        verify(emailConfigurationService).getAllConfigurations();
        verify(emailConfigurationService).getDefaultConfiguration();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve obter configuração por ID")
    void shouldGetConfigurationById() throws Exception {
        // Given
        when(emailConfigurationService.getConfigurationById(1L)).thenReturn(Optional.of(mailtrapConfig));

        // When & Then
        mockMvc.perform(get("/api/admin/email-config/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.configuration.id").value(1))
                .andExpect(jsonPath("$.configuration.provider").value("MAILTRAP"));

        verify(emailConfigurationService).getConfigurationById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve retornar 404 quando configuração não encontrada")
    void shouldReturn404WhenConfigurationNotFound() throws Exception {
        // Given
        when(emailConfigurationService.getConfigurationById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/admin/email-config/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Configuração não encontrada"));

        verify(emailConfigurationService).getConfigurationById(999L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve criar nova configuração")
    void shouldCreateConfiguration() throws Exception {
        // Given
        EmailConfiguration newConfig = new EmailConfiguration();
        newConfig.setProvider(EmailProvider.GMAIL);
        newConfig.setHost("smtp.gmail.com");
        newConfig.setPort(587);
        newConfig.setUsername("new@gmail.com");
        newConfig.setPassword("new_password");
        newConfig.setEnabled(true);
        newConfig.setDescription("Nova configuração");

        when(emailConfigurationService.createConfiguration(any(EmailConfiguration.class)))
                .thenReturn(gmailConfig);

        // When & Then
        mockMvc.perform(post("/api/admin/email-config")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newConfig)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Configuração criada com sucesso"))
                .andExpect(jsonPath("$.configuration.id").value(2));

        verify(emailConfigurationService).createConfiguration(any(EmailConfiguration.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve atualizar configuração existente")
    void shouldUpdateConfiguration() throws Exception {
        // Given
        EmailConfiguration updatedConfig = new EmailConfiguration();
        updatedConfig.setProvider(EmailProvider.GMAIL);
        updatedConfig.setHost("smtp.gmail.com");
        updatedConfig.setPort(587);
        updatedConfig.setUsername("updated@gmail.com");
        updatedConfig.setPassword("updated_password");
        updatedConfig.setEnabled(true);
        updatedConfig.setDescription("Configuração atualizada");

        when(emailConfigurationService.updateConfiguration(eq(2L), any(EmailConfiguration.class)))
                .thenReturn(gmailConfig);

        // When & Then
        mockMvc.perform(put("/api/admin/email-config/2")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedConfig)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Configuração atualizada com sucesso"))
                .andExpect(jsonPath("$.configuration.id").value(2));

        verify(emailConfigurationService).updateConfiguration(eq(2L), any(EmailConfiguration.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve definir configuração como padrão")
    void shouldSetAsDefault() throws Exception {
        // Given
        when(emailConfigurationService.setAsDefault(2L)).thenReturn(gmailConfig);

        // When & Then
        mockMvc.perform(put("/api/admin/email-config/2/default")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Configuração definida como padrão"))
                .andExpect(jsonPath("$.configuration.id").value(2));

        verify(emailConfigurationService).setAsDefault(2L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve habilitar/desabilitar configuração")
    void shouldToggleEnabled() throws Exception {
        // Given
        when(emailConfigurationService.toggleConfiguration(2L, true)).thenReturn(gmailConfig);

        // When & Then
        mockMvc.perform(put("/api/admin/email-config/2/toggle")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Status da configuração alterado"))
                .andExpect(jsonPath("$.configuration.id").value(2));

        verify(emailConfigurationService).toggleConfiguration(2L, true);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve excluir configuração")
    void shouldDeleteConfiguration() throws Exception {
        // Given
        doNothing().when(emailConfigurationService).deleteConfiguration(2L);

        // When & Then
        mockMvc.perform(delete("/api/admin/email-config/2")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Configuração excluída com sucesso"));

        verify(emailConfigurationService).deleteConfiguration(2L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve testar configuração")
    void shouldTestConfiguration() throws Exception {
        // Given
        when(smtpService.testConnection()).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/admin/email-config/1/test")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Teste de conexão realizado com sucesso"));

        verify(smtpService).testConnection();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve listar provedores disponíveis")
    void shouldGetProviders() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/email-config/providers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.providers").isArray())
                .andExpect(jsonPath("$.providers.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve obter configurações padrão do provedor")
    void shouldGetProviderDefaults() throws Exception {
        // Given
        EmailConfiguration defaultConfig = new EmailConfiguration();
        defaultConfig.setProvider(EmailProvider.GMAIL);
        defaultConfig.setHost("smtp.gmail.com");
        defaultConfig.setPort(587);

        when(emailConfigurationService.getProviderDefaults(EmailProvider.GMAIL))
                .thenReturn(defaultConfig);

        // When & Then
        mockMvc.perform(get("/api/admin/email-config/providers/GMAIL/defaults"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.defaults.provider").value("GMAIL"))
                .andExpect(jsonPath("$.defaults.host").value("smtp.gmail.com"))
                .andExpect(jsonPath("$.defaults.port").value(587));

        verify(emailConfigurationService).getProviderDefaults(EmailProvider.GMAIL);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Deve negar acesso para usuários não admin")
    void shouldDenyAccessForNonAdminUsers() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/email-config"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve negar acesso para usuários não autenticados")
    void shouldDenyAccessForUnauthenticatedUsers() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/email-config"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve retornar erro 400 para dados inválidos")
    void shouldReturn400ForInvalidData() throws Exception {
        // Given
        EmailConfiguration invalidConfig = new EmailConfiguration();
        // Configuração sem dados obrigatórios

        when(emailConfigurationService.createConfiguration(any(EmailConfiguration.class)))
                .thenThrow(new IllegalArgumentException("Dados inválidos"));

        // When & Then
        mockMvc.perform(post("/api/admin/email-config")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidConfig)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dados inválidos"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve retornar erro 500 para erros internos")
    void shouldReturn500ForInternalErrors() throws Exception {
        // Given
        when(emailConfigurationService.getAllConfigurations())
                .thenThrow(new RuntimeException("Erro interno"));

        // When & Then
        mockMvc.perform(get("/api/admin/email-config"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Erro ao listar configurações de email"));
    }
}