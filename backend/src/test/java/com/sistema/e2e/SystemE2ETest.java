package com.sistema.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.entity.ImageResizeQueue;
import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.ImageResizeQueueRepository;
import com.sistema.repository.UserRepository;
import com.sistema.util.CpfGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

/**
 * Testes End-to-End para fluxos completos do sistema.
 * 
 * Testa:
 * - Fluxo completo de autenticação e autorização
 * - Integração entre sistema de filas e telemetria
 * - Fluxos de recuperação de senha
 * - Cenários de erro e recuperação
 * - Performance de fluxos completos
 * - Monitoramento e observabilidade
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("dev-test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_e2e",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.telemetry.enabled=true",
    "app.email.enabled=false",
    "logging.level.com.sistema=DEBUG"
})
@Transactional
@DisplayName("Sistema - Testes End-to-End")
class SystemE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImageResizeQueueRepository queueRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String authToken;

    @BeforeEach
    void setUp() {
        // Limpar dados
        queueRepository.deleteAll();
        userRepository.deleteAll();

        // Criar usuário de teste
        testUser = new User();
        testUser.setFirstName("João");
        testUser.setLastName("Silva");
        testUser.setEmail("joao.silva@teste.com");
        testUser.setCpf(CpfGenerator.generateCpf());
        testUser.setPassword(passwordEncoder.encode("MinhaSenh@123"));
        testUser.setRole(UserRole.USER);
        testUser.setEnabled(true);
        testUser.setEmailVerified(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(testUser);
    }

    @Nested
    @DisplayName("Fluxo Completo de Autenticação")
    class CompleteAuthenticationFlow {

        @Test
        @DisplayName("Deve realizar fluxo completo de login e acesso a recursos protegidos")
        void shouldPerformCompleteLoginAndAccessProtectedResources() throws Exception {
            // 1. Login
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("email", testUser.getEmail());
            loginRequest.put("senha", "MinhaSenh@123");

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.user.email").value(testUser.getEmail()))
                    .andReturn();

            String loginResponse = loginResult.getResponse().getContentAsString();
            Map<String, Object> loginData = objectMapper.readValue(loginResponse, Map.class);
            authToken = (String) loginData.get("token");

            // 2. Acessar informações do usuário
            mockMvc.perform(get("/api/auth/me")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                    .andExpect(jsonPath("$.role").value("ADMIN"));

            // 3. Acessar recurso protegido (filas)
            mockMvc.perform(get("/api/image-resize-queue")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());

            // 4. Logout
            mockMvc.perform(post("/api/auth/logout")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // 5. Verificar que token foi invalidado
            mockMvc.perform(get("/api/auth/me")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Deve realizar fluxo de refresh token")
        void shouldPerformRefreshTokenFlow() throws Exception {
            // 1. Login inicial
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("email", testUser.getEmail());
            loginRequest.put("senha", "MinhaSenh@123");

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String loginResponse = loginResult.getResponse().getContentAsString();
            Map<String, Object> loginData = objectMapper.readValue(loginResponse, Map.class);
            String originalToken = (String) loginData.get("token");
            String refreshToken = (String) loginData.get("refreshToken");

            // 2. Usar refresh token
            Map<String, String> refreshRequest = new HashMap<>();
            refreshRequest.put("refreshToken", refreshToken);

            MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andReturn();

            String refreshResponse = refreshResult.getResponse().getContentAsString();
            Map<String, Object> refreshData = objectMapper.readValue(refreshResponse, Map.class);
            String newToken = (String) refreshData.get("token");

            // 3. Verificar que novo token funciona
            mockMvc.perform(get("/api/auth/me")
                    .header("Authorization", "Bearer " + newToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(testUser.getEmail()));

            // 4. Verificar que token original foi invalidado
            mockMvc.perform(get("/api/auth/me")
                    .header("Authorization", "Bearer " + originalToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Fluxo Completo de Gerenciamento de Filas")
    class CompleteQueueManagementFlow {

        @Test
        @DisplayName("Deve realizar fluxo completo de criação e processamento de fila")
        void shouldPerformCompleteQueueCreationAndProcessingFlow() throws Exception {
            // 1. Fazer login
            authToken = performLogin();

            // 2. Criar nova fila
            Map<String, Object> queueRequest = new HashMap<>();
            queueRequest.put("originalPath", "/uploads/test-image.jpg");
            queueRequest.put("targetPath", "/resized/test-image-thumb.jpg");
            queueRequest.put("targetWidth", 150);
            queueRequest.put("targetHeight", 150);
            queueRequest.put("targetFormat", "JPEG");
            queueRequest.put("quality", 85);
            queueRequest.put("priority", "HIGH");

            MvcResult createResult = mockMvc.perform(post("/api/image-resize-queue")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(queueRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andReturn();

            String createResponse = createResult.getResponse().getContentAsString();
            Map<String, Object> queueData = objectMapper.readValue(createResponse, Map.class);
            Long queueId = Long.valueOf(queueData.get("id").toString());

            // 3. Verificar que fila foi criada
            mockMvc.perform(get("/api/image-resize-queue/{id}", queueId)
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.priority").value("HIGH"));

            // 4. Simular início do processamento
            Map<String, String> statusUpdate = new HashMap<>();
            statusUpdate.put("status", "PROCESSING");

            mockMvc.perform(put("/api/image-resize-queue/{id}/status", queueId)
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(statusUpdate)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PROCESSING"));

            // 5. Simular conclusão do processamento
            statusUpdate.put("status", "COMPLETED");

            mockMvc.perform(put("/api/image-resize-queue/{id}/status", queueId)
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(statusUpdate)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));

            // 6. Verificar estatísticas
            mockMvc.perform(get("/api/image-resize-queue/statistics")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(1))
                    .andExpect(jsonPath("$.completed").value(1));
        }

        @Test
        @DisplayName("Deve realizar fluxo de tratamento de erro e reprocessamento")
        void shouldPerformErrorHandlingAndReprocessingFlow() throws Exception {
            // 1. Fazer login
            authToken = performLogin();

            // 2. Criar fila
            Map<String, Object> queueRequest = new HashMap<>();
            queueRequest.put("originalPath", "/uploads/problematic-image.jpg");
            queueRequest.put("targetPath", "/resized/problematic-image-thumb.jpg");
            queueRequest.put("targetWidth", 200);
            queueRequest.put("targetHeight", 200);
            queueRequest.put("priority", "MEDIUM");

            MvcResult createResult = mockMvc.perform(post("/api/image-resize-queue")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(queueRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            String createResponse = createResult.getResponse().getContentAsString();
            Map<String, Object> queueData = objectMapper.readValue(createResponse, Map.class);
            Long queueId = Long.valueOf(queueData.get("id").toString());

            // 3. Simular falha no processamento
            Map<String, String> failureUpdate = new HashMap<>();
            failureUpdate.put("status", "FAILED");

            mockMvc.perform(put("/api/image-resize-queue/{id}/status", queueId)
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(failureUpdate)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FAILED"));

            // 4. Buscar itens falhados
            mockMvc.perform(get("/api/image-resize-queue/failed")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].status").value("FAILED"));

            // 5. Reprocessar item falhado
            Map<String, String> retryUpdate = new HashMap<>();
            retryUpdate.put("status", "PENDING");

            mockMvc.perform(put("/api/image-resize-queue/{id}/status", queueId)
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(retryUpdate)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"));

            // 6. Simular processamento bem-sucedido
            Map<String, String> successUpdate = new HashMap<>();
            successUpdate.put("status", "COMPLETED");

            mockMvc.perform(put("/api/image-resize-queue/{id}/status", queueId)
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(successUpdate)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }
    }

    @Nested
    @DisplayName("Fluxo de Monitoramento e Observabilidade")
    class MonitoringAndObservabilityFlow {

        @Test
        @DisplayName("Deve coletar métricas durante fluxo completo")
        void shouldCollectMetricsDuringCompleteFlow() throws Exception {
            // 1. Verificar health check
            mockMvc.perform(get("/api/health")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));

            // 2. Fazer login (gera métricas de autenticação)
            authToken = performLogin();

            // 3. Criar múltiplas filas (gera métricas de API)
            for (int i = 0; i < 5; i++) {
                Map<String, Object> queueRequest = new HashMap<>();
                queueRequest.put("originalPath", "/uploads/test-" + i + ".jpg");
                queueRequest.put("targetPath", "/resized/test-" + i + "-thumb.jpg");
                queueRequest.put("targetWidth", 100 + i * 10);
                queueRequest.put("targetHeight", 100 + i * 10);
                queueRequest.put("priority", i % 2 == 0 ? "HIGH" : "LOW");

                mockMvc.perform(post("/api/image-resize-queue")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queueRequest)))
                        .andExpect(status().isCreated());
            }

            // 4. Verificar métricas do Actuator
            mockMvc.perform(get("/actuator/metrics")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.names").isArray());

            // 5. Verificar métricas específicas de HTTP
            mockMvc.perform(get("/actuator/metrics/http.server.requests")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.measurements").isArray());

            // 6. Verificar endpoint Prometheus
            mockMvc.perform(get("/actuator/prometheus")
                    .contentType(MediaType.TEXT_PLAIN))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("http_server_requests")));
        }

        @Test
        @DisplayName("Deve manter performance durante carga de trabalho")
        void shouldMaintainPerformanceDuringWorkload() throws Exception {
            // 1. Fazer login
            authToken = performLogin();

            // 2. Medir tempo de resposta para múltiplas operações
            long startTime = System.currentTimeMillis();

            // Criar 20 filas
            for (int i = 0; i < 20; i++) {
                Map<String, Object> queueRequest = new HashMap<>();
                queueRequest.put("originalPath", "/uploads/perf-test-" + i + ".jpg");
                queueRequest.put("targetPath", "/resized/perf-test-" + i + "-thumb.jpg");
                queueRequest.put("targetWidth", 150);
                queueRequest.put("targetHeight", 150);
                queueRequest.put("priority", "MEDIUM");

                mockMvc.perform(post("/api/image-resize-queue")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queueRequest)))
                        .andExpect(status().isCreated());
            }

            // Listar filas com paginação
            mockMvc.perform(get("/api/image-resize-queue")
                    .header("Authorization", "Bearer " + authToken)
                    .param("page", "0")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(10));

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;

            // 3. Verificar que operações foram executadas em tempo razoável
            assertThat(totalTime).isLessThan(10000); // Menos de 10 segundos

            // 4. Verificar estatísticas
            mockMvc.perform(get("/api/image-resize-queue/statistics")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(20))
                    .andExpect(jsonPath("$.pending").value(20));
        }
    }

    @Nested
    @DisplayName("Fluxos de Recuperação de Senha")
    class PasswordRecoveryFlows {

        @Test
        @DisplayName("Deve realizar fluxo completo de recuperação de senha")
        void shouldPerformCompletePasswordRecoveryFlow() throws Exception {
            // 1. Verificar CPF
            Map<String, String> cpfRequest = new HashMap<>();
            cpfRequest.put("cpf", testUser.getCpf());

            MvcResult cpfResult = mockMvc.perform(post("/api/auth/verify-cpf")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cpfRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.maskedEmail").exists())
                    .andReturn();

            // 2. Confirmar email
            Map<String, String> emailRequest = new HashMap<>();
            emailRequest.put("cpf", testUser.getCpf());
            emailRequest.put("email", testUser.getEmail());
            emailRequest.put("captcha", "dummy-captcha"); // Em ambiente de teste

            mockMvc.perform(post("/api/auth/confirm-email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(emailRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // 3. Simular recuperação de senha (em ambiente real seria via email)
            Map<String, String> recoveryRequest = new HashMap<>();
            recoveryRequest.put("token", "dummy-recovery-token");

            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(recoveryRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Cenários de Erro e Recuperação")
    class ErrorAndRecoveryScenarios {

        @Test
        @DisplayName("Deve lidar com falhas de autenticação e recuperar")
        void shouldHandleAuthenticationFailuresAndRecover() throws Exception {
            // 1. Tentar login com credenciais inválidas
            Map<String, String> invalidLogin = new HashMap<>();
            invalidLogin.put("email", testUser.getEmail());
            invalidLogin.put("senha", "SenhaErrada123");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidLogin)))
                    .andExpect(status().isUnauthorized());

            // 2. Fazer login correto
            Map<String, String> validLogin = new HashMap<>();
            validLogin.put("email", testUser.getEmail());
            validLogin.put("senha", "MinhaSenh@123");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validLogin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists());
        }

        @Test
        @DisplayName("Deve lidar com token expirado e renovar")
        void shouldHandleExpiredTokenAndRenew() throws Exception {
            // 1. Fazer login
            authToken = performLogin();

            // 2. Verificar que token funciona
            mockMvc.perform(get("/api/auth/me")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // 3. Simular token inválido
            mockMvc.perform(get("/api/auth/me")
                    .header("Authorization", "Bearer invalid-token")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());

            // 4. Usar token válido novamente
            mockMvc.perform(get("/api/auth/me")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Integração Completa do Sistema")
    class CompleteSystemIntegration {

        @Test
        @DisplayName("Deve executar fluxo completo do sistema com telemetria")
        void shouldExecuteCompleteSystemFlowWithTelemetry() throws Exception {
            // 1. Health check
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk());

            // 2. Login
            authToken = performLogin();

            // 3. Criar filas com diferentes prioridades
            Long highPriorityId = createQueue("HIGH", "/uploads/high.jpg");
            Long mediumPriorityId = createQueue("MEDIUM", "/uploads/medium.jpg");
            Long lowPriorityId = createQueue("LOW", "/uploads/low.jpg");

            // 4. Processar filas em ordem de prioridade
            processQueue(highPriorityId, "COMPLETED");
            processQueue(mediumPriorityId, "PROCESSING");
            processQueue(lowPriorityId, "FAILED");

            // 5. Verificar estatísticas finais
            mockMvc.perform(get("/api/image-resize-queue/statistics")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(3))
                    .andExpect(jsonPath("$.completed").value(1))
                    .andExpect(jsonPath("$.processing").value(1))
                    .andExpect(jsonPath("$.failed").value(1));

            // 6. Verificar métricas de telemetria
            mockMvc.perform(get("/actuator/metrics/http.server.requests")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.measurements").isArray());

            // 7. Logout
            mockMvc.perform(post("/api/auth/logout")
                    .header("Authorization", "Bearer " + authToken)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    // Métodos auxiliares

    private String performLogin() throws Exception {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", testUser.getEmail());
        loginRequest.put("senha", "MinhaSenh@123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Map<String, Object> data = objectMapper.readValue(response, Map.class);
        return (String) data.get("token");
    }

    private Long createQueue(String priority, String originalPath) throws Exception {
        Map<String, Object> queueRequest = new HashMap<>();
        queueRequest.put("originalPath", originalPath);
        queueRequest.put("targetPath", originalPath.replace("/uploads/", "/resized/").replace(".jpg", "-thumb.jpg"));
        queueRequest.put("targetWidth", 150);
        queueRequest.put("targetHeight", 150);
        queueRequest.put("priority", priority);

        MvcResult result = mockMvc.perform(post("/api/image-resize-queue")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(queueRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Map<String, Object> data = objectMapper.readValue(response, Map.class);
        return Long.valueOf(data.get("id").toString());
    }

    private void processQueue(Long queueId, String finalStatus) throws Exception {
        // Simular processamento
        Map<String, String> statusUpdate = new HashMap<>();
        statusUpdate.put("status", "PROCESSING");

        mockMvc.perform(put("/api/image-resize-queue/{id}/status", queueId)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk());

        // Status final
        statusUpdate.put("status", finalStatus);

        mockMvc.perform(put("/api/image-resize-queue/{id}/status", queueId)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk());
    }
}