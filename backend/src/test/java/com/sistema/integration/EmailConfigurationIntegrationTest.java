package com.sistema.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.entity.EmailConfiguration;
import com.sistema.enums.EmailProvider;
import com.sistema.repository.EmailConfigurationRepository;
import com.sistema.service.EmailConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para o sistema de configuração de email.
 * Testa o fluxo completo desde o controller até o banco de dados.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Testes de Integração - Sistema de Configuração de Email")
class EmailConfigurationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private EmailConfigurationRepository emailConfigurationRepository;

    @Autowired
    private EmailConfigurationService emailConfigurationService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Limpar dados de teste
        emailConfigurationRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve criar configuração Mailtrap via API")
    @Transactional
    void shouldCreateMailtrapConfigurationViaApi() throws Exception {
        // Given
        EmailConfiguration config = new EmailConfiguration();
        config.setProvider(EmailProvider.MAILTRAP);
        config.setHost("sandbox.smtp.mailtrap.io");
        config.setPort(2525);
        config.setUsername("test_user");
        config.setPassword("test_password");
        config.setIsActive(true);
        config.setDescription("Configuração de teste");

        // When & Then
        mockMvc.perform(post("/api/admin/email-config")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.configuration.provider").value("MAILTRAP"))
                .andExpect(jsonPath("$.configuration.host").value("sandbox.smtp.mailtrap.io"))
                .andExpect(jsonPath("$.configuration.port").value(2525))
                .andExpect(jsonPath("$.configuration.enabled").value(true));

        // Verificar no banco de dados
        List<EmailConfiguration> configurations = emailConfigurationRepository.findAll();
        assertEquals(1, configurations.size(), "Deve ter uma configuração no banco");
        
        EmailConfiguration saved = configurations.get(0);
        assertEquals(EmailProvider.MAILTRAP, saved.getProvider());
        assertEquals("sandbox.smtp.mailtrap.io", saved.getHost());
        assertEquals(2525, saved.getPort());
        assertTrue(saved.getIsActive());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve criar configuração Gmail via API")
    @Transactional
    void shouldCreateGmailConfigurationViaApi() throws Exception {
        // Given
        EmailConfiguration config = new EmailConfiguration();
        config.setProvider(EmailProvider.GMAIL);
        config.setHost("smtp.gmail.com");
        config.setPort(587);
        config.setUsername("test@gmail.com");
        config.setPassword("app_password");
        config.setIsActive(true);
        config.setDescription("Configuração Gmail");

        // When & Then
        mockMvc.perform(post("/api/admin/email-config")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.configuration.provider").value("GMAIL"))
                .andExpect(jsonPath("$.configuration.host").value("smtp.gmail.com"))
                .andExpect(jsonPath("$.configuration.port").value(587));

        // Verificar no banco de dados
        List<EmailConfiguration> savedList = emailConfigurationRepository.findByProvider(EmailProvider.GMAIL);
        assertFalse(savedList.isEmpty(), "Configuração Gmail deve estar salva");
        assertEquals("test@gmail.com", savedList.get(0).getUsername());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve definir configuração como padrão via API")
    @Transactional
    void shouldSetConfigurationAsDefaultViaApi() throws Exception {
        // Given - Criar duas configurações
        EmailConfiguration mailtrap = createMailtrapConfiguration();
        EmailConfiguration gmail = createGmailConfiguration();
        
        EmailConfiguration savedMailtrap = emailConfigurationRepository.save(mailtrap);
        EmailConfiguration savedGmail = emailConfigurationRepository.save(gmail);

        // Definir Mailtrap como padrão inicialmente
        savedMailtrap.setDefault(true);
        emailConfigurationRepository.save(savedMailtrap);

        // When - Definir Gmail como padrão via API
        mockMvc.perform(put("/api/admin/email-config/" + savedGmail.getId() + "/default")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.configuration.default").value(true));

        // Then - Verificar no banco de dados
        Optional<EmailConfiguration> defaultConfig = emailConfigurationRepository.findDefaultConfiguration();
        assertTrue(defaultConfig.isPresent(), "Deve ter uma configuração padrão");
        assertEquals(EmailProvider.GMAIL, defaultConfig.get().getProvider(), "Gmail deve ser o padrão");

        // Verificar que Mailtrap não é mais padrão
        Optional<EmailConfiguration> mailtrapConfig = emailConfigurationRepository.findById(savedMailtrap.getId());
        assertTrue(mailtrapConfig.isPresent());
        assertFalse(mailtrapConfig.get().isDefault(), "Mailtrap não deve mais ser padrão");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve alternar status enabled via API")
    @Transactional
    void shouldToggleEnabledStatusViaApi() throws Exception {
        // Given
        EmailConfiguration config = createMailtrapConfiguration();
        config.setIsActive(false);
        EmailConfiguration saved = emailConfigurationRepository.save(config);

        // When - Alternar para enabled
        mockMvc.perform(put("/api/admin/email-config/" + saved.getId() + "/toggle")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.configuration.enabled").value(true));

        // Then - Verificar no banco
        Optional<EmailConfiguration> updated = emailConfigurationRepository.findById(saved.getId());
        assertTrue(updated.isPresent());
        assertTrue(updated.get().getIsActive(), "Configuração deve estar habilitada");

        // When - Alternar para disabled
        mockMvc.perform(put("/api/admin/email-config/" + saved.getId() + "/toggle")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuration.enabled").value(false));

        // Then - Verificar no banco novamente
        Optional<EmailConfiguration> toggledAgain = emailConfigurationRepository.findById(saved.getId());
        assertTrue(toggledAgain.isPresent());
        assertFalse(toggledAgain.get().getIsActive(), "Configuração deve estar desabilitada");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve atualizar configuração via API")
    @Transactional
    void shouldUpdateConfigurationViaApi() throws Exception {
        // Given
        EmailConfiguration original = createMailtrapConfiguration();
        EmailConfiguration saved = emailConfigurationRepository.save(original);

        // Dados para atualização
        EmailConfiguration updated = new EmailConfiguration();
        updated.setProvider(EmailProvider.MAILTRAP);
        updated.setHost("new.smtp.mailtrap.io");
        updated.setPort(2526);
        updated.setUsername("new_user");
        updated.setPassword("new_password");
        updated.setIsActive(false);
        updated.setDescription("Configuração atualizada");

        // When
        mockMvc.perform(put("/api/admin/email-config/" + saved.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.configuration.host").value("new.smtp.mailtrap.io"))
                .andExpect(jsonPath("$.configuration.port").value(2526))
                .andExpect(jsonPath("$.configuration.username").value("new_user"))
                .andExpect(jsonPath("$.configuration.enabled").value(false));

        // Then - Verificar no banco
        Optional<EmailConfiguration> updatedConfig = emailConfigurationRepository.findById(saved.getId());
        assertTrue(updatedConfig.isPresent());
        assertEquals("new.smtp.mailtrap.io", updatedConfig.get().getHost());
        assertEquals(2526, updatedConfig.get().getPort());
        assertEquals("new_user", updatedConfig.get().getUsername());
        assertFalse(updatedConfig.get().getIsActive());
        assertNotNull(updatedConfig.get().getUpdatedAt());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve excluir configuração via API")
    @Transactional
    void shouldDeleteConfigurationViaApi() throws Exception {
        // Given
        EmailConfiguration config = createMailtrapConfiguration();
        EmailConfiguration saved = emailConfigurationRepository.save(config);
        Long configId = saved.getId();

        // Verificar que existe
        assertTrue(emailConfigurationRepository.existsById(configId), "Configuração deve existir");

        // When
        mockMvc.perform(delete("/api/admin/email-config/" + configId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Configuração excluída com sucesso"));

        // Then - Verificar que foi excluída
        assertFalse(emailConfigurationRepository.existsById(configId), "Configuração deve ter sido excluída");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve listar todas as configurações via API")
    @Transactional
    void shouldListAllConfigurationsViaApi() throws Exception {
        // Given
        EmailConfiguration mailtrap = createMailtrapConfiguration();
        EmailConfiguration gmail = createGmailConfiguration();
        mailtrap.setDefault(true);
        
        emailConfigurationRepository.save(mailtrap);
        emailConfigurationRepository.save(gmail);

        // When & Then
        mockMvc.perform(get("/api/admin/email-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.configurations").isArray())
                .andExpect(jsonPath("$.configurations.length()").value(2))
                .andExpect(jsonPath("$.totalConfigurations").value(2))
                .andExpect(jsonPath("$.defaultConfiguration.provider").value("MAILTRAP"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve obter configuração por ID via API")
    @Transactional
    void shouldGetConfigurationByIdViaApi() throws Exception {
        // Given
        EmailConfiguration config = createGmailConfiguration();
        EmailConfiguration saved = emailConfigurationRepository.save(config);

        // When & Then
        mockMvc.perform(get("/api/admin/email-config/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.configuration.id").value(saved.getId()))
                .andExpect(jsonPath("$.configuration.provider").value("GMAIL"))
                .andExpect(jsonPath("$.configuration.host").value("smtp.gmail.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve retornar 404 para configuração inexistente")
    void shouldReturn404ForNonExistentConfiguration() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/email-config/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Configuração não encontrada"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve obter provedores disponíveis via API")
    void shouldGetAvailableProvidersViaApi() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/email-config/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.providers").isArray())
                .andExpect(jsonPath("$.providers.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve obter configurações padrão do provedor via API")
    void shouldGetProviderDefaultsViaApi() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/email-config/providers/GMAIL/defaults"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.defaults.provider").value("GMAIL"))
                .andExpect(jsonPath("$.defaults.host").value("smtp.gmail.com"))
                .andExpect(jsonPath("$.defaults.port").value(587));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Deve negar acesso para usuários não admin")
    void shouldDenyAccessForNonAdminUsers() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/admin/email-config"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/email-config")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
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
    @DisplayName("Deve validar dados obrigatórios na criação")
    @Transactional
    void shouldValidateRequiredFieldsOnCreation() throws Exception {
        // Given - Configuração inválida (sem provider)
        EmailConfiguration invalidConfig = new EmailConfiguration();
        invalidConfig.setHost("smtp.example.com");
        invalidConfig.setPort(587);

        // When & Then
        mockMvc.perform(post("/api/admin/email-config")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidConfig)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve funcionar fluxo completo de configuração")
    @WithMockUser(roles = "ADMIN")
    @Transactional
    void shouldWorkCompleteConfigurationFlow() throws Exception {
        // 1. Criar configuração Mailtrap
        EmailConfiguration mailtrap = createMailtrapConfiguration();
        String mailtrapJson = objectMapper.writeValueAsString(mailtrap);
        
        String response1 = mockMvc.perform(post("/api/admin/email-config")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mailtrapJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // 2. Criar configuração Gmail
        EmailConfiguration gmail = createGmailConfiguration();
        String gmailJson = objectMapper.writeValueAsString(gmail);
        
        mockMvc.perform(post("/api/admin/email-config")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gmailJson))
                .andExpect(status().isCreated());

        // 3. Listar configurações
        mockMvc.perform(get("/api/admin/email-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configurations.length()").value(2));

        // 4. Definir Gmail como padrão
        List<EmailConfiguration> configs = emailConfigurationRepository.findAll();
        EmailConfiguration gmailConfig = configs.stream()
                .filter(c -> c.getProvider() == EmailProvider.GMAIL)
                .findFirst()
                .orElseThrow();

        mockMvc.perform(put("/api/admin/email-config/" + gmailConfig.getId() + "/default")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuration.default").value(true));

        // 5. Verificar que Gmail é o padrão
        Optional<EmailConfiguration> defaultConfig = emailConfigurationRepository.findDefaultConfiguration();
        assertTrue(defaultConfig.isPresent());
        assertEquals(EmailProvider.GMAIL, defaultConfig.get().getProvider());

        // 6. Desabilitar configuração
        mockMvc.perform(put("/api/admin/email-config/" + gmailConfig.getId() + "/toggle")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configuration.enabled").value(false));

        // 7. Verificar estado final
        Optional<EmailConfiguration> finalConfig = emailConfigurationRepository.findById(gmailConfig.getId());
        assertTrue(finalConfig.isPresent());
        assertTrue(finalConfig.get().isDefault());
        assertFalse(finalConfig.get().getIsActive());
    }

    /**
     * Métodos auxiliares para criar configurações de teste
     */
    private EmailConfiguration createMailtrapConfiguration() {
        EmailConfiguration config = new EmailConfiguration();
        config.setProvider(EmailProvider.MAILTRAP);
        config.setHost("sandbox.smtp.mailtrap.io");
        config.setPort(2525);
        config.setUsername("test_user");
        config.setPassword("test_password");
        config.setIsActive(true);
        config.setDefault(false);
        config.setDescription("Configuração Mailtrap de teste");
        return config;
    }

    private EmailConfiguration createGmailConfiguration() {
        EmailConfiguration config = new EmailConfiguration();
        config.setProvider(EmailProvider.GMAIL);
        config.setHost("smtp.gmail.com");
        config.setPort(587);
        config.setUsername("test@gmail.com");
        config.setPassword("app_password");
        config.setIsActive(true);
        config.setDefault(false);
        config.setDescription("Configuração Gmail de teste");
        return config;
    }
}