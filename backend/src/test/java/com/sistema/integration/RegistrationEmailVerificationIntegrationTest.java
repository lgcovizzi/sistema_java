package com.sistema.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.controller.AuthController.LoginRequest;
import com.sistema.controller.AuthController.RegisterRequest;
import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import com.sistema.repository.RefreshTokenRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de integração completo para verificar que TODOS os cadastros tenham email verificado.
 * 
 * Este teste garante que:
 * 1. Usuários comuns não conseguem fazer login sem verificar email
 * 2. Usuários ADMIN são automaticamente verificados
 * 3. O fluxo de verificação de email funciona corretamente
 * 4. Todos os cadastros seguem as regras de verificação de email
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Teste de Integração - Verificação de Email Obrigatória no Cadastro")
class RegistrationEmailVerificationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @SpyBean
    private EmailService emailService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    // Dados de teste para usuário comum
    private static final String COMMON_USER_EMAIL = "common.user@test.com";
    private static final String COMMON_USER_PASSWORD = "TestPassword123!";
    private static final String COMMON_USER_CPF = CpfGenerator.generateCpf();
    private static final String COMMON_USER_FIRST_NAME = "João";
    private static final String COMMON_USER_LAST_NAME = "Silva";

    // Dados de teste para usuário admin
    private static final String ADMIN_USER_EMAIL = "admin.user@test.com";
    private static final String ADMIN_USER_PASSWORD = "AdminPassword123!";
    private static final String ADMIN_USER_CPF = CpfGenerator.generateCpf();
    private static final String ADMIN_USER_FIRST_NAME = "Admin";
    private static final String ADMIN_USER_LAST_NAME = "Sistema";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        // Limpar dados de teste anteriores - refresh tokens primeiro para evitar violação de chave estrangeira
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("1. Deve cadastrar usuário comum com email NÃO verificado por padrão")
    void shouldRegisterCommonUserWithUnverifiedEmail() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName(COMMON_USER_FIRST_NAME);
        request.setLastName(COMMON_USER_LAST_NAME);
        request.setEmail(COMMON_USER_EMAIL);
        request.setCpf(COMMON_USER_CPF);
        request.setPassword(COMMON_USER_PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(COMMON_USER_EMAIL));

        // Verificar que o usuário foi criado com email não verificado
        Optional<User> userOpt = userRepository.findByEmail(COMMON_USER_EMAIL);
        assertThat(userOpt).isPresent();
        
        User user = userOpt.get();
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getVerificationToken()).isNotNull();
        assertThat(user.getVerificationTokenExpiresAt()).isNotNull();
        assertThat(user.getVerificationTokenExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(user.getRole()).isEqualTo(UserRole.USER);

        // Verificar que email de verificação foi enviado
        verify(emailService).sendVerificationEmail(any(User.class), any(String.class));
    }

    @Test
    @Order(2)
    @DisplayName("2. Deve FALHAR login de usuário comum com email não verificado")
    void shouldFailLoginForUnverifiedCommonUser() throws Exception {
        // Primeiro, cadastrar o usuário
        shouldRegisterCommonUserWithUnverifiedEmail();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(COMMON_USER_EMAIL);
        loginRequest.setPassword(COMMON_USER_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email não verificado. Verifique sua caixa de entrada e clique no link de verificação."));
    }

    @Test
    @Order(3)
    @DisplayName("3. Deve verificar email e permitir login após verificação")
    void shouldVerifyEmailAndAllowLogin() throws Exception {
        // Primeiro, cadastrar o usuário
        shouldRegisterCommonUserWithUnverifiedEmail();

        // Obter o token de verificação
        Optional<User> userOpt = userRepository.findByEmail(COMMON_USER_EMAIL);
        assertThat(userOpt).isPresent();
        
        User user = userOpt.get();
        String verificationToken = user.getVerificationToken();
        assertThat(verificationToken).isNotNull();

        // Verificar o email
        boolean verificationResult = emailVerificationService.verifyEmailToken(verificationToken);
        assertThat(verificationResult).isTrue();

        // Verificar que o usuário agora tem email verificado
        userOpt = userRepository.findByEmail(COMMON_USER_EMAIL);
        assertThat(userOpt).isPresent();
        user = userOpt.get();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getVerificationToken()).isNull();
        assertThat(user.getVerificationTokenExpiresAt()).isNull();

        // Agora o login deve funcionar
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(COMMON_USER_EMAIL);
        loginRequest.setPassword(COMMON_USER_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.user.email").value(COMMON_USER_EMAIL));
    }

    @Test
    @Order(4)
    @DisplayName("4. Deve cadastrar primeiro usuário como ADMIN com email automaticamente verificado")
    void shouldRegisterFirstUserAsAdminWithVerifiedEmail() throws Exception {
        // Garantir que não há usuários no banco
        userRepository.deleteAll();

        RegisterRequest request = new RegisterRequest();
        request.setFirstName(ADMIN_USER_FIRST_NAME);
        request.setLastName(ADMIN_USER_LAST_NAME);
        request.setEmail(ADMIN_USER_EMAIL);
        request.setCpf(ADMIN_USER_CPF);
        request.setPassword(ADMIN_USER_PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(ADMIN_USER_EMAIL));

        // Verificar que o primeiro usuário é ADMIN e tem email automaticamente verificado
        Optional<User> userOpt = userRepository.findByEmail(ADMIN_USER_EMAIL);
        assertThat(userOpt).isPresent();
        
        User user = userOpt.get();
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(user.isEmailVerified()).isTrue(); // ADMIN deve ter email automaticamente verificado
        assertThat(user.getVerificationToken()).isNull();
        assertThat(user.getVerificationTokenExpiresAt()).isNull();
    }

    @Test
    @Order(5)
    @DisplayName("5. Deve permitir login imediato de usuário ADMIN (email automaticamente verificado)")
    void shouldAllowImmediateLoginForAdminUser() throws Exception {
        // Primeiro, cadastrar o usuário admin
        shouldRegisterFirstUserAsAdminWithVerifiedEmail();

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(ADMIN_USER_EMAIL);
        loginRequest.setPassword(ADMIN_USER_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.user.email").value(ADMIN_USER_EMAIL))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"));
    }

    @Test
    @Order(6)
    @DisplayName("6. Deve garantir que segundo usuário seja comum e precise verificar email")
    void shouldEnsureSecondUserIsCommonAndNeedsVerification() throws Exception {
        // Primeiro, cadastrar o usuário admin
        shouldRegisterFirstUserAsAdminWithVerifiedEmail();

        // Agora cadastrar um segundo usuário (deve ser comum)
        String secondUserEmail = "second.user@test.com";
        String secondUserCpf = CpfGenerator.generateCpf();

        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Segundo");
        request.setLastName("Usuário");
        request.setEmail(secondUserEmail);
        request.setCpf(secondUserCpf);
        request.setPassword("SecondPassword123!");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(secondUserEmail));

        // Verificar que o segundo usuário é comum e precisa verificar email
        Optional<User> userOpt = userRepository.findByEmail(secondUserEmail);
        assertThat(userOpt).isPresent();
        
        User user = userOpt.get();
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.isEmailVerified()).isFalse(); // Deve precisar verificar email
        assertThat(user.getVerificationToken()).isNotNull();
        assertThat(user.getVerificationTokenExpiresAt()).isNotNull();

        // Tentar fazer login deve falhar
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(secondUserEmail);
        loginRequest.setPassword("SecondPassword123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email não verificado. Verifique sua caixa de entrada e clique no link de verificação."));
    }

    @Test
    @Order(7)
    @DisplayName("7. Deve validar que TODOS os usuários comuns tenham verificação de email obrigatória")
    void shouldValidateAllCommonUsersRequireEmailVerification() throws Exception {
        // Cadastrar múltiplos usuários e verificar que todos precisam verificar email
        for (int i = 1; i <= 3; i++) {
            String email = "user" + i + "@test.com";
            String cpf = CpfGenerator.generateCpf();

            RegisterRequest request = new RegisterRequest();
            request.setFirstName("Usuário");
            request.setLastName("Teste " + i);
            request.setEmail(email);
            request.setCpf(cpf);
            request.setPassword("TestPassword123!");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // Verificar que cada usuário precisa verificar email
            Optional<User> userOpt = userRepository.findByEmail(email);
            assertThat(userOpt).isPresent();
            
            User user = userOpt.get();
            assertThat(user.getRole()).isEqualTo(UserRole.USER);
            assertThat(user.isEmailVerified()).isFalse();
            assertThat(user.getVerificationToken()).isNotNull();
            assertThat(user.getVerificationTokenExpiresAt()).isNotNull();

            // Verificar que login falha sem verificação
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setEmail(email);
            loginRequest.setPassword("TestPassword123!");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Email não verificado. Verifique sua caixa de entrada e clique no link de verificação."));
        }

        // Verificar estatísticas finais
        long totalUsers = userRepository.count();
        long verifiedUsers = userRepository.findAll().stream()
                .mapToLong(user -> user.isEmailVerified() ? 1 : 0)
                .sum();
        long unverifiedUsers = totalUsers - verifiedUsers;

        assertThat(totalUsers).isGreaterThan(3);
        assertThat(unverifiedUsers).isEqualTo(3); // Os 3 usuários comuns cadastrados
        
        // Deve haver pelo menos 1 usuário verificado (o admin)
        assertThat(verifiedUsers).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(8)
    @DisplayName("8. Deve verificar que serviço de verificação de email está funcionando corretamente")
    void shouldVerifyEmailVerificationServiceIsWorking() {
        // Verificar que o serviço está configurado corretamente
        assertThat(emailVerificationService).isNotNull();
        
        // Criar usuário de teste
        User testUser = new User();
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test.verification@example.com");
        testUser.setPassword("TestPassword123!");
        testUser.setCpf(CpfGenerator.generateCpf());
        testUser.setRole(UserRole.USER);
        testUser.setEmailVerified(false);
        
        User savedUser = userRepository.save(testUser);
        
        // Gerar token de verificação
        String token = emailVerificationService.generateVerificationToken(savedUser);
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        
        // Verificar que o usuário precisa verificar email
        boolean needsVerification = emailVerificationService.needsEmailVerification(savedUser);
        assertThat(needsVerification).isTrue();
        
        // Verificar o token
        boolean result = emailVerificationService.verifyEmailToken(token);
        assertThat(result).isTrue();
        
        // Verificar que o usuário agora está verificado
        Optional<User> updatedUserOpt = userRepository.findById(savedUser.getId());
        assertThat(updatedUserOpt).isPresent();
        User updatedUser = updatedUserOpt.get();
        assertThat(updatedUser.isEmailVerified()).isTrue();
        assertThat(updatedUser.getVerificationToken()).isNull();
    }
}