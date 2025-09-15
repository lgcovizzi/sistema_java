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

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private Validator validator;
    private UserModel user;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        user = new UserModel();
        user.setNome("João");
        user.setSobrenome("Silva");
        user.setEmail("joao.silva@example.com");
        user.setSenha("senha123");
        user.setRole(UserRole.USUARIO);
    }

    @Test
    @DisplayName("Deve criar usuário com todos os campos obrigatórios")
    void shouldCreateUserWithAllRequiredFields() {
        // Given & When
        UserModel newUser = new UserModel("Maria", "Santos", "maria.santos@example.com", "senha456", UserRole.ASSOCIADO);
        newUser.prePersist(); // Simula o comportamento do JPA
        
        // Then
        assertThat(newUser.getNome()).isEqualTo("Maria");
        assertThat(newUser.getSobrenome()).isEqualTo("Santos");
        assertThat(newUser.getEmail()).isEqualTo("maria.santos@example.com");
        assertThat(newUser.getSenha()).isEqualTo("senha456");
        assertThat(newUser.getRole()).isEqualTo(UserRole.ASSOCIADO);
        assertThat(newUser.isEmailVerificado()).isFalse();
        assertThat(newUser.getDataCriacao()).isNotNull();
    }

    @Test
    @DisplayName("Deve validar email com formato correto")
    void shouldValidateEmailWithCorrectFormat() {
        // Given
        user.setEmail("usuario@dominio.com");
        
        // When
        Set<ConstraintViolation<UserModel>> violations = validator.validate(user);
        
        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Deve rejeitar email com formato inválido")
    void shouldRejectEmailWithInvalidFormat() {
        // Given
        user.setEmail("email-invalido");
        
        // When
        Set<ConstraintViolation<UserModel>> violations = validator.validate(user);
        
        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("deve ser um endereço de e-mail bem formado");
    }

    @Test
    @DisplayName("Deve rejeitar nome vazio")
    void shouldRejectEmptyNome() {
        // Given
        user.setNome("");
        
        // When
        Set<ConstraintViolation<UserModel>> violations = validator.validate(user);
        
        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("não deve estar em branco");
    }

    @Test
    @DisplayName("Deve rejeitar sobrenome vazio")
    void shouldRejectEmptySobrenome() {
        // Given
        user.setSobrenome("");
        
        // When
        Set<ConstraintViolation<UserModel>> violations = validator.validate(user);
        
        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("não deve estar em branco");
    }

    @Test
    @DisplayName("Deve rejeitar senha muito curta")
    void shouldRejectShortPassword() {
        // Given
        user.setSenha("123");
        
        // When
        Set<ConstraintViolation<UserModel>> violations = validator.validate(user);
        
        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("tamanho deve ser entre 6 e 100");
    }

    @Test
    @DisplayName("Deve definir data de criação automaticamente")
    void shouldSetCreationDateAutomatically() {
        // Given
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        
        // When
        UserModel newUser = new UserModel();
        newUser.prePersist();
        
        // Then
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertThat(newUser.getDataCriacao()).isBetween(before, after);
    }

    @Test
    @DisplayName("Deve permitir verificação de email")
    void shouldAllowEmailVerification() {
        // Given
        assertThat(user.isEmailVerificado()).isFalse();
        
        // When
        user.verificarEmail();
        
        // Then
        assertThat(user.isEmailVerificado()).isTrue();
        assertThat(user.getDataVerificacaoEmail()).isNotNull();
    }

    @Test
    @DisplayName("Deve gerar nome completo corretamente")
    void shouldGenerateFullNameCorrectly() {
        // Given & When
        String nomeCompleto = user.getNomeCompleto();
        
        // Then
        assertThat(nomeCompleto).isEqualTo("João Silva");
    }

    @Test
    @DisplayName("Deve implementar equals e hashCode baseado no email")
    void shouldImplementEqualsAndHashCodeBasedOnEmail() {
        // Given
        UserModel user1 = new UserModel("João", "Silva", "joao@example.com", "senha", UserRole.USUARIO);
        UserModel user2 = new UserModel("Maria", "Santos", "joao@example.com", "outrasenha", UserRole.ASSOCIADO);
        UserModel user3 = new UserModel("João", "Silva", "maria@example.com", "senha", UserRole.USUARIO);
        
        // When & Then
        assertThat(user1).isEqualTo(user2); // Mesmo email
        assertThat(user1).isNotEqualTo(user3); // Email diferente
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        assertThat(user1.hashCode()).isNotEqualTo(user3.hashCode());
    }
}