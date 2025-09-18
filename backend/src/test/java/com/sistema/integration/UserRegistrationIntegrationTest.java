package com.sistema.integration;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.controller.AuthController.RegisterRequest;
import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import com.sistema.service.EmailService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração para o fluxo completo de cadastro de usuário.
 * 
 * Este teste verifica:
 * - Cadastro de usuário comum
 * - Envio de email de verificação
 * - Ativação da conta via token
 * - Cadastro de administrador (ativação automática)
 * - Validação de dados duplicados
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("Teste de Integração - Cadastro de Usuário")
class UserRegistrationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @SpyBean
    private EmailService emailService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve cadastrar usuário comum e enviar email de verificação")
    void testUserRegistrationFlow() throws Exception {
        // 1. Dados do usuário para cadastro
        String testEmail = "joao@example.com";
        String testCpf = "12345678901";
        
        RegisterRequest userRegistration = new RegisterRequest();
        userRegistration.setEmail(testEmail);
        userRegistration.setPassword("senha123");
        userRegistration.setFirstName("João");
        userRegistration.setLastName("Silva");
        userRegistration.setCpf(testCpf);

        // 1. Realizar cadastro
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRegistration)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(testEmail))
                .andExpect(jsonPath("$.user.firstName").value("João"))
                .andExpect(jsonPath("$.user.lastName").value("Silva"));

        // 2. Verificar se o usuário foi salvo no banco
        Optional<User> savedUserOpt = userRepository.findByEmail(testEmail);
        assertThat(savedUserOpt).isPresent();
        User savedUser = savedUserOpt.get();
        assertThat(savedUser.getEmail()).isEqualTo(testEmail);
        assertThat(savedUser.getFirstName()).isEqualTo("João");
        assertThat(savedUser.getLastName()).isEqualTo("Silva");
        assertThat(savedUser.getCpf()).isEqualTo(testCpf);
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);

        // 3. Verificar que o usuário não está ativado inicialmente
        assertThat(savedUser.isActive()).isFalse();

        // 4. Verificar que o email de verificação foi enviado
        verify(emailService, times(1)).sendVerificationEmail(any(User.class), any(String.class));
    }

    @Test
    @DisplayName("Deve cadastrar administrador quando for o primeiro usuário")
    void testFirstUserBecomesAdmin() throws Exception {
        // Garantir que não há usuários no banco
        userRepository.deleteAll();
        
        // Dados do primeiro usuário
        String adminEmail = "admin@example.com";
        
        RegisterRequest adminRegistration = new RegisterRequest();
        adminRegistration.setEmail(adminEmail);
        adminRegistration.setPassword("admin123");
        adminRegistration.setFirstName("Admin");
        adminRegistration.setLastName("Sistema");
        adminRegistration.setCpf("98765432100");

        // Realizar cadastro
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminRegistration)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(adminEmail))
                .andExpect(jsonPath("$.user.firstName").value("Admin"));

        // Verificar se o usuário foi salvo no banco
        Optional<User> savedUserOpt = userRepository.findByEmail(adminEmail);
        assertThat(savedUserOpt).isPresent();
        User savedUser = savedUserOpt.get();
        assertThat(savedUser.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(savedUser.isActive()).isTrue(); // Admin deve estar ativo automaticamente

        // Verificar que o email de verificação foi enviado
        verify(emailService, times(1)).sendVerificationEmail(any(User.class), any(String.class));
    }

    @Test
    @DisplayName("Deve falhar ao tentar cadastrar email duplicado")
    void testDuplicateEmailRegistration() throws Exception {
        // Primeiro cadastro
        RegisterRequest firstUser = new RegisterRequest();
        firstUser.setEmail("test@example.com");
        firstUser.setPassword("senha123");
        firstUser.setFirstName("Test");
        firstUser.setLastName("User");
        firstUser.setCpf("12345678901");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstUser)))
                .andExpect(status().isCreated());

        // Segundo cadastro com mesmo email
        RegisterRequest duplicateUser = new RegisterRequest();
        duplicateUser.setEmail("test@example.com"); // Email duplicado
        duplicateUser.setPassword("outrasenha");
        duplicateUser.setFirstName("Another");
        duplicateUser.setLastName("User");
        duplicateUser.setCpf("98765432100");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Deve falhar ao tentar cadastrar CPF duplicado")
    void testDuplicateCpfRegistration() throws Exception {
        // Primeiro cadastro
        RegisterRequest firstUser = new RegisterRequest();
        firstUser.setEmail("first@example.com");
        firstUser.setPassword("senha123");
        firstUser.setFirstName("First");
        firstUser.setLastName("User");
        firstUser.setCpf("12345678901");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstUser)))
                .andExpect(status().isCreated());

        // Segundo cadastro com mesmo CPF
        RegisterRequest duplicateUser = new RegisterRequest();
        duplicateUser.setEmail("second@example.com");
        duplicateUser.setPassword("outrasenha");
        duplicateUser.setFirstName("Second");
        duplicateUser.setLastName("User");
        duplicateUser.setCpf("12345678901"); // CPF duplicado

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        // Verificar se apenas um usuário foi salvo no banco
        assertThat(userRepository.count()).isEqualTo(1L);
        
        // Verificar que o email de verificação foi enviado apenas uma vez
        verify(emailService, times(1)).sendVerificationEmail(any(User.class), any(String.class));
    }
}