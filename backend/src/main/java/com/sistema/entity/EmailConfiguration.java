package com.sistema.entity;

import com.sistema.enums.EmailProvider;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidade para configurações de email do sistema.
 * Permite alternar entre diferentes provedores de email (Mailtrap, Gmail).
 */
@Entity(name = "EmailConfiguration")
@Table(name = "email_configurations")
public class EmailConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Nome da configuração é obrigatório")
    @Size(max = 100, message = "Nome da configuração deve ter no máximo 100 caracteres")
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    @Column(name = "description")
    private String description;

    @NotNull(message = "Provedor de email é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private EmailProvider provider;

    @NotBlank(message = "Host SMTP é obrigatório")
    @Size(max = 255, message = "Host SMTP deve ter no máximo 255 caracteres")
    @Column(name = "smtp_host", nullable = false)
    private String smtpHost;

    @NotNull(message = "Porta SMTP é obrigatória")
    @Min(value = 1, message = "Porta SMTP deve ser maior que 0")
    @Max(value = 65535, message = "Porta SMTP deve ser menor que 65536")
    @Column(name = "smtp_port", nullable = false)
    private Integer smtpPort;

    @NotBlank(message = "Username é obrigatório")
    @Size(max = 255, message = "Username deve ter no máximo 255 caracteres")
    @Column(name = "username", nullable = false)
    private String username;

    @NotBlank(message = "Password é obrigatório")
    @Size(max = 500, message = "Password deve ter no máximo 500 caracteres")
    @Column(name = "password", nullable = false, length = 500)
    private String password; // Será criptografado
    
    @Column(name = "tls_enabled", nullable = false)
    private Boolean tlsEnabled = true;
    
    @Column(name = "auth_required", nullable = false)
    private Boolean authRequired = true;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Email(message = "Email do remetente deve ser válido")
    @Size(max = 255, message = "Email do remetente deve ter no máximo 255 caracteres")
    @Column(name = "from_email")
    private String fromEmail;
    
    @Size(max = 255, message = "Nome do remetente deve ter no máximo 255 caracteres")
    @Column(name = "from_name")
    private String fromName;
    
    @Min(value = 1000, message = "Timeout de conexão deve ser pelo menos 1000ms")
    @Column(name = "connection_timeout")
    private Integer connectionTimeout = 5000; // 5 segundos
    
    @Min(value = 1000, message = "Timeout de leitura deve ser pelo menos 1000ms")
    @Column(name = "read_timeout")
    private Integer readTimeout = 10000; // 10 segundos
    
    @Min(value = 1, message = "Máximo de tentativas deve ser pelo menos 1")
    @Max(value = 10, message = "Máximo de tentativas deve ser no máximo 10")
    @Column(name = "max_retry_attempts")
    private Integer maxRetryAttempts = 3;
    
    @Column(name = "test_connection_on_startup")
    private Boolean testConnectionOnStartup = false;
    
    @Column(name = "last_test_date")
    private LocalDateTime lastTestDate;
    
    @Column(name = "last_test_success")
    private Boolean lastTestSuccess;
    
    @Size(max = 500, message = "Resultado do último teste deve ter no máximo 500 caracteres")
    @Column(name = "last_test_result", length = 500)
    private String lastTestResult;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    // Construtores
    public EmailConfiguration() {}
    
    public EmailConfiguration(String name, EmailProvider provider, String smtpHost, 
                            Integer smtpPort, String username, String password) {
        this.name = name;
        this.provider = provider;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.username = username;
        this.password = password;
        this.tlsEnabled = provider.isDefaultTlsEnabled();
        this.authRequired = provider.isDefaultAuthRequired();
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public EmailProvider getProvider() {
        return provider;
    }
    
    public void setProvider(EmailProvider provider) {
        this.provider = provider;
    }
    
    public String getSmtpHost() {
        return smtpHost;
    }
    
    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }
    
    public String getHost() {
        return smtpHost;
    }
    
    public void setHost(String host) {
        this.smtpHost = host;
    }
    
    public Integer getSmtpPort() {
        return smtpPort;
    }
    
    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }
    
    public Integer getPort() {
        return smtpPort;
    }
    
    public void setPort(Integer port) {
        this.smtpPort = port;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Boolean getTlsEnabled() {
        return tlsEnabled;
    }
    
    public void setTlsEnabled(Boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
    }
    
    public Boolean getAuthRequired() {
        return authRequired;
    }
    
    public void setAuthRequired(Boolean authRequired) {
        this.authRequired = authRequired;
    }
    
    public Boolean getIsDefault() {
        return isDefault;
    }
    
    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
    
    public boolean isDefault() {
        return isDefault != null && isDefault;
    }
    
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public boolean isEnabled() {
        return isActive != null && isActive;
    }
    
    public void setEnabled(boolean enabled) {
        this.isActive = enabled;
    }
    
    public String getFromEmail() {
        return fromEmail;
    }
    
    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }
    
    public String getFromName() {
        return fromName;
    }
    
    public void setFromName(String fromName) {
        this.fromName = fromName;
    }
    
    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public Integer getReadTimeout() {
        return readTimeout;
    }
    
    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }
    
    public Integer getMaxRetryAttempts() {
        return maxRetryAttempts;
    }
    
    public void setMaxRetryAttempts(Integer maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }
    
    public Boolean getTestConnectionOnStartup() {
        return testConnectionOnStartup;
    }
    
    public void setTestConnectionOnStartup(Boolean testConnectionOnStartup) {
        this.testConnectionOnStartup = testConnectionOnStartup;
    }
    
    public LocalDateTime getLastTestDate() {
        return lastTestDate;
    }
    
    public void setLastTestDate(LocalDateTime lastTestDate) {
        this.lastTestDate = lastTestDate;
    }
    
    public Boolean getLastTestSuccess() {
        return lastTestSuccess;
    }
    
    public void setLastTestSuccess(Boolean lastTestSuccess) {
        this.lastTestSuccess = lastTestSuccess;
    }
    
    public String getLastTestResult() {
        return lastTestResult;
    }
    
    public void setLastTestResult(String lastTestResult) {
        this.lastTestResult = lastTestResult;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    // Métodos de conveniência
    public boolean isMailtrap() {
        return this.provider == EmailProvider.MAILTRAP;
    }

    public boolean isGmail() {
        return this.provider == EmailProvider.GMAIL;
    }

    public String getProviderDisplayName() {
        return this.provider != null ? this.provider.getDisplayName() : "Desconhecido";
    }

    @Override
    public String toString() {
        return "EmailConfiguration{" +
                "id=" + id +
                ", provider=" + provider +
                ", host='" + smtpHost + '\'' +
                ", port=" + smtpPort +
                ", username='" + username + '\'' +
                ", enabled=" + isActive +
                ", isDefault=" + isDefault +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}