package com.sistema.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import com.sistema.service.EmailService;
import com.sistema.service.EmailVerificationService;
import com.sistema.util.CpfGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para o fluxo completo de ativação por email.
 * Testa desde o cadastro até o login, passando pela verificação de email.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Testes de Integração - Fluxo Completo de Ativação por Email")
class EmailActivationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @MockBean
    private EmailService emailService;

    private Map<String, Object> validUserRegistration;
    private final String testEmail = "integration.test@example.com";
    private final String testPassword = "Password123!";

    @BeforeEach
    void setUp() {
        // Limpar dados de teste
        userRepository.deleteAll();

        // Dados válidos para registro
        validUserRegistration = new HashMap<>();
        validUserRegistration.put("firstName", "Integration");
        validUserRegistration.put("lastName", "Test");
        validUserRegistration.put("email", testEmail);
        validUserRegistration.put("cpf", CpfGenerator.generateCpf());
        validUserRegistration.put("password", testPassword);

        // Mock do serviço de email para não enviar emails reais
        doNothing().when(emailService).sendVerificationEmail(any(User.class), anyString());
    }

    @Test
    @DisplayName("Fluxo completo: Cadastro → Email não verificado → Verificação → Login bem-sucedido")
    void shouldCompleteFullActivationFlow() throws Exception {
        // 1. CADASTRO - Deve criar usuário com email não verificado
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRegistration)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Usuário registrado com sucesso. Verifique seu email para ativar a conta."))
                .andExpect(jsonPath("$.user.email").value(testEmail))
                .andExpect(jsonPath("$.user.emailVerified").value(false))
                .andReturn();

        // Verificar que o email de verificação foi enviado
        verify(emailService, times(1)).sendVerificationEmail(any(User.class), anyString());

        // Verificar que o usuário foi criado no banco
        Optional<User> userOpt = userRepository.findByEmail(testEmail);
        assertThat(userOpt).isPresent();
        User user = userOpt.get();
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getVerificationToken()).isNotNull();
        assertThat(user.getVerificationTokenExpiresAt()).isNotNull();

        // 2. TENTATIVA DE LOGIN - Deve falhar por email não verificado
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", testEmail);
        loginRequest.put("password", testPassword);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email não verificado. Verifique sua caixa de entrada e clique no link de verificação."));

        // 3. VERIFICAÇÃO DE EMAIL - Deve ativar a conta
        String verificationToken = user.getVerificationToken();
        
        Map<String, String> verifyRequest = new HashMap<>();
        verifyRequest.put("token", verificationToken);

        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verificado com sucesso!"));

        // Verificar que o usuário foi ativado no banco
        user = userRepository.findByEmail(testEmail).orElseThrow();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getVerificationToken()).isNull();
        assertThat(user.getVerificationTokenExpiresAt()).isNull();

        // 4. LOGIN APÓS VERIFICAÇÃO - Deve ser bem-sucedido
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(testEmail))
                .andExpect(jsonPath("$.user.emailVerified").value(true));
    }

    @Test
    @DisplayName("Deve permitir reenvio de email de verificação para usuário não verificado")
    void shouldAllowResendVerificationEmail() throws Exception {
        // 1. Cadastrar usuário
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRegistration)))
                .andExpect(status().isCreated());

        // Verificar primeiro envio
        verify(emailService, times(1)).sendVerificationEmail(any(User.class), anyString());

        // 2. Reenviar email de verificação
        Map<String, String> resendRequest = new HashMap<>();
        resendRequest.put("email", testEmail);

        mockMvc.perform(post("/api/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resendRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email de verificação reenviado com sucesso."));

        // Verificar que o email foi enviado novamente
        verify(emailService, times(2)).sendVerificationEmail(any(User.class), anyString());

        // Verificar que um novo token foi gerado
        User user = userRepository.findByEmail(testEmail).orElseThrow();
        assertThat(user.getVerificationToken()).isNotNull();
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar verificação com token inválido")
    void shouldRejectVerificationWithInvalidToken() throws Exception {
        // 1. Cadastrar usuário
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRegistration)))
                .andExpect(status().isCreated());

        // 2. Tentar verificar com token inválido
        Map<String, String> verifyRequest = new HashMap<>();
        verifyRequest.put("token", "token-invalido-123");

        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token de verificação inválido ou expirado."));

        // Verificar que o usuário continua não verificado
        User user = userRepository.findByEmail(testEmail).orElseThrow();
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar verificação com token expirado")
    void shouldRejectVerificationWithExpiredToken() throws Exception {
        // 1. Cadastrar usuário
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRegistration)))
                .andExpect(status().isCreated());

        // 2. Simular expiração do token
        User user = userRepository.findByEmail(testEmail).orElseThrow();
        String originalToken = user.getVerificationToken();
        
        // Forçar expiração do token (definir data no passado)
        user.setVerificationTokenExpiresAt(java.time.LocalDateTime.now().minusHours(1));
        userRepository.save(user);

        // 3. Tentar verificar com token expirado
        Map<String, String> verifyRequest = new HashMap<>();
        verifyRequest.put("token", originalToken);

        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token de verificação inválido ou expirado."));

        // Verificar que o usuário continua não verificado
        user = userRepository.findByEmail(testEmail).orElseThrow();
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("Deve permitir login imediato para usuário admin (sem verificação de email)")
    void shouldAllowImmediateLoginForAdminUser() throws Exception {
        // 1. Criar usuário admin diretamente no banco (simula criação por outro admin)
        User adminUser = new User();
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setEmail("admin@example.com");
        adminUser.setCpf(CpfGenerator.generateCpf());
        adminUser.setPassword("$2a$10$" + "encodedPassword"); // Senha codificada
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setEnabled(true);
        adminUser.setEmailVerified(true); // Admin é automaticamente verificado
        adminUser = userRepository.save(adminUser);

        // 2. Login deve ser bem-sucedido imediatamente
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "admin@example.com");
        loginRequest.put("password", "password123"); // Senha original

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.emailVerified").value(true));
    }

    @Test
    @DisplayName("Deve impedir múltiplas verificações com o mesmo token")
    void shouldPreventMultipleVerificationsWithSameToken() throws Exception {
        // 1. Cadastrar usuário
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRegistration)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail(testEmail).orElseThrow();
        String verificationToken = user.getVerificationToken();

        // 2. Primeira verificação - deve ser bem-sucedida
        Map<String, String> verifyRequest = new HashMap<>();
        verifyRequest.put("token", verificationToken);

        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verificado com sucesso!"));

        // 3. Segunda verificação com o mesmo token - deve falhar
        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token de verificação inválido ou expirado."));
    }

    @Test
    @DisplayName("Deve manter estado de verificação após logout e novo login")
    void shouldMaintainVerificationStateAfterLogoutAndRelogin() throws Exception {
        // 1. Fluxo completo de cadastro e verificação
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRegistration)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail(testEmail).orElseThrow();
        String verificationToken = user.getVerificationToken();

        Map<String, String> verifyRequest = new HashMap<>();
        verifyRequest.put("token", verificationToken);

        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk());

        // 2. Primeiro login após verificação
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", testEmail);
        loginRequest.put("password", testPassword);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Extrair token para logout
        String responseBody = loginResult.getResponse().getContentAsString();
        Map<String, Object> loginResponse = objectMapper.readValue(responseBody, Map.class);
        String accessToken = (String) loginResponse.get("accessToken");

        // 3. Logout
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 4. Novo login - deve continuar funcionando (email já verificado)
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.emailVerified").value(true));
    }

    @Test
    @DisplayName("Deve impedir reenvio de verificação para usuário já verificado")
    void shouldPreventResendVerificationForAlreadyVerifiedUser() throws Exception {
        // 1. Fluxo completo de verificação
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRegistration)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail(testEmail).orElseThrow();
        String verificationToken = user.getVerificationToken();

        Map<String, String> verifyRequest = new HashMap<>();
        verifyRequest.put("token", verificationToken);

        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk());

        // 2. Tentar reenviar verificação para usuário já verificado
        Map<String, String> resendRequest = new HashMap<>();
        resendRequest.put("email", testEmail);

        mockMvc.perform(post("/api/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resendRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email já foi verificado."));

        // Verificar que não houve envio adicional de email
        verify(emailService, times(1)).sendVerificationEmail(any(User.class), anyString());
    }

    @Test
    @DisplayName("Deve validar que cadastro gera token de verificação válido")
    void shouldValidateRegistrationGeneratesValidVerificationToken() throws Exception {
        // 1. Cadastrar usuário
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRegistration)))
                .andExpect(status().isCreated());

        // 2. Verificar propriedades do token gerado
        User user = userRepository.findByEmail(testEmail).orElseThrow();
        
        assertThat(user.getVerificationToken()).isNotNull();
        assertThat(user.getVerificationToken()).hasSize(64); // Token SHA-256
        assertThat(user.getVerificationTokenExpiresAt()).isNotNull();
        assertThat(user.getVerificationTokenExpiresAt()).isAfter(java.time.LocalDateTime.now());
        assertThat(user.getVerificationTokenExpiresAt()).isBefore(java.time.LocalDateTime.now().plusHours(25));
        assertThat(user.isEmailVerified()).isFalse();
    }
}