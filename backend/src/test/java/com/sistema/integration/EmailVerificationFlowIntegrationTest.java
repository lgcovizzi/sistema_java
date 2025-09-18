package com.sistema.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.controller.AuthController.LoginRequest;
import com.sistema.controller.AuthController.RegisterRequest;
import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import com.sistema.service.AuthService;
import com.sistema.service.EmailVerificationService;
import com.sistema.service.EmailService;
import com.sistema.util.CpfGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de integração específico para o fluxo detalhado de verificação de email.
 * 
 * Testa cenários específicos como:
 * - Tokens expirados
 * - Tokens inválidos
 * - Reenvio de verificação
 * - Múltiplas tentativas de verificação
 * - Validação de tempo de expiração
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Teste de Integração - Fluxo Detalhado de Verificação de Email")
class EmailVerificationFlowIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @SpyBean
    private EmailService emailService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private static final String TEST_EMAIL = "verification.test@example.com";
    private static final String TEST_PASSWORD = "TestPassword123!";
    private static final String TEST_CPF = CpfGenerator.generateCpf();
    private static final String TEST_FIRST_NAME = "Teste";
    private static final String TEST_LAST_NAME = "Verificação";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        // Limpar dados de teste anteriores
        userRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("1. Deve gerar token de verificação com expiração correta (24 horas)")
    void shouldGenerateVerificationTokenWithCorrectExpiration() throws Exception {
        // Cadastrar usuário
        registerTestUser();

        Optional<User> userOpt = userRepository.findByEmail(TEST_EMAIL);
        assertThat(userOpt).isPresent();

        User user = userOpt.get();
        assertThat(user.getVerificationToken()).isNotNull();
        assertThat(user.getVerificationTokenExpiresAt()).isNotNull();

        // Verificar que a expiração está aproximadamente 24 horas no futuro
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiration = user.getVerificationTokenExpiresAt();
        
        // Deve estar entre 23h50min e 24h10min no futuro (margem para processamento)
        assertThat(expiration).isAfter(now.plusHours(23).plusMinutes(50));
        assertThat(expiration).isBefore(now.plusHours(24).plusMinutes(10));
    }

    @Test
    @Order(2)
    @DisplayName("2. Deve falhar verificação com token inválido")
    void shouldFailVerificationWithInvalidToken() {
        String invalidToken = "token-invalido-123";
        
        boolean result = emailVerificationService.verifyEmailToken(invalidToken);
        
        assertThat(result).isFalse();
    }

    @Test
    @Order(3)
    @DisplayName("3. Deve falhar verificação com token expirado")
    void shouldFailVerificationWithExpiredToken() throws Exception {
        // Cadastrar usuário
        registerTestUser();

        Optional<User> userOpt = userRepository.findByEmail(TEST_EMAIL);
        assertThat(userOpt).isPresent();

        User user = userOpt.get();
        String token = user.getVerificationToken();

        // Simular token expirado alterando a data de expiração
        user.setVerificationTokenExpiresAt(LocalDateTime.now().minusHours(1));
        userRepository.save(user);

        boolean result = emailVerificationService.verifyEmailToken(token);
        
        assertThat(result).isFalse();
    }

    @Test
    @Order(4)
    @DisplayName("4. Deve permitir reenvio de email de verificação")
    void shouldAllowResendingVerificationEmail() throws Exception {
        // Cadastrar usuário
        registerTestUser();

        // Reenviar email de verificação
        mockMvc.perform(post("/api/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + TEST_EMAIL + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email de verificação reenviado com sucesso"));

        // Verificar que o email foi enviado novamente
        verify(emailService, times(2)).sendVerificationEmail(any(User.class), any(String.class));
    }

    @Test
    @Order(5)
    @DisplayName("5. Deve gerar novo token ao reenviar verificação")
    void shouldGenerateNewTokenOnResend() throws Exception {
        // Cadastrar usuário
        registerTestUser();

        Optional<User> userOpt = userRepository.findByEmail(TEST_EMAIL);
        assertThat(userOpt).isPresent();
        String originalToken = userOpt.get().getVerificationToken();

        // Reenviar email de verificação
        mockMvc.perform(post("/api/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + TEST_EMAIL + "\"}"))
                .andExpect(status().isOk());

        // Verificar que um novo token foi gerado
        userOpt = userRepository.findByEmail(TEST_EMAIL);
        assertThat(userOpt).isPresent();
        String newToken = userOpt.get().getVerificationToken();

        assertThat(newToken).isNotNull();
        assertThat(newToken).isNotEqualTo(originalToken);
    }

    @Test
    @Order(6)
    @DisplayName("6. Deve invalidar token antigo após verificação bem-sucedida")
    void shouldInvalidateTokenAfterSuccessfulVerification() throws Exception {
        // Cadastrar usuário
        registerTestUser();

        Optional<User> userOpt = userRepository.findByEmail(TEST_EMAIL);
        assertThat(userOpt).isPresent();
        String token = userOpt.get().getVerificationToken();

        // Verificar o token
        boolean result = emailVerificationService.verifyEmailToken(token);
        assertThat(result).isTrue();

        // Verificar que o token foi limpo
        userOpt = userRepository.findByEmail(TEST_EMAIL);
        assertThat(userOpt).isPresent();
        User user = userOpt.get();
        assertThat(user.getVerificationToken()).isNull();
        assertThat(user.getVerificationTokenExpiresAt()).isNull();
        assertThat(user.isEmailVerified()).isTrue();

        // Tentar usar o mesmo token novamente deve falhar
        result = emailVerificationService.verifyEmailToken(token);
        assertThat(result).isFalse();
    }

    @Test
    @Order(7)
    @DisplayName("7. Deve falhar reenvio para email já verificado")
    void shouldFailResendForAlreadyVerifiedEmail() throws Exception {
        // Cadastrar e verificar usuário
        registerTestUser();
        
        Optional<User> userOpt = userRepository.findByEmail(TEST_EMAIL);
        assertThat(userOpt).isPresent();
        String token = userOpt.get().getVerificationToken();
        
        // Verificar email
        emailVerificationService.verifyEmailToken(token);

        // Tentar reenviar para email já verificado
        mockMvc.perform(post("/api/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + TEST_EMAIL + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email já verificado"));
    }

    @Test
    @Order(8)
    @DisplayName("8. Deve falhar reenvio para email inexistente")
    void shouldFailResendForNonExistentEmail() throws Exception {
        mockMvc.perform(post("/api/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"inexistente@example.com\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Usuário não encontrado"));
    }

    @Test
    @Order(9)
    @DisplayName("9. Deve verificar que usuário ADMIN não precisa verificar email")
    void shouldVerifyAdminDoesNotNeedEmailVerification() throws Exception {
        // Garantir que não há usuários (primeiro usuário será admin)
        userRepository.deleteAll();

        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Admin");
        request.setLastName("Sistema");
        request.setEmail("admin@test.com");
        request.setCpf(CpfGenerator.generateCpf());
        request.setPassword("AdminPassword123!");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        Optional<User> userOpt = userRepository.findByEmail("admin@test.com");
        assertThat(userOpt).isPresent();

        User admin = userOpt.get();
        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.isEmailVerified()).isTrue();
        
        // Verificar que admin não precisa verificar email
        boolean needsVerification = emailVerificationService.needsEmailVerification(admin);
        assertThat(needsVerification).isFalse();
    }

    @Test
    @Order(10)
    @DisplayName("10. Deve verificar que usuário comum sempre precisa verificar email")
    void shouldVerifyCommonUserAlwaysNeedsEmailVerification() throws Exception {
        // Cadastrar usuário comum
        registerTestUser();

        Optional<User> userOpt = userRepository.findByEmail(TEST_EMAIL);
        assertThat(userOpt).isPresent();

        User user = userOpt.get();
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.isEmailVerified()).isFalse();
        
        // Verificar que usuário comum precisa verificar email
        boolean needsVerification = emailVerificationService.needsEmailVerification(user);
        assertThat(needsVerification).isTrue();
    }

    @Test
    @Order(11)
    @DisplayName("11. Deve validar fluxo completo: cadastro → verificação → login")
    void shouldValidateCompleteFlow() throws Exception {
        // 1. Cadastrar usuário
        registerTestUser();

        // 2. Verificar que login falha antes da verificação
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(TEST_EMAIL);
        loginRequest.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email não verificado. Verifique sua caixa de entrada e clique no link de verificação."));

        // 3. Verificar email
        Optional<User> userOpt = userRepository.findByEmail(TEST_EMAIL);
        assertThat(userOpt).isPresent();
        String token = userOpt.get().getVerificationToken();
        
        boolean result = emailVerificationService.verifyEmailToken(token);
        assertThat(result).isTrue();

        // 4. Verificar que login agora funciona
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    @Order(12)
    @DisplayName("12. Deve verificar estatísticas de verificação de email")
    void shouldVerifyEmailVerificationStatistics() throws Exception {
        // Cadastrar múltiplos usuários
        for (int i = 1; i <= 3; i++) {
            RegisterRequest request = new RegisterRequest();
            request.setFirstName("User");
            request.setLastName("Test " + i);
            request.setEmail("user" + i + "@test.com");
            request.setCpf(CpfGenerator.generateCpf());
            request.setPassword("TestPassword123!");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Verificar estatísticas
        long totalUsers = userRepository.count();
        long verifiedUsers = userRepository.findAll().stream()
                .mapToLong(user -> user.isEmailVerified() ? 1 : 0)
                .sum();
        long unverifiedUsers = totalUsers - verifiedUsers;

        assertThat(totalUsers).isEqualTo(3);
        assertThat(unverifiedUsers).isEqualTo(3); // Todos os usuários comuns não verificados
        assertThat(verifiedUsers).isEqualTo(0); // Nenhum verificado ainda

        // Verificar um usuário
        Optional<User> userOpt = userRepository.findByEmail("user1@test.com");
        assertThat(userOpt).isPresent();
        String token = userOpt.get().getVerificationToken();
        emailVerificationService.verifyEmailToken(token);

        // Verificar estatísticas atualizadas
        verifiedUsers = userRepository.findAll().stream()
                .mapToLong(user -> user.isEmailVerified() ? 1 : 0)
                .sum();
        unverifiedUsers = totalUsers - verifiedUsers;

        assertThat(verifiedUsers).isEqualTo(1);
        assertThat(unverifiedUsers).isEqualTo(2);
    }

    private void registerTestUser() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName(TEST_FIRST_NAME);
        request.setLastName(TEST_LAST_NAME);
        request.setEmail(TEST_EMAIL);
        request.setCpf(TEST_CPF);
        request.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}