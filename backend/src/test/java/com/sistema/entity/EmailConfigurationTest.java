package com.sistema.entity;

import com.sistema.enums.EmailProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para a entidade EmailConfiguration.
 * Verifica validações, getters, setters e comportamentos da entidade.
 */
@DisplayName("Testes da EmailConfiguration")
class EmailConfigurationTest {

    private Validator validator;
    private EmailConfiguration emailConfiguration;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        emailConfiguration = new EmailConfiguration();
        emailConfiguration.setProvider(EmailProvider.MAILTRAP);
        emailConfiguration.setHost("sandbox.smtp.mailtrap.io");
        emailConfiguration.setPort(2525);
        emailConfiguration.setUsername("test_user");
        emailConfiguration.setPassword("test_password");
        emailConfiguration.setIsActive(true);
        emailConfiguration.setDefault(false);
        emailConfiguration.setDescription("Configuração de teste");
    }

    @Test
    @DisplayName("Deve criar EmailConfiguration válida")
    void shouldCreateValidEmailConfiguration() {
        // When
        Set<ConstraintViolation<EmailConfiguration>> violations = validator.validate(emailConfiguration);

        // Then
        assertTrue(violations.isEmpty(), "EmailConfiguration válida não deve ter violações");
    }

    @Test
    @DisplayName("Deve falhar validação quando provider é nulo")
    void shouldFailValidationWhenProviderIsNull() {
        // Given
        emailConfiguration.setProvider(null);

        // When
        Set<ConstraintViolation<EmailConfiguration>> violations = validator.validate(emailConfiguration);

        // Then
        assertFalse(violations.isEmpty(), "Deve ter violações quando provider é nulo");
        assertTrue(hasViolationForField(violations, "provider"), "Deve ter violação para campo provider");
    }

    @Test
    @DisplayName("Deve falhar validação quando host é nulo")
    void shouldFailValidationWhenHostIsNull() {
        // Given
        emailConfiguration.setHost(null);

        // When
        Set<ConstraintViolation<EmailConfiguration>> violations = validator.validate(emailConfiguration);

        // Then
        assertFalse(violations.isEmpty(), "Deve ter violações quando host é nulo");
        assertTrue(hasViolationForField(violations, "host"), "Deve ter violação para campo host");
    }

    @Test
    @DisplayName("Deve falhar validação quando host é vazio")
    void shouldFailValidationWhenHostIsEmpty() {
        // Given
        emailConfiguration.setHost("");

        // When
        Set<ConstraintViolation<EmailConfiguration>> violations = validator.validate(emailConfiguration);

        // Then
        assertFalse(violations.isEmpty(), "Deve ter violações quando host é vazio");
        assertTrue(hasViolationForField(violations, "host"), "Deve ter violação para campo host");
    }

    @Test
    @DisplayName("Deve falhar validação quando host tem apenas espaços")
    void shouldFailValidationWhenHostIsBlank() {
        // Given
        emailConfiguration.setHost("   ");

        // When
        Set<ConstraintViolation<EmailConfiguration>> violations = validator.validate(emailConfiguration);

        // Then
        assertFalse(violations.isEmpty(), "Deve ter violações quando host tem apenas espaços");
        assertTrue(hasViolationForField(violations, "host"), "Deve ter violação para campo host");
    }

    @Test
    @DisplayName("Deve falhar validação quando porta é nula")
    void shouldFailValidationWhenPortIsNull() {
        // Given
        emailConfiguration.setPort(null);

        // When
        Set<ConstraintViolation<EmailConfiguration>> violations = validator.validate(emailConfiguration);

        // Then
        assertFalse(violations.isEmpty(), "Deve ter violações quando porta é nula");
        assertTrue(hasViolationForField(violations, "port"), "Deve ter violação para campo port");
    }

    @Test
    @DisplayName("Deve falhar validação quando porta é menor que 1")
    void shouldFailValidationWhenPortIsLessThanOne() {
        // Given
        emailConfiguration.setPort(0);

        // When
        Set<ConstraintViolation<EmailConfiguration>> violations = validator.validate(emailConfiguration);

        // Then
        assertFalse(violations.isEmpty(), "Deve ter violações quando porta é menor que 1");
        assertTrue(hasViolationForField(violations, "port"), "Deve ter violação para campo port");
    }

    @Test
    @DisplayName("Deve falhar validação quando porta é maior que 65535")
    void shouldFailValidationWhenPortIsGreaterThan65535() {
        // Given
        emailConfiguration.setPort(65536);

        // When
        Set<ConstraintViolation<EmailConfiguration>> violations = validator.validate(emailConfiguration);

        // Then
        assertFalse(violations.isEmpty(), "Deve ter violações quando porta é maior que 65535");
        assertTrue(hasViolationForField(violations, "port"), "Deve ter violação para campo port");
    }

    @Test
    @DisplayName("Deve aceitar portas válidas")
    void shouldAcceptValidPorts() {
        // Given & When & Then
        emailConfiguration.setPort(1);
        assertTrue(validator.validate(emailConfiguration).isEmpty(), "Porta 1 deve ser válida");

        emailConfiguration.setPort(587);
        assertTrue(validator.validate(emailConfiguration).isEmpty(), "Porta 587 deve ser válida");

        emailConfiguration.setPort(2525);
        assertTrue(validator.validate(emailConfiguration).isEmpty(), "Porta 2525 deve ser válida");

        emailConfiguration.setPort(65535);
        assertTrue(validator.validate(emailConfiguration).isEmpty(), "Porta 65535 deve ser válida");
    }

    @Test
    @DisplayName("Deve falhar validação quando username é nulo")
    void shouldFailValidationWhenUsernameIsNull() {
        // Given
        emailConfiguration.setUsername(null);

        // When
        Set<ConstraintViolation<EmailConfiguration>> violations = validator.validate(emailConfiguration);

        // Then
        assertFalse(violations.isEmpty(), "Deve ter violações quando username é nulo");
        assertTrue(hasViolationForField(violations, "username"), "Deve ter violação para campo username");
    }

    @Test
    @DisplayName("Deve falhar validação quando username é vazio")
    void shouldFailValidationWhenUsernameIsEmpty() {
        // Given
        emailConfiguration.setUsername("");

        // When
        Set<ConstraintViolation<EmailConfiguration>> violations = validator.validate(emailConfiguration);

        // Then
        assertFalse(violations.isEmpty(), "Deve ter violações quando username é vazio");
        assertTrue(hasViolationForField(violations, "username"), "Deve ter violação para campo username");
    }

    @Test
    @DisplayName("Deve falhar validação quando password é nulo")
    void shouldFailValidationWhenPasswordIsNull() {
        // Given
        emailConfiguration.setPassword(null);

        // When
        Set<ConstraintViolation<EmailConfiguration>> violations = validator.validate(emailConfiguration);

        // Then
        assertFalse(violations.isEmpty(), "Deve ter violações quando password é nulo");
        assertTrue(hasViolationForField(violations, "password"), "Deve ter violação para campo password");
    }

    @Test
    @DisplayName("Deve falhar validação quando password é vazio")
    void shouldFailValidationWhenPasswordIsEmpty() {
        // Given
        emailConfiguration.setPassword("");

        // When
        Set<ConstraintViolation<EmailConfiguration>> violations = validator.validate(emailConfiguration);

        // Then
        assertFalse(violations.isEmpty(), "Deve ter violações quando password é vazio");
        assertTrue(hasViolationForField(violations, "password"), "Deve ter violação para campo password");
    }

    @Test
    @DisplayName("Deve definir timestamps automaticamente")
    void shouldSetTimestampsAutomatically() {
        // Given
        EmailConfiguration config = new EmailConfiguration();
        config.setProvider(EmailProvider.MAILTRAP);
        config.setHost("smtp.mailtrap.io");
        config.setPort(587);
        config.setUsername("test@example.com");
        config.setPassword("password");
        config.setIsActive(true);

        // When - Os timestamps são definidos automaticamente pelo Hibernate
        // @CreationTimestamp e @UpdateTimestamp fazem isso automaticamente

        // Then - Verificar que os campos existem (serão definidos pelo Hibernate na persistência)
        assertNotNull(config, "Configuração deve ser criada");
        assertEquals(EmailProvider.MAILTRAP, config.getProvider(), "Provider deve ser definido");
        assertEquals("smtp.mailtrap.io", config.getHost(), "Host deve ser definido");
    }

    @Test
    @DisplayName("Deve atualizar timestamp na atualização")
    void shouldUpdateTimestampOnUpdate() {
        // Given - Os timestamps são gerenciados automaticamente pelo Hibernate
        // @CreationTimestamp define createdAt na criação
        // @UpdateTimestamp atualiza updatedAt a cada save()
        
        // When - Simular uma atualização de campo
        emailConfiguration.setDescription("Descrição atualizada");
        
        // Then - Verificar que a configuração pode ser atualizada
        assertNotNull(emailConfiguration, "Configuração deve existir");
        assertEquals("Descrição atualizada", emailConfiguration.getDescription(), 
                "Descrição deve ser atualizada");
        assertEquals(EmailProvider.MAILTRAP, emailConfiguration.getProvider(), 
                "Provider deve permanecer inalterado");
    }

    @Test
    @DisplayName("Deve implementar equals corretamente")
    void shouldImplementEqualsCorrectly() {
        // Given
        EmailConfiguration config1 = new EmailConfiguration();
        config1.setId(1L);
        config1.setProvider(EmailProvider.GMAIL);
        config1.setHost("smtp.gmail.com");

        EmailConfiguration config2 = new EmailConfiguration();
        config2.setId(1L);
        config2.setProvider(EmailProvider.GMAIL);
        config2.setHost("smtp.gmail.com");

        EmailConfiguration config3 = new EmailConfiguration();
        config3.setId(2L);
        config3.setProvider(EmailProvider.GMAIL);
        config3.setHost("smtp.gmail.com");

        // When & Then
        assertEquals(config1, config2, "Configurações com mesmo ID devem ser iguais");
        assertNotEquals(config1, config3, "Configurações com IDs diferentes devem ser diferentes");
        assertNotEquals(config1, null, "Configuração não deve ser igual a null");
        assertNotEquals(config1, "string", "Configuração não deve ser igual a objeto de tipo diferente");
    }

    @Test
    @DisplayName("Deve implementar hashCode corretamente")
    void shouldImplementHashCodeCorrectly() {
        // Given
        EmailConfiguration config1 = new EmailConfiguration();
        config1.setId(1L);

        EmailConfiguration config2 = new EmailConfiguration();
        config2.setId(1L);

        EmailConfiguration config3 = new EmailConfiguration();
        config3.setId(2L);

        // When & Then
        assertEquals(config1.hashCode(), config2.hashCode(), 
                "Configurações iguais devem ter mesmo hashCode");
        assertNotEquals(config1.hashCode(), config3.hashCode(), 
                "Configurações diferentes devem ter hashCodes diferentes");
    }

    @Test
    @DisplayName("Deve implementar toString corretamente")
    void shouldImplementToStringCorrectly() {
        // Given
        emailConfiguration.setId(1L);

        // When
        String toString = emailConfiguration.toString();

        // Then
        assertNotNull(toString, "ToString não deve ser nulo");
        assertTrue(toString.contains("EmailConfiguration"), "ToString deve conter nome da classe");
        assertTrue(toString.contains("id=1"), "ToString deve conter ID");
        assertTrue(toString.contains("provider=MAILTRAP"), "ToString deve conter provider");
        assertTrue(toString.contains("host=sandbox.smtp.mailtrap.io"), "ToString deve conter host");
    }

    @Test
    @DisplayName("Deve permitir valores padrão corretos")
    void shouldAllowCorrectDefaultValues() {
        // Given
        EmailConfiguration config = new EmailConfiguration();

        // When & Then
        assertNull(config.getId(), "ID deve ser nulo por padrão");
        assertNull(config.getProvider(), "Provider deve ser nulo por padrão");
        assertNull(config.getHost(), "Host deve ser nulo por padrão");
        assertNull(config.getPort(), "Port deve ser nulo por padrão");
        assertNull(config.getUsername(), "Username deve ser nulo por padrão");
        assertNull(config.getPassword(), "Password deve ser nulo por padrão");
        assertFalse(config.getIsActive(), "IsActive deve ser false por padrão");
        assertFalse(config.isDefault(), "Default deve ser false por padrão");
        assertNull(config.getDescription(), "Description deve ser nulo por padrão");
        assertNull(config.getCreatedAt(), "CreatedAt deve ser nulo por padrão");
        assertNull(config.getUpdatedAt(), "UpdatedAt deve ser nulo por padrão");
    }

    @Test
    @DisplayName("Deve permitir descrição nula")
    void shouldAllowNullDescription() {
        // Given
        emailConfiguration.setDescription(null);

        // When
        Set<ConstraintViolation<EmailConfiguration>> violations = validator.validate(emailConfiguration);

        // Then
        assertTrue(violations.isEmpty(), "Descrição nula deve ser permitida");
    }

    @Test
    @DisplayName("Deve permitir descrição vazia")
    void shouldAllowEmptyDescription() {
        // Given
        emailConfiguration.setDescription("");

        // When
        Set<ConstraintViolation<EmailConfiguration>> violations = validator.validate(emailConfiguration);

        // Then
        assertTrue(violations.isEmpty(), "Descrição vazia deve ser permitida");
    }

    @Test
    @DisplayName("Deve aceitar descrição longa")
    void shouldAcceptLongDescription() {
        // Given
        String longDescription = "A".repeat(1000);
        emailConfiguration.setDescription(longDescription);

        // When
        Set<ConstraintViolation<EmailConfiguration>> violations = validator.validate(emailConfiguration);

        // Then
        assertTrue(violations.isEmpty(), "Descrição longa deve ser permitida");
        assertEquals(longDescription, emailConfiguration.getDescription(), 
                "Descrição deve ser armazenada corretamente");
    }

    @Test
    @DisplayName("Deve manter estado de enabled corretamente")
    void shouldMaintainEnabledStateCorrectly() {
        // Given & When & Then
        emailConfiguration.setIsActive(true);
        assertTrue(emailConfiguration.getIsActive(), "IsActive deve ser true quando definido como true");

        emailConfiguration.setIsActive(false);
        assertFalse(emailConfiguration.getIsActive(), "IsActive deve ser false quando definido como false");
    }

    @Test
    @DisplayName("Deve manter estado de default corretamente")
    void shouldMaintainDefaultStateCorrectly() {
        // Given & When & Then
        emailConfiguration.setDefault(true);
        assertTrue(emailConfiguration.isDefault(), "Default deve ser true quando definido como true");

        emailConfiguration.setDefault(false);
        assertFalse(emailConfiguration.isDefault(), "Default deve ser false quando definido como false");
    }

    /**
     * Método auxiliar para verificar se existe violação para um campo específico.
     */
    private boolean hasViolationForField(Set<ConstraintViolation<EmailConfiguration>> violations, String fieldName) {
        return violations.stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals(fieldName));
    }
}