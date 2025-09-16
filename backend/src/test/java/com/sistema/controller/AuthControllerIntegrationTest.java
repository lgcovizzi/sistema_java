package com.sistema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import com.sistema.service.AttemptService;
import com.sistema.service.CaptchaService;
import com.sistema.service.TokenBlacklistService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AttemptService attemptService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private User testUser;
    private String testUserPassword = "TestPassword123!";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Clean up any existing test data
        userRepository.deleteAll();
        
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode(testUserPassword));
        testUser.setRole(UserRole.USER);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);
    }

    @Nested
    @DisplayName("User Registration Tests")
    class UserRegistrationTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUserSuccessfully() throws Exception {
            // Given
            Map<String, String> registrationData = new HashMap<>();
            registrationData.put("username", "newuser");
            registrationData.put("email", "newuser@example.com");
            registrationData.put("password", "NewPassword123!");

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registrationData)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message", containsString("successfully")));
        }

        @Test
        @DisplayName("Should reject registration with existing email")
        void shouldRejectRegistrationWithExistingEmail() throws Exception {
            // Given
            Map<String, String> registrationData = new HashMap<>();
            registrationData.put("username", "anotheruser");
            registrationData.put("email", testUser.getEmail()); // Existing email
            registrationData.put("password", "AnotherPassword123!");

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registrationData)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", containsString("already exists")));
        }

        @Test
        @DisplayName("Should reject registration with weak password")
        void shouldRejectRegistrationWithWeakPassword() throws Exception {
            // Given
            Map<String, String> registrationData = new HashMap<>();
            registrationData.put("username", "weakpassuser");
            registrationData.put("email", "weak@example.com");
            registrationData.put("password", "weak"); // Weak password

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registrationData)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", containsString("password")));
        }

        @Test
        @DisplayName("Should reject registration with invalid email")
        void shouldRejectRegistrationWithInvalidEmail() throws Exception {
            // Given
            Map<String, String> registrationData = new HashMap<>();
            registrationData.put("username", "invalidemailuser");
            registrationData.put("email", "invalid-email"); // Invalid email
            registrationData.put("password", "ValidPassword123!");

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registrationData)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", containsString("email")));
        }

        @Test
        @DisplayName("Should reject registration with missing fields")
        void shouldRejectRegistrationWithMissingFields() throws Exception {
            // Given
            Map<String, String> incompleteData = new HashMap<>();
            incompleteData.put("username", "incompleteuser");
            // Missing email and password

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(incompleteData)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("User Login Tests")
    class UserLoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
            // Given
            Map<String, String> loginData = new HashMap<>();
            loginData.put("email", testUser.getEmail());
            loginData.put("password", testUserPassword);

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginData)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", notNullValue()))
                    .andExpect(jsonPath("$.refreshToken", notNullValue()))
                    .andExpect(jsonPath("$.user.email", is(testUser.getEmail())))
                    .andExpect(jsonPath("$.user.username", is(testUser.getUsername())));
        }

        @Test
        @DisplayName("Should reject login with invalid credentials")
        void shouldRejectLoginWithInvalidCredentials() throws Exception {
            // Given
            Map<String, String> loginData = new HashMap<>();
            loginData.put("email", testUser.getEmail());
            loginData.put("password", "WrongPassword123!");

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginData)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error", containsString("Invalid")));
        }

        @Test
        @DisplayName("Should reject login with disabled user")
        void shouldRejectLoginWithDisabledUser() throws Exception {
            // Given
            testUser.setEnabled(false);
            userRepository.save(testUser);

            Map<String, String> loginData = new HashMap<>();
            loginData.put("email", testUser.getEmail());
            loginData.put("password", testUserPassword);

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginData)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error", containsString("disabled")));
        }

        @Test
        @DisplayName("Should require captcha after multiple failed attempts")
        void shouldRequireCaptchaAfterMultipleFailedAttempts() throws Exception {
            // Given - Simulate multiple failed attempts
            String clientIp = "127.0.0.1";
            for (int i = 0; i < 5; i++) {
                attemptService.recordAttempt(clientIp);
            }

            Map<String, String> loginData = new HashMap<>();
            loginData.put("email", testUser.getEmail());
            loginData.put("password", testUserPassword);

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginData))
                            .with(request -> {
                                request.setRemoteAddr(clientIp);
                                return request;
                            }))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", containsString("captcha")));
        }

        @Test
        @DisplayName("Should login with captcha after failed attempts")
        void shouldLoginWithCaptchaAfterFailedAttempts() throws Exception {
            // Given - Simulate multiple failed attempts
            String clientIp = "127.0.0.1";
            for (int i = 0; i < 5; i++) {
                attemptService.recordAttempt(clientIp);
            }

            // Create captcha
            Map<String, Object> captchaResponse = captchaService.createCaptcha();
            String captchaId = (String) captchaResponse.get("captchaId");
            String captchaAnswer = "test"; // Assuming we can set a known answer for testing

            Map<String, String> loginData = new HashMap<>();
            loginData.put("email", testUser.getEmail());
            loginData.put("password", testUserPassword);
            loginData.put("captchaId", captchaId);
            loginData.put("captchaAnswer", captchaAnswer);

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginData))
                            .with(request -> {
                                request.setRemoteAddr(clientIp);
                                return request;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", notNullValue()));
        }
    }

    @Nested
    @DisplayName("Token Management Tests")
    class TokenManagementTests {

        private String accessToken;
        private String refreshToken;

        @BeforeEach
        void setUpTokens() throws Exception {
            // Login to get tokens
            Map<String, String> loginData = new HashMap<>();
            loginData.put("email", testUser.getEmail());
            loginData.put("password", testUserPassword);

            String response = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginData)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            accessToken = (String) responseMap.get("accessToken");
            refreshToken = (String) responseMap.get("refreshToken");
        }

        @Test
        @DisplayName("Should refresh access token successfully")
        void shouldRefreshAccessTokenSuccessfully() throws Exception {
            // Given
            Map<String, String> refreshData = new HashMap<>();
            refreshData.put("refreshToken", refreshToken);

            // When & Then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshData)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", notNullValue()))
                    .andExpect(jsonPath("$.refreshToken", notNullValue()));
        }

        @Test
        @DisplayName("Should reject refresh with invalid token")
        void shouldRejectRefreshWithInvalidToken() throws Exception {
            // Given
            Map<String, String> refreshData = new HashMap<>();
            refreshData.put("refreshToken", "invalid-token");

            // When & Then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshData)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error", containsString("Invalid")));
        }

        @Test
        @DisplayName("Should logout successfully")
        void shouldLogoutSuccessfully() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", containsString("successfully")));
        }

        @Test
        @DisplayName("Should reject requests with blacklisted token after logout")
        void shouldRejectRequestsWithBlacklistedTokenAfterLogout() throws Exception {
            // Given - Logout first
            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            // When & Then - Try to use the token
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("User Profile Tests")
    class UserProfileTests {

        private String accessToken;

        @BeforeEach
        void setUpToken() throws Exception {
            // Login to get token
            Map<String, String> loginData = new HashMap<>();
            loginData.put("email", testUser.getEmail());
            loginData.put("password", testUserPassword);

            String response = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginData)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            accessToken = (String) responseMap.get("accessToken");
        }

        @Test
        @DisplayName("Should get user profile successfully")
        void shouldGetUserProfileSuccessfully() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email", is(testUser.getEmail())))
                    .andExpect(jsonPath("$.username", is(testUser.getUsername())))
                    .andExpect(jsonPath("$.role", is(testUser.getRole().toString())))
                    .andExpect(jsonPath("$.enabled", is(testUser.isEnabled())));
        }

        @Test
        @DisplayName("Should reject profile request without token")
        void shouldRejectProfileRequestWithoutToken() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should change password successfully")
        void shouldChangePasswordSuccessfully() throws Exception {
            // Given
            Map<String, String> passwordData = new HashMap<>();
            passwordData.put("currentPassword", testUserPassword);
            passwordData.put("newPassword", "NewPassword456!");

            // When & Then
            mockMvc.perform(put("/api/auth/change-password")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordData)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", containsString("successfully")));
        }

        @Test
        @DisplayName("Should reject password change with wrong current password")
        void shouldRejectPasswordChangeWithWrongCurrentPassword() throws Exception {
            // Given
            Map<String, String> passwordData = new HashMap<>();
            passwordData.put("currentPassword", "WrongPassword123!");
            passwordData.put("newPassword", "NewPassword456!");

            // When & Then
            mockMvc.perform(put("/api/auth/change-password")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(passwordData)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", containsString("current password")));
        }
    }

    @Nested
    @DisplayName("Admin Operations Tests")
    class AdminOperationsTests {

        private String adminToken;
        private User adminUser;

        @BeforeEach
        void setUpAdmin() throws Exception {
            // Create admin user
            adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setEmail("admin@example.com");
            adminUser.setPassword(passwordEncoder.encode("AdminPassword123!"));
            adminUser.setRole(UserRole.ADMIN);
            adminUser.setEnabled(true);
            adminUser = userRepository.save(adminUser);

            // Login as admin
            Map<String, String> loginData = new HashMap<>();
            loginData.put("email", adminUser.getEmail());
            loginData.put("password", "AdminPassword123!");

            String response = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginData)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            adminToken = (String) responseMap.get("accessToken");
        }

        @Test
        @DisplayName("Should get user statistics as admin")
        void shouldGetUserStatisticsAsAdmin() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/auth/statistics")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalUsers", greaterThan(0)))
                    .andExpect(jsonPath("$.enabledUsers", greaterThan(0)));
        }

        @Test
        @DisplayName("Should enable/disable user as admin")
        void shouldEnableDisableUserAsAdmin() throws Exception {
            // Given
            Map<String, Boolean> enableData = new HashMap<>();
            enableData.put("enabled", false);

            // When & Then
            mockMvc.perform(put("/api/auth/enable/" + testUser.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(enableData)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", containsString("successfully")));
        }

        @Test
        @DisplayName("Should change user role as admin")
        void shouldChangeUserRoleAsAdmin() throws Exception {
            // Given
            Map<String, String> roleData = new HashMap<>();
            roleData.put("role", "ADMIN");

            // When & Then
            mockMvc.perform(put("/api/auth/role/" + testUser.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(roleData)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", containsString("successfully")));
        }

        @Test
        @DisplayName("Should reject admin operations for regular user")
        void shouldRejectAdminOperationsForRegularUser() throws Exception {
            // Given - Login as regular user
            Map<String, String> loginData = new HashMap<>();
            loginData.put("email", testUser.getEmail());
            loginData.put("password", testUserPassword);

            String response = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginData)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            String userToken = (String) responseMap.get("accessToken");

            // When & Then
            mockMvc.perform(get("/api/auth/statistics")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Security and Edge Cases Tests")
    class SecurityEdgeCasesTests {

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        void shouldHandleMalformedJsonGracefully() throws Exception {
            // Given
            String malformedJson = "{invalid json}";

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle SQL injection attempts")
        void shouldHandleSqlInjectionAttempts() throws Exception {
            // Given
            Map<String, String> maliciousData = new HashMap<>();
            maliciousData.put("email", "'; DROP TABLE users; --");
            maliciousData.put("password", "password");

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(maliciousData)))
                    .andExpect(status().isUnauthorized()); // Should not cause server error
        }

        @Test
        @DisplayName("Should handle XSS attempts")
        void shouldHandleXssAttempts() throws Exception {
            // Given
            Map<String, String> xssData = new HashMap<>();
            xssData.put("username", "<script>alert('xss')</script>");
            xssData.put("email", "xss@example.com");
            xssData.put("password", "XssPassword123!");

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(xssData)))
                    .andExpect(status().isBadRequest()); // Should reject or sanitize
        }

        @Test
        @DisplayName("Should handle very long input strings")
        void shouldHandleVeryLongInputStrings() throws Exception {
            // Given
            StringBuilder longString = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                longString.append("a");
            }

            Map<String, String> longData = new HashMap<>();
            longData.put("username", longString.toString());
            longData.put("email", "long@example.com");
            longData.put("password", "LongPassword123!");

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(longData)))
                    .andExpect(status().isBadRequest()); // Should reject overly long input
        }

        @Test
        @DisplayName("Should handle concurrent login attempts")
        void shouldHandleConcurrentLoginAttempts() throws Exception {
            // Given
            Map<String, String> loginData = new HashMap<>();
            loginData.put("email", testUser.getEmail());
            loginData.put("password", testUserPassword);

            // When & Then - Multiple concurrent requests should not cause issues
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginData)))
                        .andExpect(status().isOk());
            }
        }
    }
}