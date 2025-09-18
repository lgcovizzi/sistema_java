package com.sistema.service;

import com.sistema.entity.EmailConfiguration;
import com.sistema.enums.EmailProvider;
import com.sistema.repository.EmailConfigurationRepository;
import com.sistema.service.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Serviço para gerenciar configurações de email do sistema.
 * Permite alternar entre diferentes provedores (Mailtrap, Gmail).
 */
@Service
@Transactional
public class EmailConfigurationService extends BaseService {

    private final EmailConfigurationRepository emailConfigurationRepository;

    @Autowired
    public EmailConfigurationService(EmailConfigurationRepository emailConfigurationRepository) {
        this.emailConfigurationRepository = emailConfigurationRepository;
    }

    /**
     * Obtém a configuração de email padrão ativa.
     * 
     * @return Optional com a configuração padrão
     */
    @Transactional(readOnly = true)
    public Optional<EmailConfiguration> getDefaultConfiguration() {
        logDebug("Buscando configuração de email padrão");
        
        Optional<EmailConfiguration> config = emailConfigurationRepository.findDefaultConfiguration();
        
        if (config.isPresent()) {
            logInfo(String.format("Configuração padrão encontrada: %s", config.get().getProviderDisplayName()));
        } else {
            logWarn("Nenhuma configuração padrão encontrada");
        }
        
        return config;
    }

    /**
     * Cria uma nova configuração de email.
     * 
     * @param configuration a configuração a ser criada
     * @return a configuração criada
     */
    public EmailConfiguration createConfiguration(EmailConfiguration configuration) {
        validateNotNull(configuration, "Configuração não pode ser nula");
        validateNotNull(configuration.getProvider(), "Provider é obrigatório");
        validateNotEmpty(configuration.getHost(), "Host é obrigatório");
        validateNotNull(configuration.getPort(), "Port é obrigatório");
        validateNotEmpty(configuration.getUsername(), "Username é obrigatório");
        validateNotEmpty(configuration.getPassword(), "Password é obrigatório");

        logInfo(String.format("Criando nova configuração de email para provider: %s", configuration.getProvider()));

        // Se for marcada como padrão, remove o padrão das outras
        if (configuration.isDefault()) {
            clearAllDefaultFlags();
        }

        EmailConfiguration saved = emailConfigurationRepository.save(configuration);
        logInfo(String.format("Configuração criada com sucesso. ID: %d", saved.getId()));
        
        return saved;
    }

    /**
     * Atualiza uma configuração existente.
     * 
     * @param id o ID da configuração
     * @param configuration os dados atualizados
     * @return a configuração atualizada
     */
    public EmailConfiguration updateConfiguration(Long id, EmailConfiguration configuration) {
        validateNotNull(id, "ID não pode ser nulo");
        validateNotNull(configuration, "Configuração não pode ser nula");

        logInfo(String.format("Atualizando configuração de email ID: %d", id));

        EmailConfiguration existing = emailConfigurationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Configuração não encontrada com ID: " + id));

        // Atualiza os campos
        existing.setProvider(configuration.getProvider());
        existing.setHost(configuration.getHost());
        existing.setPort(configuration.getPort());
        existing.setUsername(configuration.getUsername());
        existing.setPassword(configuration.getPassword());
        existing.setEnabled(configuration.isEnabled());
        existing.setDescription(configuration.getDescription());
        existing.setUpdatedBy(configuration.getUpdatedBy());

        // Se for marcada como padrão, remove o padrão das outras
        if (configuration.isDefault() && !existing.isDefault()) {
            clearAllDefaultFlags();
            existing.setDefault(true);
        } else if (!configuration.isDefault() && existing.isDefault()) {
            existing.setDefault(false);
        }

        EmailConfiguration updated = emailConfigurationRepository.save(existing);
        logInfo("Configuração atualizada com sucesso");
        
        return updated;
    }

