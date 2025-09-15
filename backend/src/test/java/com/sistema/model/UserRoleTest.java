package com.sistema.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserRoleTest {

    @Test
    @DisplayName("Deve conter todos os tipos de usuário definidos")
    void shouldContainAllDefinedUserTypes() {
        // Given & When
        UserRole[] roles = UserRole.values();
        
        // Then
        assertThat(roles).hasSize(6);
        assertThat(roles).contains(
            UserRole.USUARIO,
            UserRole.ASSOCIADO,
            UserRole.COLABORADOR,
            UserRole.DIRETOR,
            UserRole.FUNDADOR,
            UserRole.ADMINISTRADOR
        );
    }

    @Test
    @DisplayName("Deve retornar nome correto para cada role")
    void shouldReturnCorrectNameForEachRole() {
        // Given & When & Then
        assertThat(UserRole.USUARIO.name()).isEqualTo("USUARIO");
        assertThat(UserRole.ASSOCIADO.name()).isEqualTo("ASSOCIADO");
        assertThat(UserRole.COLABORADOR.name()).isEqualTo("COLABORADOR");
        assertThat(UserRole.DIRETOR.name()).isEqualTo("DIRETOR");
        assertThat(UserRole.FUNDADOR.name()).isEqualTo("FUNDADOR");
        assertThat(UserRole.ADMINISTRADOR.name()).isEqualTo("ADMINISTRADOR");
    }

    @Test
    @DisplayName("Deve permitir conversão de string para enum")
    void shouldAllowStringToEnumConversion() {
        // Given & When & Then
        assertThat(UserRole.valueOf("USUARIO")).isEqualTo(UserRole.USUARIO);
        assertThat(UserRole.valueOf("ASSOCIADO")).isEqualTo(UserRole.ASSOCIADO);
        assertThat(UserRole.valueOf("COLABORADOR")).isEqualTo(UserRole.COLABORADOR);
        assertThat(UserRole.valueOf("DIRETOR")).isEqualTo(UserRole.DIRETOR);
        assertThat(UserRole.valueOf("FUNDADOR")).isEqualTo(UserRole.FUNDADOR);
        assertThat(UserRole.valueOf("ADMINISTRADOR")).isEqualTo(UserRole.ADMINISTRADOR);
    }
}