package com.sistema.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.controller.AuthController.LoginRequest;
import com.sistema.controller.AuthController.RegisterRequest;
import com.sistema.entity.User;
import com.sistema.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureWebMvc
@Testcontainers
@Transactional
@DisplayName("Application Integration Tests")
class ApplicationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379).toString());
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // Clean up database and Redis before each test
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("Should start application with containers")
    void shouldStartApplicationWithContainers() {
        // Given
        assertThat(postgres.isRunning()).isTrue();
        assertThat(redis.isRunning()).isTrue();

        // When & Then - Application should be running
        assertThat(userRepository).isNotNull();
        assertThat(redisTemplate).isNotNull();
    }

    @Test
    @DisplayName("Should perform complete user registration and authentication flow")
    void shouldPerformCompleteUserRegistrationAndAuthenticationFlow() throws Exception {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        // When - Register user
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andReturn();

        // Then - Verify user was saved to database
        User savedUser = userRepository.findByUsername("testuser").orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.isEnabled()).isTrue();

        // And - Extract tokens from response
        String registerResponseJson = registerResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> registerResponse = objectMapper.readValue(registerResponseJson, Map.class);
        String accessToken = (String) registerResponse.get("accessToken");
        String refreshToken = (String) registerResponse.get("refreshToken");

        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        // When - Login with same credentials
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andReturn();

        // Then - Verify login response
        String loginResponseJson = loginResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> loginResponse = objectMapper.readValue(loginResponseJson, Map.class);
        
        assertThat(loginResponse.get("accessToken")).isNotNull();
        assertThat(loginResponse.get("refreshToken")).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) loginResponse.get("user");
        assertThat(user.get("username")).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should test Redis connectivity and operations")
    void shouldTestRedisConnectivityAndOperations() throws Exception {
        // Given
        String testKey = "test-key";
        String testValue = "test-value";

        // When - Test Redis through API endpoint
        mockMvc.perform(get("/api/redis-test")
                        .param("key", testKey)
                        .param("value", testValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Redis test successful"))
                .andExpect(jsonPath("$.details.key").value(testKey))
                .andExpect(jsonPath("$.details.setValue").value(testValue))
                .andExpect(jsonPath("$.details.getValue").value(testValue));

        // Then - Verify value was actually stored in Redis
        Object storedValue = redisTemplate.opsForValue().get(testKey);
        assertThat(storedValue).isEqualTo(testValue);
    }

    @Test
    @DisplayName("Should test health endpoints with database and Redis")
    void shouldTestHealthEndpointsWithDatabaseAndRedis() throws Exception {
        // When & Then - Test health endpoint
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.details.database").value("Connected"))
                .andExpect(jsonPath("$.details.redis").value("Connected"));

        // When & Then - Test info endpoint
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("Sistema Java"))
                .andExpect(jsonPath("$.app.version").exists())
                .andExpect(jsonPath("$.java.version").exists())
                .andExpect(jsonPath("$.spring.version").exists());
    }

    @Test
    @DisplayName("Should test JWT token refresh flow")
    void shouldTestJwtTokenRefreshFlow() throws Exception {
        // Given - Register and login user first
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("refreshuser");
        registerRequest.setEmail("refresh@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Refresh");
        registerRequest.setLastName("User");

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String registerResponseJson = registerResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> registerResponse = objectMapper.readValue(registerResponseJson, Map.class);
        String refreshToken = (String) registerResponse.get("refreshToken");

        // When - Use refresh token to get new access token
        mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.username").value("refreshuser"));
    }

    @Test
    @DisplayName("Should test user management operations")
    void shouldTestUserManagementOperations() throws Exception {
        // Given - Create admin user
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("$2a$10$encoded.password.hash"); // BCrypt encoded
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        // Role será definida automaticamente como USER no AuthService
        adminUser.setEnabled(true);
        userRepository.save(adminUser);

        // And - Create regular user
        User regularUser = new User();
        regularUser.setUsername("regular");
        regularUser.setEmail("regular@example.com");
        regularUser.setPassword("$2a$10$encoded.password.hash");
        regularUser.setFirstName("Regular");
        regularUser.setLastName("User");
        // Role será definida automaticamente como USER no AuthService
        regularUser.setEnabled(true);
        User savedRegularUser = userRepository.save(regularUser);

        // When & Then - Test finding users
        assertThat(userRepository.findByUsername("admin")).isPresent();
        assertThat(userRepository.findByUsername("regular")).isPresent();
        assertThat(userRepository.findByEmail("admin@example.com")).isPresent();
        
        // And - Test user count
        long userCount = userRepository.count();
        assertThat(userCount).isEqualTo(2);

        // And - Test finding active users
        assertThat(userRepository.findByEnabledTrue()).hasSize(2);
    }

    @Test
    @DisplayName("Should test token blacklist functionality")
    void shouldTestTokenBlacklistFunctionality() throws Exception {
        // Given - Register user and get tokens
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("blacklistuser");
        registerRequest.setEmail("blacklist@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("Blacklist");
        registerRequest.setLastName("User");

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String registerResponseJson = registerResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> registerResponse = objectMapper.readValue(registerResponseJson, Map.class);
        String accessToken = (String) registerResponse.get("accessToken");

        // When - Logout user (should blacklist token)
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));

        // Then - Verify token is blacklisted by checking Redis
        // The token should be stored in Redis blacklist
        assertThat(redisTemplate.hasKey("blacklist:" + accessToken)).isTrue();
    }

    @Test
    @DisplayName("Should test home page rendering")
    void shouldTestHomePageRendering() throws Exception {
        // When & Then - Test home page
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("appName", "Sistema Java"))
                .andExpect(model().attribute("version", "1.0.0"));

        // When & Then - Test api-simple redirect
        mockMvc.perform(get("/api-simple"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("Should test database transactions and rollback")
    void shouldTestDatabaseTransactionsAndRollback() {
        // Given
        long initialCount = userRepository.count();

        // When - Create user in transaction
        User testUser = new User();
        testUser.setUsername("transactionuser");
        testUser.setEmail("transaction@example.com");
        testUser.setPassword("password");
        testUser.setFirstName("Transaction");
        testUser.setLastName("User");
        // Role será definida automaticamente como USER no AuthService
        testUser.setEnabled(true);
        
        User savedUser = userRepository.save(testUser);
        
        // Then - Verify user was saved
        assertThat(savedUser.getId()).isNotNull();
        assertThat(userRepository.count()).isEqualTo(initialCount + 1);
        
        // And - Verify user can be found
        assertThat(userRepository.findByUsername("transactionuser")).isPresent();
    }

    @Test
    @DisplayName("Should test error handling for invalid requests")
    void shouldTestErrorHandlingForInvalidRequests() throws Exception {
        // When & Then - Test invalid login
        LoginRequest invalidLogin = new LoginRequest();
        invalidLogin.setUsernameOrEmail("nonexistent");
        invalidLogin.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized());

        // When & Then - Test invalid registration (duplicate username)
        RegisterRequest firstUser = new RegisterRequest();
        firstUser.setUsername("duplicate");
        firstUser.setEmail("first@example.com");
        firstUser.setPassword("password123");
        firstUser.setFirstName("First");
        firstUser.setLastName("User");

        // Register first user
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstUser)))
                .andExpect(status().isOk());

        // Try to register second user with same username
        RegisterRequest duplicateUser = new RegisterRequest();
        duplicateUser.setUsername("duplicate");
        duplicateUser.setEmail("second@example.com");
        duplicateUser.setPassword("password123");
        duplicateUser.setFirstName("Second");
        duplicateUser.setLastName("User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isBadRequest());
    }
}