    /**
     * Define uma configuração como padrão.
     * 
     * @param id o ID da configuração
     */
    public void setAsDefault(Long id) {
        validateNotNull(id, "ID não pode ser nulo");

        logInfo(String.format("Definindo configuração ID %d como padrão", id));

        EmailConfiguration config = emailConfigurationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Configuração não encontrada com ID: " + id));

        if (!config.isEnabled()) {
            throw new RuntimeException("Não é possível definir uma configuração desabilitada como padrão");
        }

        // Remove o padrão de todas as outras configurações
        clearAllDefaultFlags();
        
        // Define esta como padrão
        emailConfigurationRepository.setAsDefault(id);
        
        logInfo("Configuração definida como padrão com sucesso");
    }

    /**
     * Remove o status de padrão de todas as configurações.
     */
    private void clearAllDefaultFlags() {
        logDebug("Removendo status de padrão de todas as configurações");
        emailConfigurationRepository.clearAllDefaultFlags();
    }

    /**
     * Busca todas as configurações ativas.
     * 
     * @return lista de configurações ativas
     */
    @Transactional(readOnly = true)
    public List<EmailConfiguration> getActiveConfigurations() {
        logDebug("Buscando todas as configurações ativas");
        return emailConfigurationRepository.findEnabledOrderByDefaultFirst();
    }

    /**
     * Busca todas as configurações.
     * 
     * @return lista de todas as configurações
     */
    @Transactional(readOnly = true)
    public List<EmailConfiguration> getAllConfigurations() {
        logDebug("Buscando todas as configurações");
        return emailConfigurationRepository.findAllOrderByCreatedAtDesc();
    }

    /**
     * Busca configurações por provedor.
     * 
     * @param provider o provedor
     * @return lista de configurações do provedor
     */
    @Transactional(readOnly = true)
    public List<EmailConfiguration> getConfigurationsByProvider(EmailProvider provider) {
        validateNotNull(provider, "Provider não pode ser nulo");
        
        logDebug(String.format("Buscando configurações para provider: %s", provider));
        return emailConfigurationRepository.findByProviderAndEnabledTrue(provider);
    }

    /**
     * Busca uma configuração por ID.
     * 
     * @param id o ID da configuração
     * @return Optional com a configuração
     */
    @Transactional(readOnly = true)
    public Optional<EmailConfiguration> getConfigurationById(Long id) {
        validateNotNull(id, "ID não pode ser nulo");
        
        logDebug(String.format("Buscando configuração por ID: %d", id));
        return emailConfigurationRepository.findById(id);
    }

    /**
     * Busca uma configuração por ID (retorna diretamente ou null).
     * 
     * @param id o ID da configuração
     * @return a configuração ou null se não encontrada
     */
    @Transactional(readOnly = true)
    public EmailConfiguration getConfiguration(Long id) {
        validateNotNull(id, "ID não pode ser nulo");
        
        logDebug(String.format("Buscando configuração por ID: %d", id));
        return emailConfigurationRepository.findById(id).orElse(null);
    }

    /**
     * Habilita ou desabilita uma configuração.
     * 
     * @param id o ID da configuração
     * @param enabled true para habilitar, false para desabilitar
     */
    public void toggleConfiguration(Long id, boolean enabled) {
        validateNotNull(id, "ID não pode ser nulo");

        logInfo(String.format("Alterando status da configuração ID %d para: %s", id, enabled ? "habilitada" : "desabilitada"));

        EmailConfiguration config = emailConfigurationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Configuração não encontrada com ID: " + id));

        // Se está desabilitando a configuração padrão, precisa definir outra como padrão
        if (!enabled && config.isDefault()) {
            config.setDefault(false);
            
            // Busca outra configuração ativa para ser padrão
            List<EmailConfiguration> activeConfigs = emailConfigurationRepository.findByEnabledTrue();
            Optional<EmailConfiguration> newDefault = activeConfigs.stream()
                    .filter(c -> !c.getId().equals(id))
                    .findFirst();
                    
            if (newDefault.isPresent()) {
                newDefault.get().setDefault(true);
                emailConfigurationRepository.save(newDefault.get());
                logInfo(String.format("Nova configuração padrão definida: ID %d", newDefault.get().getId()));
            } else {
                logWarn("Nenhuma configuração ativa disponível para ser padrão");
            }
        }

        config.setEnabled(enabled);
        emailConfigurationRepository.save(config);
        
        logInfo("Status da configuração alterado com sucesso");
    }

