package com.sistema.debug;

import com.sistema.entity.User;
import com.sistema.repository.UserRepository;
import com.sistema.service.AuthService;
import com.sistema.util.CpfGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("User Persistence Debug Tests")
class UserPersistenceDebugTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private static String testEmail;
    private static String testCpf;

    @Test
    @Order(1)
    @DisplayName("1. Deve registrar usuário e verificar persistência")
    void shouldRegisterUserAndVerifyPersistence() {
        // Given
        testEmail = "persistence.test." + System.currentTimeMillis() + "@example.com";
        testCpf = CpfGenerator.generateCpf();
        String password = "MinhaSenh@123";

        System.out.println("=== TESTE 1: REGISTRANDO USUÁRIO ===");
        System.out.println("Email: " + testEmail);
        System.out.println("CPF: " + testCpf);

        // When
        User createdUser = authService.register(testEmail, password, "João", "Silva", testCpf);

        // Then
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getEmail()).isEqualTo(testEmail);
        assertThat(createdUser.getCpf()).isEqualTo(testCpf);

        // Verificar se está no banco
        Optional<User> foundUser = userRepository.findByEmail(testEmail);
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(testEmail);

        System.out.println("Usuário criado com ID: " + createdUser.getId());
        System.out.println("Usuário encontrado no banco: " + foundUser.isPresent());
    }

    @Test
    @Order(2)
    @DisplayName("2. Deve encontrar usuário criado no teste anterior")
    void shouldFindUserFromPreviousTest() {
        System.out.println("=== TESTE 2: BUSCANDO USUÁRIO ===");
        System.out.println("Procurando por email: " + testEmail);

        // When
        Optional<User> foundUser = userRepository.findByEmail(testEmail);

        // Then
        System.out.println("Usuário encontrado: " + foundUser.isPresent());
        if (foundUser.isPresent()) {
            System.out.println("ID: " + foundUser.get().getId());
            System.out.println("Email: " + foundUser.get().getEmail());
            System.out.println("CPF: " + foundUser.get().getCpf());
        }

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(testEmail);
        assertThat(foundUser.get().getCpf()).isEqualTo(testCpf);
    }

    @Test
    @Order(3)
    @DisplayName("3. Deve listar todos os usuários")
    void shouldListAllUsers() {
        System.out.println("=== TESTE 3: LISTANDO TODOS OS USUÁRIOS ===");

        // When
        var allUsers = userRepository.findAll();

        // Then
        System.out.println("Total de usuários no banco: " + allUsers.size());
        allUsers.forEach(user -> {
            System.out.println("- ID: " + user.getId() + ", Email: " + user.getEmail() + ", CPF: " + user.getCpf());
        });

        assertThat(allUsers).isNotEmpty();
    }
}