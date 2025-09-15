package com.sistema.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationTest {

    private Validator validator;
    private EmailVerification emailVerification;
    private UserModel user;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        user = new UserModel("João", "Silva", "joao.silva@example.com", "senha123", UserRole.USUARIO);
        user.prePersist();
        
        emailVerification = new EmailVerification();
        emailVerification.setUser(user);
        emailVerification.setToken(UUID.randomUUID().toString());
    }

    @Test
    @DisplayName("Deve criar verificação de email com token e usuário")
    void shouldCreateEmailVerificationWithTokenAndUser() {
        // Given
        String token = "test-token-123";
        
        // When
        EmailVerification verification = new EmailVerification(user, token);
        verification.prePersist();
        
        // Then
        assertThat(verification.getUser()).isEqualTo(user);
        assertThat(verification.getToken()).isEqualTo(token);
        assertThat(verification.getDataCriacao()).isNotNull();
        assertThat(verification.getDataExpiracao()).isNotNull();
        assertThat(verification.isUsado()).isFalse();
    }

    @Test
    @DisplayName("Deve definir data de expiração para 24 horas após criação")
    void shouldSetExpirationDateTo24HoursAfterCreation() {
        // Given
        LocalDateTime before = LocalDateTime.now().plusHours(23).plusMinutes(59);
        LocalDateTime after = LocalDateTime.now().plusHours(24).plusMinutes(1);
        
        // When
        EmailVerification verification = new EmailVerification(user, "token");
        verification.prePersist();
        
        // Then
        assertThat(verification.getDataExpiracao()).isBetween(before, after);
    }

    @Test
    @DisplayName("Deve validar token não vazio")
    void shouldValidateTokenNotEmpty() {
        // Given
        emailVerification.setToken("");
        
        // When
        Set<ConstraintViolation<EmailVerification>> violations = validator.validate(emailVerification);
        
        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("não deve estar em branco");
    }

    @Test
    @DisplayName("Deve validar usuário não nulo")
    void shouldValidateUserNotNull() {
        // Given
        emailVerification.setUser(null);
        
        // When
        Set<ConstraintViolation<EmailVerification>> violations = validator.validate(emailVerification);
        
        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("não deve ser nulo");
    }

    @Test
    @DisplayName("Deve verificar se token está expirado")
    void shouldCheckIfTokenIsExpired() {
        // Given
        emailVerification.setDataExpiracao(LocalDateTime.now().minusHours(1));
        
        // When
        boolean isExpired = emailVerification.isExpirado();
        
        // Then
        assertThat(isExpired).isTrue();
    }

    @Test
    @DisplayName("Deve verificar se token não está expirado")
    void shouldCheckIfTokenIsNotExpired() {
        // Given
        emailVerification.setDataExpiracao(LocalDateTime.now().plusHours(1));
        
        // When
        boolean isExpired = emailVerification.isExpirado();
        
        // Then
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("Deve marcar token como usado")
    void shouldMarkTokenAsUsed() {
        // Given
        assertThat(emailVerification.isUsado()).isFalse();
        
        // When
        emailVerification.marcarComoUsado();
        
        // Then
        assertThat(emailVerification.isUsado()).isTrue();
        assertThat(emailVerification.getDataUso()).isNotNull();
    }

    @Test
    @DisplayName("Deve verificar se token é válido (não expirado e não usado)")
    void shouldCheckIfTokenIsValid() {
        // Given
        emailVerification.setDataExpiracao(LocalDateTime.now().plusHours(1));
        emailVerification.setUsado(false);
        
        // When
        boolean isValid = emailVerification.isValido();
        
        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Deve verificar se token é inválido quando expirado")
    void shouldCheckIfTokenIsInvalidWhenExpired() {
        // Given
        emailVerification.setDataExpiracao(LocalDateTime.now().minusHours(1));
        emailVerification.setUsado(false);
        
        // When
        boolean isValid = emailVerification.isValido();
        
        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Deve verificar se token é inválido quando usado")
    void shouldCheckIfTokenIsInvalidWhenUsed() {
        // Given
        emailVerification.setDataExpiracao(LocalDateTime.now().plusHours(1));
        emailVerification.setUsado(true);
        
        // When
        boolean isValid = emailVerification.isValido();
        
        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Deve gerar token único automaticamente")
    void shouldGenerateUniqueTokenAutomatically() {
        // Given & When
        EmailVerification verification1 = EmailVerification.criarParaUsuario(user);
        EmailVerification verification2 = EmailVerification.criarParaUsuario(user);
        
        // Then
        assertThat(verification1.getToken()).isNotNull();
        assertThat(verification2.getToken()).isNotNull();
        assertThat(verification1.getToken()).isNotEqualTo(verification2.getToken());
        assertThat(verification1.getToken()).hasSize(36); // UUID length
    }

    @Test
    @DisplayName("Deve implementar equals e hashCode baseado no token")
    void shouldImplementEqualsAndHashCodeBasedOnToken() {
        // Given
        EmailVerification verification1 = new EmailVerification(user, "token-123");
        EmailVerification verification2 = new EmailVerification(user, "token-123");
        EmailVerification verification3 = new EmailVerification(user, "token-456");
        
        // When & Then
        assertThat(verification1).isEqualTo(verification2); // Mesmo token
        assertThat(verification1).isNotEqualTo(verification3); // Token diferente
        assertThat(verification1.hashCode()).isEqualTo(verification2.hashCode());
        assertThat(verification1.hashCode()).isNotEqualTo(verification3.hashCode());
    }
}