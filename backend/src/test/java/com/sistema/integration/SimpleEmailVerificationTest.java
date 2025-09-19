package com.sistema.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import com.sistema.service.EmailService;
import com.sistema.service.SmtpService;
import com.sistema.service.AttemptService;
import com.sistema.util.CpfGenerator;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

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
@DisplayName("Teste Simples - Verificação de Email")
class SimpleEmailVerificationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @MockBean
    private SmtpService smtpService;

    @Autowired
    private AttemptService attemptService;

    private final String testEmail = "simple.test@example.com";
    private final String testPassword = "Password123!";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        // Limpar tentativas de login para evitar captcha
        try {
            attemptService.clearLoginAttempts("127.0.0.1");
        } catch (Exception e) {
            // Ignorar erros de Redis se não estiver disponível
        }
    }

    @Test
    @DisplayName("Deve permitir login para usuário com email já verificado")
    void shouldAllowLoginForVerifiedUser() throws Exception {
        // 1. Criar usuário diretamente no banco com email já verificado
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(testEmail);
        user.setCpf(CpfGenerator.generateCpf());
        user.setPassword(passwordEncoder.encode(testPassword));
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        user.setEmailVerified(true); // Email já verificado
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        
        User savedUser = userRepository.save(user);
        userRepository.flush();

        System.out.println("DEBUG: Usuário criado - Email verificado: " + user.isEmailVerified());
        System.out.println("DEBUG: Usuário criado - Enabled: " + user.isEnabled());
        System.out.println("DEBUG: Usuário salvo - ID: " + savedUser.getId());
        System.out.println("DEBUG: Usuário salvo - Email verificado: " + savedUser.isEmailVerified());
        
        // Verificar se o usuário foi realmente persistido
        Optional<User> userFromDb = userRepository.findByEmail(testEmail);
        if (userFromDb.isPresent()) {
            User dbUser = userFromDb.get();
            System.out.println("DEBUG: Usuário do banco - Email verificado: " + dbUser.isEmailVerified());
            System.out.println("DEBUG: Usuário do banco - Enabled: " + dbUser.isEnabled());
            System.out.println("DEBUG: Usuário do banco - Role: " + dbUser.getRole());
        } else {
            System.out.println("DEBUG: ERRO - Usuário não encontrado no banco!");
        }

        // 2. Tentar fazer login - deve ser bem-sucedido
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", testEmail);
        loginRequest.put("password", testPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> loginEntity = new HttpEntity<>(objectMapper.writeValueAsString(loginRequest), headers);
        ResponseEntity<String> loginResult = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login", 
                loginEntity, 
                String.class);

        System.out.println("DEBUG: Status do login: " + loginResult.getStatusCode());
        System.out.println("DEBUG: Body do login: " + loginResult.getBody());

        assertThat(loginResult.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResult.getBody()).contains("accessToken");
        assertThat(loginResult.getBody()).contains("refreshToken");
        assertThat(loginResult.getBody()).contains("Bearer");
        assertThat(loginResult.getBody()).contains(testEmail);
        assertThat(loginResult.getBody()).contains("\"emailVerified\":true");
    }

    @Test
    @DisplayName("Deve rejeitar login para usuário com email não verificado")
    void shouldRejectLoginForUnverifiedUser() throws Exception {
        // 1. Criar usuário diretamente no banco com email NÃO verificado
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(testEmail);
        user.setCpf(CpfGenerator.generateCpf());
        user.setPassword(passwordEncoder.encode(testPassword));
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        user.setEmailVerified(false); // Email NÃO verificado
        user.setVerificationToken("test-token");
        user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
        
        userRepository.save(user);
        userRepository.flush();

        System.out.println("DEBUG: Usuário criado - Email verificado: " + user.isEmailVerified());
        System.out.println("DEBUG: Usuário criado - Enabled: " + user.isEnabled());

        // 2. Tentar fazer login - deve falhar por email não verificado
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", testEmail);
        loginRequest.put("password", testPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> loginEntity = new HttpEntity<>(objectMapper.writeValueAsString(loginRequest), headers);
        ResponseEntity<String> loginResult = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login", 
                loginEntity, 
                String.class);

        System.out.println("DEBUG: Status do login: " + loginResult.getStatusCode());
        System.out.println("DEBUG: Body do login: " + loginResult.getBody());

        assertThat(loginResult.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(loginResult.getBody()).contains("Email não verificado");
    }
}