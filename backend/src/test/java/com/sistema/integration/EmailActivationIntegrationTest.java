package com.sistema.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import com.sistema.service.EmailService;
import com.sistema.service.EmailVerificationService;
import com.sistema.service.SmtpService;
import com.sistema.util.CpfGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


/**
 * Testes de integração para o fluxo completo de ativação por email.
 * Testa desde o cadastro até o login, passando pela verificação de email.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.cache.type=simple",
        "spring.redis.host=",
        "spring.redis.port=",
        "management.health.redis.enabled=false",
        "spring.data.redis.repositories.enabled=false"
    }
)
@ActiveProfiles("test")
@DisplayName("Testes de Integração - Fluxo Completo de Ativação por Email")
class EmailActivationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private SmtpService smtpService;

    private Map<String, Object> validUserRegistration;
    private final String testEmail = "integration.test@example.com";
    private final String testPassword = "Password123!";

    @BeforeEach
    void setUp() {
        // Limpar dados de teste anteriores
        userRepository.deleteAll();

        // Criar um usuário admin primeiro para garantir que o usuário de teste não seja o primeiro
        User adminUser = new User();
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword("$2a$10$dummy.hash.for.admin.user");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("Test");
        adminUser.setCpf(CpfGenerator.generateCpf());
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setEmailVerified(true);
        adminUser.setActive(true);
        adminUser.setCreatedAt(LocalDateTime.now());
        adminUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(adminUser);

        // Configurar dados de teste válidos
        validUserRegistration = new HashMap<>();
        validUserRegistration.put("firstName", "Integration");
        validUserRegistration.put("lastName", "Test");
        validUserRegistration.put("email", testEmail);
        validUserRegistration.put("cpf", CpfGenerator.generateCpf());
        validUserRegistration.put("password", testPassword);

        // Mock do serviço de email para não enviar emails reais
        when(emailService.sendVerificationEmail(any(User.class), anyString())).thenReturn(true);
        when(smtpService.sendHtmlEmail(anyString(), anyString(), anyString())).thenReturn(true);
    }

    private ResponseEntity<String> postRequest(String endpoint, Object requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            return restTemplate.postForEntity("http://localhost:" + port + endpoint, entity, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao fazer requisição POST", e);
        }
    }

    @Test
    @DisplayName("Fluxo completo: Cadastro → Email não verificado → Verificação → Login bem-sucedido")
    void shouldCompleteFullActivationFlow() throws Exception {
        // 1. CADASTRO - Deve criar usuário com email não verificado
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> registerEntity = new HttpEntity<>(objectMapper.writeValueAsString(validUserRegistration), headers);
        
        ResponseEntity<String> registerResult = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/register", 
                registerEntity, 
                String.class);
        
        assertThat(registerResult.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResult.getBody()).contains("accessToken");
        assertThat(registerResult.getBody()).contains("refreshToken");
        assertThat(registerResult.getBody()).contains("tokenType");
        assertThat(registerResult.getBody()).contains("Bearer");
        assertThat(registerResult.getBody()).contains(testEmail);

        // Verificar que o email de verificação foi enviado
        verify(emailService, times(1)).sendVerificationEmail(any(User.class), anyString());

        // Verificar que o usuário foi criado no banco
        Optional<User> userOpt = userRepository.findByEmail(testEmail);
        assertThat(userOpt).isPresent();
        User user = userOpt.get();
        System.out.println("DEBUG: Email verificado: " + user.isEmailVerified());
        System.out.println("DEBUG: Token existe: " + (user.getVerificationToken() != null));
        System.out.println("DEBUG: Token expira em: " + user.getVerificationTokenExpiresAt());
        
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getVerificationToken()).isNotNull();
        assertThat(user.getVerificationTokenExpiresAt()).isNotNull();

        // 2. TENTATIVA DE LOGIN - Deve falhar por email não verificado
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", testEmail);
        loginRequest.put("password", testPassword);

        HttpEntity<String> loginEntity = new HttpEntity<>(objectMapper.writeValueAsString(loginRequest), headers);
        ResponseEntity<String> loginResult = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login", 
                loginEntity, 
                String.class);
        
        assertThat(loginResult.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(loginResult.getBody()).contains("Email não verificado");

        // 3. VERIFICAÇÃO DE EMAIL - Deve ativar a conta
        String verificationToken = user.getVerificationToken();
        System.out.println("DEBUG: Token de verificação: " + verificationToken);
        
        Map<String, String> verifyRequest = new HashMap<>();
        verifyRequest.put("token", verificationToken);

        HttpEntity<String> verifyEntity = new HttpEntity<>(objectMapper.writeValueAsString(verifyRequest), headers);
        ResponseEntity<String> verifyResult = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/verify-email", 
                verifyEntity, 
                String.class);
        
        System.out.println("DEBUG: Status da verificação: " + verifyResult.getStatusCode());
        System.out.println("DEBUG: Body da verificação: " + verifyResult.getBody());
        
        // Verificar se o usuário foi realmente atualizado no banco
        userRepository.flush();
        User userAfterVerification = userRepository.findByEmail(testEmail).orElse(null);
        System.out.println("DEBUG: Usuário após verificação - Email verificado: " + (userAfterVerification != null ? userAfterVerification.isEmailVerified() : "null"));
        System.out.println("DEBUG: Usuário após verificação - Token: " + (userAfterVerification != null ? userAfterVerification.getVerificationToken() : "null"));
        System.out.println("DEBUG: Usuário após verificação - Token expira em: " + (userAfterVerification != null ? userAfterVerification.getVerificationTokenExpiresAt() : "null"));
        
        assertThat(verifyResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(verifyResult.getBody()).contains("Email verificado com sucesso");

        // Forçar flush e clear do contexto de persistência para garantir que os dados sejam recarregados
        userRepository.flush();
        
        // Verificar que o usuário foi ativado no banco
        user = userRepository.findByEmail(testEmail).orElseThrow();
        System.out.println("DEBUG: Após verificação - Email verificado: " + user.isEmailVerified());
        System.out.println("DEBUG: Após verificação - Token: " + user.getVerificationToken());
        System.out.println("DEBUG: Após verificação - Token expira: " + user.getVerificationTokenExpiresAt());
        
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getVerificationToken()).isNull();
        assertThat(user.getVerificationTokenExpiresAt()).isNull();

        // 4. LOGIN APÓS VERIFICAÇÃO - Deve ser bem-sucedido
        HttpEntity<String> finalLoginEntity = new HttpEntity<>(objectMapper.writeValueAsString(loginRequest), headers);
        ResponseEntity<String> finalLoginResult = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login", 
                finalLoginEntity, 
                String.class);
        
        assertThat(finalLoginResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(finalLoginResult.getBody()).contains("accessToken");
        assertThat(finalLoginResult.getBody()).contains("refreshToken");
        assertThat(finalLoginResult.getBody()).contains("Bearer");
        assertThat(finalLoginResult.getBody()).contains(testEmail);
        assertThat(finalLoginResult.getBody()).contains("\"emailVerified\":true");
    }

    @Test
    @DisplayName("Deve permitir reenvio de email de verificação para usuário não verificado")
    void shouldAllowResendVerificationEmail() throws Exception {
        // 1. Cadastrar usuário
        ResponseEntity<String> registerResult = postRequest("/api/auth/register", validUserRegistration);
        assertThat(registerResult.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResult.getBody()).contains("accessToken");

        // Verificar primeiro envio
        verify(emailService, times(1)).sendVerificationEmail(any(User.class), anyString());

        // 2. Reenviar email de verificação
        Map<String, String> resendRequest = new HashMap<>();
        resendRequest.put("email", testEmail);

        ResponseEntity<String> resendResult = postRequest("/api/auth/resend-verification", resendRequest);
        assertThat(resendResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resendResult.getBody()).contains("Email de verificação reenviado com sucesso");

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
        ResponseEntity<String> registerResult = postRequest("/api/auth/register", validUserRegistration);
        assertThat(registerResult.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 2. Tentar verificar com token inválido
        Map<String, String> verifyRequest = new HashMap<>();
        verifyRequest.put("token", "token-invalido-123");

        ResponseEntity<String> verifyResult = postRequest("/api/auth/verify-email", verifyRequest);
        assertThat(verifyResult.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(verifyResult.getBody()).contains("Token de verificação inválido ou expirado");

        // Verificar que o usuário continua não verificado
        User user = userRepository.findByEmail(testEmail).orElseThrow();
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar verificação com token expirado")
    void shouldRejectVerificationWithExpiredToken() throws Exception {
        // 1. Cadastrar usuário
        ResponseEntity<String> registerResult = postRequest("/api/auth/register", validUserRegistration);
        assertThat(registerResult.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 2. Simular expiração do token
        User user = userRepository.findByEmail(testEmail).orElseThrow();
        String originalToken = user.getVerificationToken();
        
        // Forçar expiração do token (definir data no passado)
        user.setVerificationTokenExpiresAt(java.time.LocalDateTime.now().minusHours(1));
        userRepository.save(user);

        // 3. Tentar verificar com token expirado
        Map<String, String> verifyRequest = new HashMap<>();
        verifyRequest.put("token", originalToken);

        ResponseEntity<String> verifyResult = postRequest("/api/auth/verify-email", verifyRequest);
        assertThat(verifyResult.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(verifyResult.getBody()).contains("Token de verificação inválido ou expirado");

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

        ResponseEntity<String> loginResult = postRequest("/api/auth/login", loginRequest);
        assertThat(loginResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResult.getBody()).contains("accessToken");
        assertThat(loginResult.getBody()).contains("refreshToken");
        assertThat(loginResult.getBody()).contains("ADMIN");
    }

    @Test
    @DisplayName("Deve impedir múltiplas verificações com o mesmo token")
    void shouldPreventMultipleVerificationsWithSameToken() throws Exception {
        // 1. Cadastrar usuário
        ResponseEntity<String> registerResult = postRequest("/api/auth/register", validUserRegistration);
        assertThat(registerResult.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        User user = userRepository.findByEmail(testEmail).orElseThrow();
        String verificationToken = user.getVerificationToken();

        // 2. Primeira verificação - deve ser bem-sucedida
        Map<String, String> verifyRequest = new HashMap<>();
        verifyRequest.put("token", verificationToken);

        ResponseEntity<String> firstVerifyResult = postRequest("/api/auth/verify-email", verifyRequest);
        assertThat(firstVerifyResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstVerifyResult.getBody()).contains("Email verificado com sucesso");

        // 3. Segunda verificação com o mesmo token - deve falhar
        ResponseEntity<String> secondVerifyResult = postRequest("/api/auth/verify-email", verifyRequest);
        assertThat(secondVerifyResult.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(secondVerifyResult.getBody()).contains("Token de verificação inválido ou expirado");
    }

    @Test
    @DisplayName("Deve manter estado de verificação após logout e novo login")
    void shouldMaintainVerificationStateAfterLogoutAndRelogin() throws Exception {
        // 1. Fluxo completo de cadastro e verificação
        ResponseEntity<String> registerResult = postRequest("/api/auth/register", validUserRegistration);
        assertThat(registerResult.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        User user = userRepository.findByEmail(testEmail).orElseThrow();
        String verificationToken = user.getVerificationToken();

        Map<String, String> verifyRequest = new HashMap<>();
        verifyRequest.put("token", verificationToken);

        ResponseEntity<String> verifyResult = postRequest("/api/auth/verify-email", verifyRequest);
        assertThat(verifyResult.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 2. Primeiro login após verificação
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", testEmail);
        loginRequest.put("password", testPassword);

        ResponseEntity<String> loginResult = postRequest("/api/auth/login", loginRequest);
        assertThat(loginResult.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Extrair token para logout
        String responseBody = loginResult.getBody();
        Map<String, Object> loginResponse = objectMapper.readValue(responseBody, Map.class);
        String accessToken = (String) loginResponse.get("accessToken");

        // 3. Logout
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> logoutEntity = new HttpEntity<>(headers);
        
        ResponseEntity<String> logoutResult = restTemplate.exchange(
            "http://localhost:" + port + "/api/auth/logout",
            HttpMethod.POST,
            logoutEntity,
            String.class
        );
        assertThat(logoutResult.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 4. Novo login - deve continuar funcionando (email já verificado)
        ResponseEntity<String> secondLoginResult = postRequest("/api/auth/login", loginRequest);
        assertThat(secondLoginResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondLoginResult.getBody()).contains("accessToken");
        assertThat(secondLoginResult.getBody()).contains("refreshToken");
    }

    @Test
    @DisplayName("Deve impedir reenvio de verificação para usuário já verificado")
    void shouldPreventResendVerificationForAlreadyVerifiedUser() throws Exception {
        // 1. Fluxo completo de verificação
        ResponseEntity<String> registerResult = postRequest("/api/auth/register", validUserRegistration);
        assertThat(registerResult.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        User user = userRepository.findByEmail(testEmail).orElseThrow();
        String verificationToken = user.getVerificationToken();

        Map<String, String> verifyRequest = new HashMap<>();
        verifyRequest.put("token", verificationToken);

        ResponseEntity<String> verifyResult = postRequest("/api/auth/verify-email", verifyRequest);
        assertThat(verifyResult.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 2. Tentar reenviar verificação para usuário já verificado
        Map<String, String> resendRequest = new HashMap<>();
        resendRequest.put("email", testEmail);

        ResponseEntity<String> resendResult = postRequest("/api/auth/resend-verification", resendRequest);
        assertThat(resendResult.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resendResult.getBody()).contains("Email já foi verificado");

        // Verificar que não houve envio adicional de email
        verify(emailService, times(1)).sendVerificationEmail(any(User.class), anyString());
    }

    @Test
    @DisplayName("Deve validar que cadastro gera token de verificação válido")
    void shouldValidateRegistrationGeneratesValidVerificationToken() throws Exception {
        // 1. Cadastrar usuário
        ResponseEntity<String> registerResult = postRequest("/api/auth/register", validUserRegistration);
        assertThat(registerResult.getStatusCode()).isEqualTo(HttpStatus.CREATED);

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