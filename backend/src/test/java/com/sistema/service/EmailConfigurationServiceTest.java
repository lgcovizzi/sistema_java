package com.sistema.service;

import com.sistema.entity.EmailConfiguration;
import com.sistema.entity.EmailProvider;
import com.sistema.repository.EmailConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para EmailConfigurationService.
 * Verifica todas as operações CRUD e validações do serviço.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do EmailConfigurationService")
class EmailConfigurationServiceTest {

    @Mock
    private EmailConfigurationRepository emailConfigurationRepository;

    @InjectMocks
    private EmailConfigurationService emailConfigurationService;

    private EmailConfiguration mailtrapConfig;
    private EmailConfiguration gmailConfig;

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
    }

    @Test
    @DisplayName("Deve criar configuração de email com sucesso")
    void shouldCreateEmailConfiguration() {
        // Given
        EmailConfiguration newConfig = new EmailConfiguration();
        newConfig.setProvider(EmailProvider.GMAIL);
        newConfig.setHost("smtp.gmail.com");
        newConfig.setPort(587);
        newConfig.setUsername("new@gmail.com");
        newConfig.setPassword("new_password");
        newConfig.setEnabled(true);
        newConfig.setDescription("Nova configuração");

        when(emailConfigurationRepository.save(any(EmailConfiguration.class)))
                .thenReturn(gmailConfig);

        // When
        EmailConfiguration result = emailConfigurationService.createConfiguration(newConfig);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProvider()).isEqualTo(EmailProvider.GMAIL);
        verify(emailConfigurationRepository).save(any(EmailConfiguration.class));
    }

    @Test
    @DisplayName("Deve obter todas as configurações")
    void shouldGetAllConfigurations() {
        // Given
        List<EmailConfiguration> configurations = Arrays.asList(mailtrapConfig, gmailConfig);
        when(emailConfigurationRepository.findAll()).thenReturn(configurations);

        // When
        List<EmailConfiguration> result = emailConfigurationService.getAllConfigurations();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(mailtrapConfig, gmailConfig);
        verify(emailConfigurationRepository).findAll();
    }

    @Test
    @DisplayName("Deve obter configuração por ID")
    void shouldGetConfigurationById() {
        // Given
        when(emailConfigurationRepository.findById(1L)).thenReturn(Optional.of(mailtrapConfig));

        // When
        Optional<EmailConfiguration> result = emailConfigurationService.getConfigurationById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getProvider()).isEqualTo(EmailProvider.MAILTRAP);
        verify(emailConfigurationRepository).findById(1L);
    }

    @Test
    @DisplayName("Deve retornar empty quando configuração não encontrada")
    void shouldReturnEmptyWhenConfigurationNotFound() {
        // Given
        when(emailConfigurationRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<EmailConfiguration> result = emailConfigurationService.getConfigurationById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(emailConfigurationRepository).findById(999L);
    }

    @Test
    @DisplayName("Deve atualizar configuração existente")
    void shouldUpdateConfiguration() {
        // Given
        EmailConfiguration updatedConfig = new EmailConfiguration();
        updatedConfig.setProvider(EmailProvider.GMAIL);
        updatedConfig.setHost("smtp.gmail.com");
        updatedConfig.setPort(587);
        updatedConfig.setUsername("updated@gmail.com");
        updatedConfig.setPassword("updated_password");
        updatedConfig.setEnabled(true);
        updatedConfig.setDescription("Configuração atualizada");

        when(emailConfigurationRepository.findById(2L)).thenReturn(Optional.of(gmailConfig));
        when(emailConfigurationRepository.save(any(EmailConfiguration.class))).thenReturn(gmailConfig);

        // When
        EmailConfiguration result = emailConfigurationService.updateConfiguration(2L, updatedConfig);

        // Then
        assertThat(result).isNotNull();
        verify(emailConfigurationRepository).findById(2L);
        verify(emailConfigurationRepository).save(any(EmailConfiguration.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar configuração inexistente")
    void shouldThrowExceptionWhenUpdatingNonExistentConfiguration() {
        // Given
        EmailConfiguration updatedConfig = new EmailConfiguration();
        when(emailConfigurationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> emailConfigurationService.updateConfiguration(999L, updatedConfig))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Configuração não encontrada com ID: 999");
    }

    @Test
    @DisplayName("Deve definir configuração como padrão")
    void shouldSetAsDefault() {
        // Given
        when(emailConfigurationRepository.findById(2L)).thenReturn(Optional.of(gmailConfig));
        when(emailConfigurationRepository.save(any(EmailConfiguration.class))).thenReturn(gmailConfig);

        // When
        EmailConfiguration result = emailConfigurationService.setAsDefault(2L);

        // Then
        assertThat(result).isNotNull();
        verify(emailConfigurationRepository).clearAllDefaultFlags();
        verify(emailConfigurationRepository).findById(2L);
        verify(emailConfigurationRepository).save(any(EmailConfiguration.class));
    }

    @Test
    @DisplayName("Deve habilitar/desabilitar configuração")
    void shouldToggleEnabled() {
        // Given
        when(emailConfigurationRepository.findById(2L)).thenReturn(Optional.of(gmailConfig));
        when(emailConfigurationRepository.save(any(EmailConfiguration.class))).thenReturn(gmailConfig);

        // When
        EmailConfiguration result = emailConfigurationService.toggleEnabled(2L);

        // Then
        assertThat(result).isNotNull();
        verify(emailConfigurationRepository).findById(2L);
        verify(emailConfigurationRepository).save(any(EmailConfiguration.class));
    }

    @Test
    @DisplayName("Deve excluir configuração")
    void shouldDeleteConfiguration() {
        // Given
        when(emailConfigurationRepository.findById(2L)).thenReturn(Optional.of(gmailConfig));

        // When
        emailConfigurationService.deleteConfiguration(2L);

        // Then
        verify(emailConfigurationRepository).findById(2L);
        verify(emailConfigurationRepository).delete(gmailConfig);
    }

    @Test
    @DisplayName("Deve lançar exceção ao excluir configuração padrão")
    void shouldThrowExceptionWhenDeletingDefaultConfiguration() {
        // Given
        when(emailConfigurationRepository.findById(1L)).thenReturn(Optional.of(mailtrapConfig));

        // When & Then
        assertThatThrownBy(() -> emailConfigurationService.deleteConfiguration(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Não é possível excluir a configuração padrão");
    }

    @Test
    @DisplayName("Deve obter configuração padrão")
    void shouldGetDefaultConfiguration() {
        // Given
        when(emailConfigurationRepository.findDefaultConfiguration()).thenReturn(Optional.of(mailtrapConfig));

        // When
        Optional<EmailConfiguration> result = emailConfigurationService.getDefaultConfiguration();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().isDefault()).isTrue();
        verify(emailConfigurationRepository).findDefaultConfiguration();
    }

    @Test
    @DisplayName("Deve obter configurações ativas")
    void shouldGetEnabledConfigurations() {
        // Given
        List<EmailConfiguration> enabledConfigs = Arrays.asList(mailtrapConfig);
        when(emailConfigurationRepository.findByEnabledTrue()).thenReturn(enabledConfigs);

        // When
        List<EmailConfiguration> result = emailConfigurationService.getEnabledConfigurations();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isEnabled()).isTrue();
        verify(emailConfigurationRepository).findByEnabledTrue();
    }

    @Test
    @DisplayName("Deve validar configuração com dados válidos")
    void shouldValidateConfigurationWithValidData() {
        // Given
        EmailConfiguration validConfig = new EmailConfiguration();
        validConfig.setProvider(EmailProvider.GMAIL);
        validConfig.setHost("smtp.gmail.com");
        validConfig.setPort(587);
        validConfig.setUsername("test@gmail.com");
        validConfig.setPassword("password");

        // When & Then
        assertThatCode(() -> emailConfigurationService.validateConfiguration(validConfig))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve lançar exceção ao validar configuração com dados inválidos")
    void shouldThrowExceptionWhenValidatingInvalidConfiguration() {
        // Given
        EmailConfiguration invalidConfig = new EmailConfiguration();
        // Configuração sem dados obrigatórios

        // When & Then
        assertThatThrownBy(() -> emailConfigurationService.validateConfiguration(invalidConfig))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve obter configurações por provedor")
    void shouldGetConfigurationsByProvider() {
        // Given
        List<EmailConfiguration> gmailConfigs = Arrays.asList(gmailConfig);
        when(emailConfigurationRepository.findByProviderAndEnabledTrue(EmailProvider.GMAIL))
                .thenReturn(gmailConfigs);

        // When
        List<EmailConfiguration> result = emailConfigurationService.getConfigurationsByProvider(EmailProvider.GMAIL);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProvider()).isEqualTo(EmailProvider.GMAIL);
        verify(emailConfigurationRepository).findByProviderAndEnabledTrue(EmailProvider.GMAIL);
    }

    @Test
    @DisplayName("Deve obter configurações padrão do provedor")
    void shouldGetProviderDefaults() {
        // When
        EmailConfiguration result = emailConfigurationService.getProviderDefaults(EmailProvider.GMAIL);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getProvider()).isEqualTo(EmailProvider.GMAIL);
        assertThat(result.getHost()).isEqualTo("smtp.gmail.com");
        assertThat(result.getPort()).isEqualTo(587);
    }

    @Test
    @DisplayName("Deve lançar exceção com parâmetros nulos")
    void shouldThrowExceptionWithNullParameters() {
        // When & Then
        assertThatThrownBy(() -> emailConfigurationService.createConfiguration(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Configuração não pode ser nula");

        assertThatThrownBy(() -> emailConfigurationService.getConfigurationById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID não pode ser nulo");

        assertThatThrownBy(() -> emailConfigurationService.getConfigurationsByProvider(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Provider não pode ser nulo");
    }
}