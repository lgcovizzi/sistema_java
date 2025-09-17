package com.sistema.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

@DisplayName("User Entity Tests")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    @DisplayName("Deve criar usuário com campos básicos")
    void shouldCreateUserWithBasicFields() {
        // Given
        String firstName = "João";
        String lastName = "Silva";
        String email = "joao@email.com";
        String password = "senha123";

        // When
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(password);

        // Then
        assertThat(user.getFirstName()).isEqualTo(firstName);
        assertThat(user.getLastName()).isEqualTo(lastName);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getPassword()).isEqualTo(password);
    }

    @Test
    @DisplayName("Deve ter role USER por padrão")
    void shouldHaveUserRoleByDefault() {
        // When
        User newUser = new User();

        // Then
        assertThat(newUser.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("Deve permitir definir role como ADMIN")
    void shouldAllowSettingAdminRole() {
        // When
        user.setRole(UserRole.ADMIN);

        // Then
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("Deve verificar se usuário é administrador")
    void shouldCheckIfUserIsAdmin() {
        // Given
        user.setRole(UserRole.ADMIN);

        // When
        boolean isAdmin = user.isAdmin();

        // Then
        assertThat(isAdmin).isTrue();
    }

    @Test
    @DisplayName("Deve verificar se usuário não é administrador")
    void shouldCheckIfUserIsNotAdmin() {
        // Given
        user.setRole(UserRole.USER);

        // When
        boolean isAdmin = user.isAdmin();

        // Then
        assertThat(isAdmin).isFalse();
    }

    @Test
    @DisplayName("Deve ter usuário ativo por padrão")
    void shouldBeActiveByDefault() {
        // When
        User newUser = new User();

        // Then
        assertThat(newUser.isActive()).isTrue();
    }

    @Test
    @DisplayName("Deve permitir desativar usuário")
    void shouldAllowDeactivatingUser() {
        // When
        user.setActive(false);

        // Then
        assertThat(user.isActive()).isFalse();
    }

    @Test
    @DisplayName("Deve definir timestamps automaticamente")
    void shouldSetTimestampsAutomatically() {
        // Given
        LocalDateTime before = LocalDateTime.now();

        // When
        user.prePersist();
        LocalDateTime after = LocalDateTime.now();

        // Then
        assertThat(user.getCreatedAt()).isBetween(before, after);
        assertThat(user.getUpdatedAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("Deve atualizar timestamp de modificação")
    void shouldUpdateModificationTimestamp() {
        // Given
        user.prePersist();
        LocalDateTime originalUpdatedAt = user.getUpdatedAt();
        
        try {
            Thread.sleep(10); // Pequena pausa para garantir diferença no timestamp
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        user.preUpdate();

        // Then
        assertThat(user.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(user.getCreatedAt()).isEqualTo(user.getCreatedAt()); // Não deve mudar
    }

    @Test
    @DisplayName("Deve retornar nome completo")
    void shouldReturnFullName() {
        // Given
        user.setFirstName("João");
        user.setLastName("Silva");

        // When
        String fullName = user.getFullName();

        // Then
        assertThat(fullName).isEqualTo("João Silva");
    }

    @Test
    @DisplayName("Deve implementar equals e hashCode corretamente")
    void shouldImplementEqualsAndHashCodeCorrectly() {
        // Given
        User user1 = new User();
        user1.setId(1L);
        user1.setEmail("test@email.com");

        User user2 = new User();
        user2.setId(2L);
        user2.setEmail("other@email.com");

        // Then
        assertThat(user1.getId()).isEqualTo(1L);
        assertThat(user1.getUsername()).isEqualTo("test@email.com");
        assertThat(user1).isNotEqualTo(user2);
        assertThat(user1.hashCode()).isNotZero();
    }

    @Test
    @DisplayName("Deve implementar toString corretamente")
    void shouldImplementToStringCorrectly() {
        // Given
        user.setId(1L);
        user.setEmail("test@email.com");
        user.setRole(UserRole.USER);

        // When
        String toString = user.toString();

        // Then
        assertThat(toString)
            .contains("User")
            .contains("id=1")
            .contains("username=test@email.com")
            .contains("email=test@email.com")
            .contains("role=USER");
    }
}