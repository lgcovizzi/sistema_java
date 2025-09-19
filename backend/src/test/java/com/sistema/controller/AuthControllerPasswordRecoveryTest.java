package com.sistema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.service.AuthService;
import com.sistema.service.AttemptService;
import com.sistema.service.CaptchaService;
import com.sistema.service.EmailService;
import com.sistema.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class, properties = {
    "management.health.redis.enabled=false",
    "spring.data.redis.repositories.enabled=false"
})
@DisplayName("AuthController Password Recovery Tests")
class AuthControllerPasswordRecoveryTest {

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
    private EmailService emailService;

    private User testUser;
    private String testCpf;
    private String testEmail;

    @BeforeEach
    void setUp() {
        testCpf = "12345678901";
        testEmail = "test@example.com";
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail(testEmail);
        testUser.setCpf(testCpf);
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.USER);
        testUser.setEnabled(true);
        testUser.setEmailVerified(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("POST /api/auth/verify-cpf")
    class VerifyCpfEndpointTests {

        @Test
        @DisplayName("Should return masked email when CPF exists")
        void shouldReturnMaskedEmailWhenCpfExists() throws Exception {
            // Given
            when(authService.findByCpf(testCpf)).thenReturn(Optional.of(testUser));
            
            String requestBody = """
                {
                    "cpf": "%s"
                }
                """.formatted(testCpf);

            // When & Then
            mockMvc.perform(post("/api/auth/verify-cpf")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.maskedEmail").exists())
                    .andExpect(jsonPath("$.message").value("CPF encontrado. Confirme o email para prosseguir."));

            verify(authService).findByCpf(testCpf);
        }

        @Test
        @DisplayName("Should return error when CPF does not exist")
        void shouldReturnErrorWhenCpfDoesNotExist() throws Exception {
            // Given
            String nonExistentCpf = "99999999999";
            when(authService.findByCpf(nonExistentCpf)).thenReturn(Optional.empty());
            
            String requestBody = """
                {
                    "cpf": "%s"
                }
                """.formatted(nonExistentCpf);

            // When & Then
            mockMvc.perform(post("/api/auth/verify-cpf")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("CPF não encontrado."));

            verify(authService).findByCpf(nonExistentCpf);
        }

        @Test
        @DisplayName("Should return validation error for invalid CPF")
        void shouldReturnValidationErrorForInvalidCpf() throws Exception {
            // Given
            String invalidCpf = "123";
            
            String requestBody = """
                {
                    "cpf": "%s"
                }
                """.formatted(invalidCpf);

            // When & Then
            mockMvc.perform(post("/api/auth/verify-cpf")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return validation error for empty CPF")
        void shouldReturnValidationErrorForEmptyCpf() throws Exception {
            // Given
            String requestBody = """
                {
                    "cpf": ""
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/auth/verify-cpf")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/confirm-email")
    class ConfirmEmailEndpointTests {

        @Test
        @DisplayName("Should confirm email when CPF and email match with valid captcha")
        void shouldConfirmEmailWhenCpfAndEmailMatchWithValidCaptcha() throws Exception {
            // Given
            when(authService.findByCpf(testCpf)).thenReturn(Optional.of(testUser));
            when(captchaService.validateCaptcha("captchaId", "answer")).thenReturn(true);
            when(attemptService.isCaptchaRequiredForPasswordReset(anyString())).thenReturn(true);
            
            String requestBody = """
                {
                    "cpf": "%s",
                    "email": "%s",
                    "captchaId": "captchaId",
                    "captchaAnswer": "answer"
                }
                """.formatted(testCpf, testEmail);

            // When & Then
            mockMvc.perform(post("/api/auth/confirm-email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .header("X-Forwarded-For", "192.168.1.1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Email confirmado. Token de recuperação enviado."))
                    .andExpect(jsonPath("$.email").value(testEmail));

            verify(authService).findByCpf(testCpf);
            verify(captchaService).validateCaptcha("captchaId", "answer");
        }

        @Test
        @DisplayName("Should return error when email does not match")
        void shouldReturnErrorWhenEmailDoesNotMatch() throws Exception {
            // Given
            String wrongEmail = "wrong@example.com";
            when(authService.findByCpf(testCpf)).thenReturn(Optional.of(testUser));
            when(captchaService.validateCaptcha("captchaId", "answer")).thenReturn(true);
            when(attemptService.isCaptchaRequiredForPasswordReset(anyString())).thenReturn(true);
            
            String requestBody = """
                {
                    "cpf": "%s",
                    "email": "%s",
                    "captchaId": "captchaId",
                    "captchaAnswer": "answer"
                }
                """.formatted(testCpf, wrongEmail);

            // When & Then
            mockMvc.perform(post("/api/auth/confirm-email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .header("X-Forwarded-For", "192.168.1.1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Email não corresponde ao CPF informado."));

            verify(authService).findByCpf(testCpf);
        }

        @Test
        @DisplayName("Should return error when captcha is invalid")
        void shouldReturnErrorWhenCaptchaIsInvalid() throws Exception {
            // Given
            when(authService.findByCpf(testCpf)).thenReturn(Optional.of(testUser));
            when(captchaService.validateCaptcha("captchaId", "wrongAnswer")).thenReturn(false);
            when(attemptService.isCaptchaRequiredForPasswordReset(anyString())).thenReturn(true);
            
            String requestBody = """
                {
                    "cpf": "%s",
                    "email": "%s",
                    "captchaId": "captchaId",
                    "captchaAnswer": "wrongAnswer"
                }
                """.formatted(testCpf, testEmail);

            // When & Then
            mockMvc.perform(post("/api/auth/confirm-email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .header("X-Forwarded-For", "192.168.1.1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Captcha inválido."));

            verify(captchaService).validateCaptcha("captchaId", "wrongAnswer");
        }

        @Test
        @DisplayName("Should return error when CPF not found")
        void shouldReturnErrorWhenCpfNotFound() throws Exception {
            // Given
            String nonExistentCpf = "99999999999";
            when(authService.findByCpf(nonExistentCpf)).thenReturn(Optional.empty());
            when(attemptService.isCaptchaRequiredForPasswordReset(anyString())).thenReturn(true);
            
            String requestBody = """
                {
                    "cpf": "%s",
                    "email": "%s",
                    "captchaId": "captchaId",
                    "captchaAnswer": "answer"
                }
                """.formatted(nonExistentCpf, testEmail);

            // When & Then
            mockMvc.perform(post("/api/auth/confirm-email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .header("X-Forwarded-For", "192.168.1.1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("CPF não encontrado."));

            verify(authService).findByCpf(nonExistentCpf);
        }
    }

    @Nested
    @DisplayName("POST /api/auth/forgot-password-with-confirmation")
    class ForgotPasswordWithConfirmationEndpointTests {

        @Test
        @DisplayName("Should process password recovery when all validations pass")
        void shouldProcessPasswordRecoveryWhenAllValidationsPass() throws Exception {
            // Given
            when(authService.findByCpf(testCpf)).thenReturn(Optional.of(testUser));
            when(captchaService.validateCaptcha("captchaId", "answer")).thenReturn(true);
            when(attemptService.isCaptchaRequiredForPasswordReset(anyString())).thenReturn(true);
            // Mock para simular processamento de recuperação de senha
            when(authService.findByCpf(testCpf)).thenReturn(Optional.of(testUser));
            
            String requestBody = """
                {
                    "cpf": "%s",
                    "confirmedEmail": "%s",
                    "captchaId": "captchaId",
                    "captchaAnswer": "answer"
                }
                """.formatted(testCpf, testEmail);

            // When & Then
            mockMvc.perform(post("/api/auth/forgot-password-with-confirmation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .header("X-Forwarded-For", "192.168.1.1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Nova senha enviada por email."));

            verify(authService).findByCpf(testCpf);
            verify(captchaService).validateCaptcha("captchaId", "answer");
            verify(authService).findByCpf(testCpf);
        }

        @Test
        @DisplayName("Should return error when email not confirmed")
        void shouldReturnErrorWhenEmailNotConfirmed() throws Exception {
            // Given
            String wrongEmail = "wrong@example.com";
            when(authService.findByCpf(testCpf)).thenReturn(Optional.of(testUser));
            when(captchaService.validateCaptcha("captchaId", "answer")).thenReturn(true);
            when(attemptService.isCaptchaRequiredForPasswordReset(anyString())).thenReturn(true);
            
            String requestBody = """
                {
                    "cpf": "%s",
                    "confirmedEmail": "%s",
                    "captchaId": "captchaId",
                    "captchaAnswer": "answer"
                }
                """.formatted(testCpf, wrongEmail);

            // When & Then
            mockMvc.perform(post("/api/auth/forgot-password-with-confirmation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .header("X-Forwarded-For", "192.168.1.1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Email não foi confirmado previamente."));

            verify(authService).findByCpf(testCpf);
        }

        @Test
        @DisplayName("Should handle rate limiting")
        void shouldHandleRateLimiting() throws Exception {
            // Given
            when(attemptService.isCaptchaRequiredForPasswordReset(anyString())).thenReturn(false);
            
            String requestBody = """
                {
                    "cpf": "%s",
                    "confirmedEmail": "%s",
                    "captchaId": "captchaId",
                    "captchaAnswer": "answer"
                }
                """.formatted(testCpf, testEmail);

            // When & Then
            mockMvc.perform(post("/api/auth/forgot-password-with-confirmation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .header("X-Forwarded-For", "192.168.1.1"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Muitas tentativas. Tente novamente mais tarde."));

            verify(attemptService).isCaptchaRequiredForPasswordReset(anyString());
        }

        @Test
        @DisplayName("Should return validation error for missing fields")
        void shouldReturnValidationErrorForMissingFields() throws Exception {
            // Given
            String requestBody = """
                {
                    "cpf": "",
                    "confirmedEmail": "",
                    "captchaId": "",
                    "captchaAnswer": ""
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/auth/forgot-password-with-confirmation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should handle missing X-Forwarded-For header")
        void shouldHandleMissingXForwardedForHeader() throws Exception {
            // Given
            when(attemptService.isCaptchaRequiredForPasswordReset(anyString())).thenReturn(true);
            
            String requestBody = """
                {
                    "cpf": "%s"
                }
                """.formatted(testCpf);

            // When & Then
            mockMvc.perform(post("/api/auth/verify-cpf")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should handle malformed JSON")
        void shouldHandleMalformedJson() throws Exception {
            // Given
            String malformedJson = "{ invalid json }";

            // When & Then
            mockMvc.perform(post("/api/auth/verify-cpf")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(malformedJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle missing Content-Type header")
        void shouldHandleMissingContentTypeHeader() throws Exception {
            // Given
            String requestBody = """
                {
                    "cpf": "%s"
                }
                """.formatted(testCpf);

            // When & Then
            mockMvc.perform(post("/api/auth/verify-cpf")
                    .content(requestBody))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }
}