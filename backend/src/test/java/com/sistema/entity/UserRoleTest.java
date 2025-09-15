package com.sistema.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

@DisplayName("UserRole Enum Tests")
class UserRoleTest {

    @Test
    @DisplayName("Deve ter valores USER e ADMIN")
    void shouldHaveUserAndAdminValues() {
        // When & Then
        assertThat(UserRole.values()).containsExactly(UserRole.USER, UserRole.ADMIN);
    }

    @Test
    @DisplayName("Deve retornar descrição correta para USER")
    void shouldReturnCorrectDescriptionForUser() {
        // When
        String description = UserRole.USER.getDescription();

        // Then
        assertThat(description).isEqualTo("Usuário comum");
    }

    @Test
    @DisplayName("Deve retornar descrição correta para ADMIN")
    void shouldReturnCorrectDescriptionForAdmin() {
        // When
        String description = UserRole.ADMIN.getDescription();

        // Then
        assertThat(description).isEqualTo("Administrador");
    }

    @Test
    @DisplayName("Deve retornar authority correta para USER")
    void shouldReturnCorrectAuthorityForUser() {
        // When
        String authority = UserRole.USER.getAuthority();

        // Then
        assertThat(authority).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Deve retornar authority correta para ADMIN")
    void shouldReturnCorrectAuthorityForAdmin() {
        // When
        String authority = UserRole.ADMIN.getAuthority();

        // Then
        assertThat(authority).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Deve verificar se role tem permissão de admin")
    void shouldCheckIfRoleHasAdminPermission() {
        // When & Then
        assertThat(UserRole.ADMIN.hasAdminPermission()).isTrue();
        assertThat(UserRole.USER.hasAdminPermission()).isFalse();
    }

    @Test
    @DisplayName("Deve converter string para UserRole")
    void shouldConvertStringToUserRole() {
        // When & Then
        assertThat(UserRole.fromString("USER")).isEqualTo(UserRole.USER);
        assertThat(UserRole.fromString("ADMIN")).isEqualTo(UserRole.ADMIN);
        assertThat(UserRole.fromString("user")).isEqualTo(UserRole.USER);
        assertThat(UserRole.fromString("admin")).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("Deve lançar exceção para string inválida")
    void shouldThrowExceptionForInvalidString() {
        // When & Then
        assertThatThrownBy(() -> UserRole.fromString("INVALID"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Role inválida: INVALID");
    }

    @Test
    @DisplayName("Deve lançar exceção para string nula")
    void shouldThrowExceptionForNullString() {
        // When & Then
        assertThatThrownBy(() -> UserRole.fromString(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Role não pode ser nula ou vazia");
    }

    @Test
    @DisplayName("Deve lançar exceção para string vazia")
    void shouldThrowExceptionForEmptyString() {
        // When & Then
        assertThatThrownBy(() -> UserRole.fromString(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Role não pode ser nula ou vazia");
    }
}