    /**
     * Remove uma configuração.
     * 
     * @param id o ID da configuração
     */
    public void deleteConfiguration(Long id) {
        validateNotNull(id, "ID não pode ser nulo");

        logInfo(String.format("Removendo configuração ID: %d", id));

        EmailConfiguration config = emailConfigurationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Configuração não encontrada com ID: " + id));

        // Se está removendo a configuração padrão, define outra como padrão
        if (config.isDefault()) {
            List<EmailConfiguration> activeConfigs = emailConfigurationRepository.findByEnabledTrue();
            Optional<EmailConfiguration> newDefault = activeConfigs.stream()
                    .filter(c -> !c.getId().equals(id))
                    .findFirst();
                    
            if (newDefault.isPresent()) {
                newDefault.get().setDefault(true);
                emailConfigurationRepository.save(newDefault.get());
                logInfo(String.format("Nova configuração padrão definida: ID %d", newDefault.get().getId()));
            }
        }

        emailConfigurationRepository.deleteById(id);
        logInfo("Configuração removida com sucesso");
    }

    /**
     * Verifica se existe uma configuração padrão.
     * 
     * @return true se existe configuração padrão
     */
    @Transactional(readOnly = true)
    public boolean hasDefaultConfiguration() {
        return emailConfigurationRepository.existsDefaultConfiguration();
    }

    /**
     * Cria configuração padrão do Mailtrap se não existir nenhuma.
     */
    public void ensureDefaultConfiguration() {
        if (!hasDefaultConfiguration()) {
            logInfo("Criando configuração padrão do Mailtrap");
            
            EmailConfiguration mailtrapConfig = new EmailConfiguration();
            mailtrapConfig.setProvider(EmailProvider.MAILTRAP);
            mailtrapConfig.setHost("sandbox.smtp.mailtrap.io");
            mailtrapConfig.setPort(2525);
            mailtrapConfig.setUsername("your_mailtrap_username");
            mailtrapConfig.setPassword("your_mailtrap_password");
            mailtrapConfig.setDescription("Configuração padrão do Mailtrap para desenvolvimento");
            mailtrapConfig.setDefault(true);
            mailtrapConfig.setEnabled(true);
            mailtrapConfig.setCreatedBy("SYSTEM");
            
            emailConfigurationRepository.save(mailtrapConfig);
            logInfo("Configuração padrão do Mailtrap criada");
        }
    }

    /**
     * Obtém estatísticas das configurações.
     * 
     * @return mapa com estatísticas
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getStatistics() {
        logDebug("Gerando estatísticas das configurações de email");
        
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        long totalConfigs = emailConfigurationRepository.count();
        long activeConfigs = emailConfigurationRepository.findByEnabledTrue().size();
        long mailtrapConfigs = emailConfigurationRepository.countByProviderAndEnabledTrue(EmailProvider.MAILTRAP);
        long gmailConfigs = emailConfigurationRepository.countByProviderAndEnabledTrue(EmailProvider.GMAIL);
        boolean hasDefault = hasDefaultConfiguration();
        
        stats.put("total", totalConfigs);
        stats.put("active", activeConfigs);
        stats.put("mailtrap", mailtrapConfigs);
        stats.put("gmail", gmailConfigs);
        stats.put("hasDefault", hasDefault);
        
        Optional<EmailConfiguration> defaultConfig = getDefaultConfiguration();
        if (defaultConfig.isPresent()) {
            stats.put("defaultProvider", defaultConfig.get().getProvider().getDisplayName());
        }
        
        return stats;
    }

    /**
     * Obtém configurações padrão para um provedor específico.
     * 
     * @param provider o provedor
     * @return configuração com valores padrão
     */
    @Transactional(readOnly = true)
    public EmailConfiguration getProviderDefaults(EmailProvider provider) {
        validateNotNull(provider, "Provider não pode ser nulo");
        
        logDebug(String.format("Obtendo configurações padrão para provider: %s", provider));
        
        EmailConfiguration config = new EmailConfiguration();
        config.setProvider(provider);
        config.setHost(provider.getDefaultHost());
        config.setPort(provider.getDefaultPort());
        config.setTlsEnabled(provider.isDefaultTlsEnabled());
        config.setAuthRequired(provider.isDefaultAuthRequired());
        config.setEnabled(true);
        config.setDefault(false);
        
        return config;
    }
}