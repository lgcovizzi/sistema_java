package com.sistema.repository;

import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setFirstName("João");
        user1.setLastName("Silva");
        user1.setEmail("joao.silva@example.com");
        user1.setUsername("joao.silva");
        user1.setPassword("senha123");
        user1.setRole(UserRole.USER);
        user1.setActive(true);
        user1.setCreatedAt(LocalDateTime.now().minusDays(2));
        
        user2 = new User();
        user2.setFirstName("Maria");
        user2.setLastName("Santos");
        user2.setEmail("maria.santos@example.com");
        user2.setUsername("maria.santos");
        user2.setPassword("senha456");
        user2.setRole(UserRole.ADMIN);
        user2.setActive(false);
        user2.setCreatedAt(LocalDateTime.now().minusDays(1));
        
        user3 = new User();
        user3.setFirstName("Pedro");
        user3.setLastName("Oliveira");
        user3.setEmail("pedro.oliveira@example.com");
        user3.setUsername("pedro.oliveira");
        user3.setPassword("senha789");
        user3.setRole(UserRole.USER);
        user3.setActive(true);
        user3.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve encontrar usuário por username")
    void shouldFindUserByUsername() {
        // Given
        entityManager.persistAndFlush(user1);
        
        // When
        Optional<User> found = userRepository.findByUsername("joao.silva");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("João");
        assertThat(found.get().getUsername()).isEqualTo("joao.silva");
    }

    @Test
    @DisplayName("Deve encontrar usuário por email")
    void shouldFindUserByEmail() {
        // Given
        entityManager.persistAndFlush(user1);
        
        // When
        Optional<User> found = userRepository.findByEmail("joao.silva@example.com");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("João");
        assertThat(found.get().getEmail()).isEqualTo("joao.silva@example.com");
    }

    @Test
    @DisplayName("Deve verificar se username existe")
    void shouldCheckIfUsernameExists() {
        // Given
        entityManager.persistAndFlush(user1);
        
        // When
        boolean exists = userRepository.existsByUsername("joao.silva");
        boolean notExists = userRepository.existsByUsername("inexistente");
        
        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Deve verificar se email existe")
    void shouldCheckIfEmailExists() {
        // Given
        entityManager.persistAndFlush(user1);
        
        // When
        boolean exists = userRepository.existsByEmail("joao.silva@example.com");
        boolean notExists = userRepository.existsByEmail("inexistente@example.com");
        
        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Deve encontrar usuários por role")
    void shouldFindUsersByRole() {
        // Given
        entityManager.persistAndFlush(user1); // USER
        entityManager.persistAndFlush(user2); // ADMIN
        entityManager.persistAndFlush(user3); // USER
        
        // When
        List<User> users = userRepository.findByRole(UserRole.USER);
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);
        
        // Then
        assertThat(users).hasSize(2);
        assertThat(admins).hasSize(1);
        assertThat(users.get(0).getRole()).isEqualTo(UserRole.USER);
        assertThat(admins.get(0).getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("Deve contar usuários por role")
    void shouldCountUsersByRole() {
        // Given
        entityManager.persistAndFlush(user1); // USER
        entityManager.persistAndFlush(user2); // ADMIN
        entityManager.persistAndFlush(user3); // USER
        
        // When
        long userCount = userRepository.countByRole(UserRole.USER);
        long adminCount = userRepository.countByRole(UserRole.ADMIN);
        
        // Then
        assertThat(userCount).isEqualTo(2);
        assertThat(adminCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve encontrar usuários ativos")
    void shouldFindActiveUsers() {
        // Given
        entityManager.persistAndFlush(user1); // ativo
        entityManager.persistAndFlush(user2); // inativo
        entityManager.persistAndFlush(user3); // ativo
        
        // When
        List<User> activeUsers = userRepository.findByActiveTrue();
        List<User> inactiveUsers = userRepository.findByActiveFalse();
        
        // Then
        assertThat(activeUsers).hasSize(2);
        assertThat(inactiveUsers).hasSize(1);
    }

    @Test
    @DisplayName("Deve contar usuários ativos")
    void shouldCountActiveUsers() {
        // Given
        entityManager.persistAndFlush(user1); // ativo
        entityManager.persistAndFlush(user2); // inativo
        entityManager.persistAndFlush(user3); // ativo
        
        // When
        long activeCount = userRepository.countByActiveTrue();
        
        // Then
        assertThat(activeCount).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve verificar se é o primeiro usuário")
    void shouldCheckIfFirstUser() {
        // When - sem usuários
        boolean isFirstWhenEmpty = userRepository.isFirstUser();
        
        // Then
        assertThat(isFirstWhenEmpty).isTrue();
        
        // Given - com usuários
        entityManager.persistAndFlush(user1);
        
        // When - com usuários
        boolean isFirstWhenNotEmpty = userRepository.isFirstUser();
        
        // Then
        assertThat(isFirstWhenNotEmpty).isFalse();
    }

    @Test
    @DisplayName("Deve encontrar primeiro usuário criado")
    void shouldFindFirstUserCreated() {
        // Given
        entityManager.persistAndFlush(user3); // mais recente
        entityManager.persistAndFlush(user2); // meio
        entityManager.persistAndFlush(user1); // mais antigo
        
        // When
        Optional<User> firstUser = userRepository.findFirstByOrderByCreatedAtAsc();
        
        // Then
        assertThat(firstUser).isPresent();
        assertThat(firstUser.get().getUsername()).isEqualTo("joao.silva");
    }

    @Test
    @DisplayName("Deve buscar usuários por texto")
    void shouldSearchUsers() {
        // Given
        entityManager.persistAndFlush(user1); // João Silva
        entityManager.persistAndFlush(user2); // Maria Santos
        entityManager.persistAndFlush(user3); // Pedro Oliveira
        
        // When
        List<User> foundByFirstName = userRepository.searchUsers("João");
        List<User> foundByEmail = userRepository.searchUsers("maria.santos");
        List<User> foundByUsername = userRepository.searchUsers("pedro");
        
        // Then
        assertThat(foundByFirstName).hasSize(1);
        assertThat(foundByFirstName.get(0).getFirstName()).isEqualTo("João");
        
        assertThat(foundByEmail).hasSize(1);
        assertThat(foundByEmail.get(0).getEmail()).isEqualTo("maria.santos@example.com");
        
        assertThat(foundByUsername).hasSize(1);
        assertThat(foundByUsername.get(0).getUsername()).isEqualTo("pedro.oliveira");
    }
}