package com.sistema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.service.AuthService;
import com.sistema.service.AttemptService;
import com.sistema.service.CaptchaService;
import com.sistema.service.PasswordResetService;
import com.sistema.util.CpfGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes unitários específicos para o endpoint /api/auth/forgot-password
 * Foco em prevenir erros 400 Bad Request através de validação abrangente
 */
@WebMvcTest(value = AuthController.class, properties = {
    "management.health.redis.enabled=false",
    "spring.data.redis.repositories.enabled=false"
})
@DisplayName("AuthController - Forgot Password Unit Tests")
class AuthControllerForgotPasswordUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private AttemptService attemptService;

    @MockBean
    private CaptchaService captchaService;

    @MockBean
    private PasswordResetService passwordResetService;

    private String validCpf;
    private String validCaptchaId;
    private String validCaptchaAnswer;
    private User testUser;

    @BeforeEach
    void setUp() {
        validCpf = CpfGenerator.generateCpf();
        validCaptchaId = "test-captcha-id";
        validCaptchaAnswer = "ABCD";
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setCpf(validCpf);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(UserRole.USER);
        testUser.setEnabled(true);
        testUser.setEmailVerified(true);
    }

    @Nested
    @DisplayName("Validação de Entrada - Casos que causam 400 Bad Request")
    class InputValidationTests {

        @Test
        @DisplayName("Deve retornar 400 quando CPF está vazio")
        void shouldReturn400WhenCpfIsEmpty() throws Exception {
            var request = createForgotPasswordRequest("", validCaptchaId, validCaptchaAnswer);

            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.errorCode").exists());
        }

        @Test
        @DisplayName("Deve retornar 400 quando CPF é null")
        void shouldReturn400WhenCpfIsNull() throws Exception {
            var request = createForgotPasswordRequest(null, validCaptchaId, validCaptchaAnswer);

            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar 400 quando CPF é inválido")
        void shouldReturn400WhenCpfIsInvalid() throws Exception {
            var request = createForgotPasswordRequest("12345678901", validCaptchaId, validCaptchaAnswer);

            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("CPF inválido")));
        }

        @Test
        @DisplayName("Deve retornar 400 quando captchaId está vazio")
        void shouldReturn400WhenCaptchaIdIsEmpty() throws Exception {
            var request = createForgotPasswordRequest(validCpf, "", validCaptchaAnswer);

            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("ID do captcha é obrigatório")));
        }

        @Test
        @DisplayName("Deve retornar 400 quando captchaId é null")
        void shouldReturn400WhenCaptchaIdIsNull() throws Exception {
            var request = createForgotPasswordRequest(validCpf, null, validCaptchaAnswer);

            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar 400 quando captchaAnswer está vazio")
        void shouldReturn400WhenCaptchaAnswerIsEmpty() throws Exception {
            var request = createForgotPasswordRequest(validCpf, validCaptchaId, "");

            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("Resposta do captcha é obrigatória")));
        }

        @Test
        @DisplayName("Deve retornar 400 quando captchaAnswer é null")
        void shouldReturn400WhenCaptchaAnswerIsNull() throws Exception {
            var request = createForgotPasswordRequest(validCpf, validCaptchaId, null);

            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar 400 quando todos os campos são inválidos")
        void shouldReturn400WhenAllFieldsAreInvalid() throws Exception {
            var request = createForgotPasswordRequest("", "", "");

            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar 400 quando body da requisição está vazio")
        void shouldReturn400WhenRequestBodyIsEmpty() throws Exception {
            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar 400 quando Content-Type não é JSON")
        void shouldReturn400WhenContentTypeIsNotJson() throws Exception {
            var request = createForgotPasswordRequest(validCpf, validCaptchaId, validCaptchaAnswer);

            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Validação de Captcha - Casos que causam 400 Bad Request")
    class CaptchaValidationTests {

        @Test
        @DisplayName("Deve retornar 400 quando captcha é inválido")
        void shouldReturn400WhenCaptchaIsInvalid() throws Exception {
            // Arrange
            when(attemptService.isPasswordResetRateLimited(anyString())).thenReturn(false);
            when(captchaService.validateCaptcha(validCaptchaId, validCaptchaAnswer)).thenReturn(false);

            var request = createForgotPasswordRequest(validCpf, validCaptchaId, validCaptchaAnswer);

            // Act & Assert
            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Captcha inválido"))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_CAPTCHA"))
                    .andExpect(jsonPath("$.requiresCaptcha").value(true));

            verify(attemptService).recordPasswordResetAttempt(anyString());
        }

        @Test
        @DisplayName("Deve retornar 400 quando captcha não existe")
        void shouldReturn400WhenCaptchaDoesNotExist() throws Exception {
            // Arrange
            when(attemptService.isPasswordResetRateLimited(anyString())).thenReturn(false);
            when(captchaService.validateCaptcha("invalid-captcha-id", validCaptchaAnswer)).thenReturn(false);

            var request = createForgotPasswordRequest(validCpf, "invalid-captcha-id", validCaptchaAnswer);

            // Act & Assert
            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_CAPTCHA"));
        }

        @Test
        @DisplayName("Deve retornar 400 quando resposta do captcha está incorreta")
        void shouldReturn400WhenCaptchaAnswerIsWrong() throws Exception {
            // Arrange
            when(attemptService.isPasswordResetRateLimited(anyString())).thenReturn(false);
            when(captchaService.validateCaptcha(validCaptchaId, "WRONG")).thenReturn(false);

            var request = createForgotPasswordRequest(validCpf, validCaptchaId, "WRONG");

            // Act & Assert
            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_CAPTCHA"));
        }
    }

    @Nested
    @DisplayName("Rate Limiting - Casos que causam 429 Too Many Requests")
    class RateLimitingTests {

        @Test
        @DisplayName("Deve retornar 429 quando rate limit é atingido")
        void shouldReturn429WhenRateLimitIsReached() throws Exception {
            // Arrange
            when(attemptService.isPasswordResetRateLimited(anyString())).thenReturn(true);
            when(attemptService.getPasswordResetRateLimitRemainingSeconds(anyString())).thenReturn(45L);

            var request = createForgotPasswordRequest(validCpf, validCaptchaId, validCaptchaAnswer);

            // Act & Assert
            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"))
                    .andExpect(jsonPath("$.remainingSeconds").value(45));
        }
    }

    @Nested
    @DisplayName("Casos de Sucesso - Validação de fluxo correto")
    class SuccessTests {

        @Test
        @DisplayName("Deve retornar 200 quando todos os dados são válidos e usuário existe")
        void shouldReturn200WhenAllDataIsValidAndUserExists() throws Exception {
            // Arrange
            when(attemptService.isPasswordResetRateLimited(anyString())).thenReturn(false);
            when(captchaService.validateCaptcha(validCaptchaId, validCaptchaAnswer)).thenReturn(true);
            when(authService.findByCpf(validCpf)).thenReturn(Optional.of(testUser));
            when(passwordResetService.initiatePasswordReset(testUser.getEmail())).thenReturn(true);

            var request = createForgotPasswordRequest(validCpf, validCaptchaId, validCaptchaAnswer);

            // Act & Assert
            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Instruções de recuperação foram enviadas para o email cadastrado"))
                    .andExpect(jsonPath("$.maskedEmail").exists());

            verify(attemptService).clearPasswordResetAttempts(anyString());
            verify(attemptService).recordPasswordResetSuccess(anyString());
        }

        @Test
        @DisplayName("Deve retornar 404 quando CPF não existe no sistema")
        void shouldReturn404WhenCpfDoesNotExist() throws Exception {
            // Arrange
            when(attemptService.isPasswordResetRateLimited(anyString())).thenReturn(false);
            when(captchaService.validateCaptcha(validCaptchaId, validCaptchaAnswer)).thenReturn(true);
            when(authService.findByCpf(validCpf)).thenReturn(Optional.empty());

            var request = createForgotPasswordRequest(validCpf, validCaptchaId, validCaptchaAnswer);

            // Act & Assert
            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("CPF_NOT_FOUND"))
                    .andExpect(jsonPath("$.requiresCaptcha").value(true));

            verify(attemptService).recordPasswordResetAttempt(anyString());
        }
    }

    @Nested
    @DisplayName("Casos de Erro Interno - Validação de tratamento de exceções")
    class InternalErrorTests {

        @Test
        @DisplayName("Deve retornar 500 quando falha ao enviar email")
        void shouldReturn500WhenEmailSendFails() throws Exception {
            // Arrange
            when(attemptService.isPasswordResetRateLimited(anyString())).thenReturn(false);
            when(captchaService.validateCaptcha(validCaptchaId, validCaptchaAnswer)).thenReturn(true);
            when(authService.findByCpf(validCpf)).thenReturn(Optional.of(testUser));
            when(passwordResetService.initiatePasswordReset(testUser.getEmail())).thenReturn(false);

            var request = createForgotPasswordRequest(validCpf, validCaptchaId, validCaptchaAnswer);

            // Act & Assert
            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value("EMAIL_SEND_FAILED"))
                    .andExpect(jsonPath("$.requiresCaptcha").value(true));

            verify(attemptService).recordPasswordResetAttempt(anyString());
        }

        @Test
        @DisplayName("Deve retornar 500 quando ocorre exceção inesperada")
        void shouldReturn500WhenUnexpectedExceptionOccurs() throws Exception {
            // Arrange
            when(attemptService.isPasswordResetRateLimited(anyString())).thenReturn(false);
            when(captchaService.validateCaptcha(validCaptchaId, validCaptchaAnswer)).thenReturn(true);
            when(authService.findByCpf(validCpf)).thenThrow(new RuntimeException("Database error"));

            var request = createForgotPasswordRequest(validCpf, validCaptchaId, validCaptchaAnswer);

            // Act & Assert
            mockMvc.perform(post("/api/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));

            verify(attemptService).recordPasswordResetAttempt(anyString());
        }
    }

    /**
     * Helper method para criar request de forgot password
     */
    private ForgotPasswordTestRequest createForgotPasswordRequest(String cpf, String captchaId, String captchaAnswer) {
        return new ForgotPasswordTestRequest(cpf, captchaId, captchaAnswer);
    }

    /**
     * Classe interna para representar o request de forgot password nos testes
     */
    private static class ForgotPasswordTestRequest {
        public final String cpf;
        public final String captchaId;
        public final String captchaAnswer;

        public ForgotPasswordTestRequest(String cpf, String captchaId, String captchaAnswer) {
            this.cpf = cpf;
            this.captchaId = captchaId;
            this.captchaAnswer = captchaAnswer;
        }
    }
}