package com.sistema.service;

import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("João");
        testUser.setLastName("Silva");
        testUser.setEmail("joao@email.com");
        testUser.setUsername("joao");
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.USER);
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve criar primeiro usuário como admin")
    void shouldCreateFirstUserAsAdmin() {
        // Given
        when(userRepository.existsByEmail("admin@email.com")).thenReturn(false);
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.isFirstUser()).thenReturn(true);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        
        User savedUser = new User();
        savedUser.setRole(UserRole.ADMIN);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        User result = userService.createUser("Admin", "User", "admin@email.com", "admin", "password123");

        // Then
        assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository).isFirstUser();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Deve criar usuário regular quando não for o primeiro")
    void shouldCreateRegularUserWhenNotFirst() {
        // Given
        when(userRepository.existsByEmail("user@email.com")).thenReturn(false);
        when(userRepository.existsByUsername("user")).thenReturn(false);
        when(userRepository.isFirstUser()).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        
        User savedUser = new User();
        savedUser.setRole(UserRole.USER);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        User result = userService.createUser("Regular", "User", "user@email.com", "user", "password123");

        // Then
        assertThat(result.getRole()).isEqualTo(UserRole.USER);
        verify(userRepository).isFirstUser();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando email já existe")
    void shouldThrowExceptionWhenEmailExists() {
        // Given
        when(userRepository.existsByEmail("existing@email.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> 
            userService.createUser("Test", "User", "existing@email.com", "testuser", "password123")
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Email já está em uso");
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando username já existe")
    void shouldThrowExceptionWhenUsernameExists() {
        // Given
        when(userRepository.existsByEmail("test@email.com")).thenReturn(false);
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> 
            userService.createUser("Test", "User", "test@email.com", "existinguser", "password123")
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Username já está em uso");
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve buscar usuário por username")
    void shouldFindUserByUsername() {
        // Given
        when(userRepository.findByUsername("joao")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByUsername("joao");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("joao");
        verify(userRepository).findByUsername("joao");
    }

    @Test
    @DisplayName("Deve buscar usuário por email")
    void shouldFindUserByEmail() {
        // Given
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByEmail("joao@email.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("joao@email.com");
        verify(userRepository).findByEmail("joao@email.com");
    }

    @Test
    @DisplayName("Deve buscar usuário por username ou email")
    void shouldFindUserByUsernameOrEmail() {
        // Given
        when(userRepository.findByUsernameOrEmail("joao", "joao")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByUsernameOrEmail("joao");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("joao");
        verify(userRepository).findByUsernameOrEmail("joao", "joao");
    }

    @Test
    @DisplayName("Deve listar usuários ativos")
    void shouldFindActiveUsers() {
        // Given
        List<User> activeUsers = Arrays.asList(testUser);
        when(userRepository.findByActiveTrue()).thenReturn(activeUsers);

        // When
        List<User> result = userService.findActiveUsers();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isActive()).isTrue();
        verify(userRepository).findByActiveTrue();
    }

    @Test
    @DisplayName("Deve listar usuários por role")
    void shouldFindUsersByRole() {
        // Given
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findByRole(UserRole.USER)).thenReturn(users);

        // When
        List<User> result = userService.findUsersByRole(UserRole.USER);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(UserRole.USER);
        verify(userRepository).findByRole(UserRole.USER);
    }

    @Test
    @DisplayName("Deve atualizar status do usuário")
    void shouldUpdateUserStatus() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUserStatus(1L, false);

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar status de usuário inexistente")
    void shouldThrowExceptionWhenUpdatingNonExistentUserStatus() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUserStatus(999L, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Usuário não encontrado");
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve atualizar role do usuário")
    void shouldUpdateUserRole() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUserRole(1L, UserRole.ADMIN);

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Deve atualizar último login")
    void shouldUpdateLastLogin() {
        // Given
        when(userRepository.findByUsername("joao")).thenReturn(Optional.of(testUser));
        
        // When
        userService.updateLastLogin("joao");

        // Then
        verify(userRepository).findByUsername("joao");
        verify(userRepository).updateLastLogin(eq(1L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Deve verificar se é primeiro usuário")
    void shouldCheckIfFirstUser() {
        // Given
        when(userRepository.isFirstUser()).thenReturn(true);

        // When
        boolean result = userService.isFirstUser();

        // Then
        assertThat(result).isTrue();
        verify(userRepository).isFirstUser();
    }

    @Test
    @DisplayName("Deve obter estatísticas dos usuários")
    void shouldGetUserStatistics() {
        // Given
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByActiveTrue()).thenReturn(8L);
        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(2L);
        when(userRepository.countByRole(UserRole.USER)).thenReturn(8L);

        // When
        UserService.UserStatistics stats = userService.getUserStatistics();

        // Then
        assertThat(stats.getTotalUsers()).isEqualTo(10L);
        assertThat(stats.getActiveUsers()).isEqualTo(8L);
        assertThat(stats.getAdminUsers()).isEqualTo(2L);
        assertThat(stats.getRegularUsers()).isEqualTo(8L);
        assertThat(stats.getInactiveUsers()).isEqualTo(2L);
        assertThat(stats.getActiveUserPercentage()).isEqualTo(80.0);
    }
}