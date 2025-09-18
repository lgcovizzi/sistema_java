package com.sistema.exception;

import com.sistema.util.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorCodes Tests")
class ErrorCodesTest {

    @Test
    @DisplayName("Deve ter todas as constantes de erro de validação definidas")
    void shouldHaveAllValidationErrorCodes() {
        // Then
        assertNotNull(ErrorCodes.VALIDATION_ERROR);
        assertNotNull(ErrorCodes.INVALID_REQUEST);
        assertNotNull(ErrorCodes.FIELD_REQUIRED);
        assertNotNull(ErrorCodes.INVALID_EMAIL_FORMAT);
        assertNotNull(ErrorCodes.INVALID_CPF_FORMAT);
        assertNotNull(ErrorCodes.FIELD_TOO_LONG);
        assertNotNull(ErrorCodes.FIELD_TOO_SHORT);
        
        assertEquals("VALIDATION_ERROR", ErrorCodes.VALIDATION_ERROR);
        assertEquals("INVALID_REQUEST", ErrorCodes.INVALID_REQUEST);
        assertEquals("FIELD_REQUIRED", ErrorCodes.FIELD_REQUIRED);
        assertEquals("INVALID_EMAIL_FORMAT", ErrorCodes.INVALID_EMAIL_FORMAT);
        assertEquals("INVALID_CPF_FORMAT", ErrorCodes.INVALID_CPF_FORMAT);
        assertEquals("FIELD_TOO_LONG", ErrorCodes.FIELD_TOO_LONG);
        assertEquals("FIELD_TOO_SHORT", ErrorCodes.FIELD_TOO_SHORT);
    }

    @Test
    @DisplayName("Deve ter todas as constantes de erro de autenticação definidas")
    void shouldHaveAllAuthenticationErrorCodes() {
        // Then
        assertNotNull(ErrorCodes.AUTHENTICATION_ERROR);
        assertNotNull(ErrorCodes.INVALID_CREDENTIALS);
        assertNotNull(ErrorCodes.ACCOUNT_LOCKED);
        assertNotNull(ErrorCodes.ACCOUNT_DISABLED);
        assertNotNull(ErrorCodes.TOKEN_EXPIRED);
        assertNotNull(ErrorCodes.INVALID_TOKEN);
        assertNotNull(ErrorCodes.ACCESS_DENIED);
        assertNotNull(ErrorCodes.EMAIL_NOT_VERIFIED);
        
        assertEquals("AUTHENTICATION_ERROR", ErrorCodes.AUTHENTICATION_ERROR);
        assertEquals("INVALID_CREDENTIALS", ErrorCodes.INVALID_CREDENTIALS);
        assertEquals("ACCOUNT_LOCKED", ErrorCodes.ACCOUNT_LOCKED);
        assertEquals("ACCOUNT_DISABLED", ErrorCodes.ACCOUNT_DISABLED);
        assertEquals("TOKEN_EXPIRED", ErrorCodes.TOKEN_EXPIRED);
        assertEquals("INVALID_TOKEN", ErrorCodes.INVALID_TOKEN);
        assertEquals("ACCESS_DENIED", ErrorCodes.ACCESS_DENIED);
        assertEquals("EMAIL_NOT_VERIFIED", ErrorCodes.EMAIL_NOT_VERIFIED);
    }

    @Test
    @DisplayName("Deve ter todas as constantes de erro de recurso definidas")
    void shouldHaveAllResourceErrorCodes() {
        // Then
        assertNotNull(ErrorCodes.RESOURCE_NOT_FOUND);
        assertNotNull(ErrorCodes.USER_NOT_FOUND);
        assertNotNull(ErrorCodes.USER_ALREADY_EXISTS);
        assertNotNull(ErrorCodes.EMAIL_ALREADY_EXISTS);
        assertNotNull(ErrorCodes.CPF_ALREADY_EXISTS);
        
        assertEquals("RESOURCE_NOT_FOUND", ErrorCodes.RESOURCE_NOT_FOUND);
        assertEquals("USER_NOT_FOUND", ErrorCodes.USER_NOT_FOUND);
        assertEquals("USER_ALREADY_EXISTS", ErrorCodes.USER_ALREADY_EXISTS);
        assertEquals("EMAIL_ALREADY_EXISTS", ErrorCodes.EMAIL_ALREADY_EXISTS);
        assertEquals("CPF_ALREADY_EXISTS", ErrorCodes.CPF_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("Deve ter todas as constantes de erro de negócio definidas")
    void shouldHaveAllBusinessErrorCodes() {
        // Then
        assertNotNull(ErrorCodes.VALIDATION_FAILED);
        assertNotNull(ErrorCodes.REGISTRATION_ERROR);
        assertNotNull(ErrorCodes.INVALID_REGISTRATION_DATA);
        assertNotNull(ErrorCodes.PROFILE_UPDATE_ERROR);
        
        assertEquals("VALIDATION_FAILED", ErrorCodes.VALIDATION_FAILED);
        assertEquals("REGISTRATION_ERROR", ErrorCodes.REGISTRATION_ERROR);
        assertEquals("INVALID_REGISTRATION_DATA", ErrorCodes.INVALID_REGISTRATION_DATA);
        assertEquals("PROFILE_UPDATE_ERROR", ErrorCodes.PROFILE_UPDATE_ERROR);
    }

    @Test
    @DisplayName("Deve ter todas as constantes de erro de sistema definidas")
    void shouldHaveAllSystemErrorCodes() {
        // Then
        assertNotNull(ErrorCodes.INTERNAL_ERROR);
        assertNotNull(ErrorCodes.DATABASE_ERROR);
        assertNotNull(ErrorCodes.REDIS_ERROR);
        assertNotNull(ErrorCodes.CONFIGURATION_ERROR);
        assertNotNull(ErrorCodes.SMTP_ERROR);
        
        assertEquals("INTERNAL_ERROR", ErrorCodes.INTERNAL_ERROR);
        assertEquals("DATABASE_ERROR", ErrorCodes.DATABASE_ERROR);
        assertEquals("REDIS_ERROR", ErrorCodes.REDIS_ERROR);
        assertEquals("CONFIGURATION_ERROR", ErrorCodes.CONFIGURATION_ERROR);
        assertEquals("SMTP_ERROR", ErrorCodes.SMTP_ERROR);
    }

    @Test
    @DisplayName("Deve ter todas as constantes de erro de rate limiting definidas")
    void shouldHaveAllRateLimitErrorCodes() {
        // Then
        assertNotNull(ErrorCodes.RATE_LIMITED);
        assertNotNull(ErrorCodes.LOGIN_RATE_LIMITED);
        assertNotNull(ErrorCodes.PASSWORD_RESET_RATE_LIMITED);
        
        assertEquals("RATE_LIMITED", ErrorCodes.RATE_LIMITED);
        assertEquals("LOGIN_RATE_LIMITED", ErrorCodes.LOGIN_RATE_LIMITED);
        assertEquals("PASSWORD_RESET_RATE_LIMITED", ErrorCodes.PASSWORD_RESET_RATE_LIMITED);
    }

    @Test
    @DisplayName("Deve ter todas as constantes de erro de email definidas")
    void shouldHaveAllEmailErrorCodes() {
        // Then
        assertNotNull(ErrorCodes.EMAIL_NOT_VERIFIED);
        assertNotNull(ErrorCodes.EMAIL_SEND_ERROR);
        assertNotNull(ErrorCodes.EMAIL_VERIFICATION_ERROR);
        assertNotNull(ErrorCodes.VERIFICATION_TOKEN_INVALID);
        assertNotNull(ErrorCodes.VERIFICATION_TOKEN_EXPIRED);
        
        assertEquals("EMAIL_NOT_VERIFIED", ErrorCodes.EMAIL_NOT_VERIFIED);
        assertEquals("EMAIL_SEND_ERROR", ErrorCodes.EMAIL_SEND_ERROR);
        assertEquals("EMAIL_VERIFICATION_ERROR", ErrorCodes.EMAIL_VERIFICATION_ERROR);
        assertEquals("VERIFICATION_TOKEN_INVALID", ErrorCodes.VERIFICATION_TOKEN_INVALID);
        assertEquals("VERIFICATION_TOKEN_EXPIRED", ErrorCodes.VERIFICATION_TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("Deve ter todas as constantes de erro de captcha definidas")
    void shouldHaveAllCaptchaErrorCodes() {
        // Then
        assertNotNull(ErrorCodes.CAPTCHA_REQUIRED);
        assertNotNull(ErrorCodes.INVALID_CAPTCHA);
        assertNotNull(ErrorCodes.CAPTCHA_EXPIRED);
        
        assertEquals("CAPTCHA_REQUIRED", ErrorCodes.CAPTCHA_REQUIRED);
        assertEquals("INVALID_CAPTCHA", ErrorCodes.INVALID_CAPTCHA);
        assertEquals("CAPTCHA_EXPIRED", ErrorCodes.CAPTCHA_EXPIRED);
    }

    @Test
    @DisplayName("Todas as constantes devem ser public static final String")
    void shouldHaveAllConstantsAsPublicStaticFinalString() throws IllegalAccessException {
        // Given
        Class<ErrorCodes> clazz = ErrorCodes.class;
        Field[] fields = clazz.getDeclaredFields();

        // When & Then
        for (Field field : fields) {
            // Verificar modificadores
            assertTrue(Modifier.isPublic(field.getModifiers()), 
                "Campo " + field.getName() + " deve ser public");
            assertTrue(Modifier.isStatic(field.getModifiers()), 
                "Campo " + field.getName() + " deve ser static");
            assertTrue(Modifier.isFinal(field.getModifiers()), 
                "Campo " + field.getName() + " deve ser final");
            
            // Verificar tipo
            assertEquals(String.class, field.getType(), 
                "Campo " + field.getName() + " deve ser do tipo String");
            
            // Verificar valor não nulo
            String value = (String) field.get(null);
            assertNotNull(value, "Campo " + field.getName() + " não deve ser null");
            assertFalse(value.trim().isEmpty(), "Campo " + field.getName() + " não deve ser vazio");
        }
    }

    @Test
    @DisplayName("Todos os códigos de erro devem ser únicos")
    void shouldHaveUniqueErrorCodes() throws IllegalAccessException {
        // Given
        Class<ErrorCodes> clazz = ErrorCodes.class;
        Field[] fields = clazz.getDeclaredFields();
        Set<String> errorCodes = new HashSet<>();

        // When
        for (Field field : fields) {
            String value = (String) field.get(null);
            
            // Then
            assertFalse(errorCodes.contains(value), 
                "Código de erro duplicado encontrado: " + value);
            errorCodes.add(value);
        }
    }

    @Test
    @DisplayName("Códigos de erro devem seguir padrão de nomenclatura")
    void shouldFollowNamingConvention() throws IllegalAccessException {
        // Given
        Class<ErrorCodes> clazz = ErrorCodes.class;
        Field[] fields = clazz.getDeclaredFields();

        // When & Then
        for (Field field : fields) {
            String fieldName = field.getName();
            String value = (String) field.get(null);
            
            // Nome do campo deve estar em UPPER_CASE
            assertEquals(fieldName.toUpperCase(), fieldName, 
                "Nome do campo deve estar em UPPER_CASE: " + fieldName);
            
            // Valor deve estar em UPPER_CASE
            assertEquals(value.toUpperCase(), value, 
                "Valor do código de erro deve estar em UPPER_CASE: " + value);
            
            // Valor deve conter apenas letras, números e underscore
            assertTrue(value.matches("^[A-Z0-9_]+$"), 
                "Código de erro deve conter apenas letras maiúsculas, números e underscore: " + value);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "VALIDATION_ERROR", "INVALID_REQUEST", "FIELD_REQUIRED", "INVALID_EMAIL_FORMAT",
        "AUTHENTICATION_ERROR", "INVALID_CREDENTIALS", "TOKEN_EXPIRED",
        "RESOURCE_NOT_FOUND", "USER_NOT_FOUND", "EMAIL_ALREADY_EXISTS",
        "VALIDATION_FAILED", "REGISTRATION_ERROR",
        "INTERNAL_ERROR", "DATABASE_ERROR",
        "RATE_LIMITED", "LOGIN_RATE_LIMITED",
        "EMAIL_NOT_VERIFIED", "CAPTCHA_REQUIRED"
    })
    @DisplayName("Deve ter códigos de erro essenciais definidos")
    void shouldHaveEssentialErrorCodesDefined(String expectedCode) throws IllegalAccessException {
        // Given
        Class<ErrorCodes> clazz = ErrorCodes.class;
        Field[] fields = clazz.getDeclaredFields();
        
        // When
        boolean found = false;
        for (Field field : fields) {
            String value = (String) field.get(null);
            if (expectedCode.equals(value)) {
                found = true;
                break;
            }
        }
        
        // Then
        assertTrue(found, "Código de erro essencial não encontrado: " + expectedCode);
    }

    @Test
    @DisplayName("Deve ter número mínimo de códigos de erro definidos")
    void shouldHaveMinimumNumberOfErrorCodes() {
        // Given
        Class<ErrorCodes> clazz = ErrorCodes.class;
        Field[] fields = clazz.getDeclaredFields();

        // Then
        assertTrue(fields.length >= 25, 
            "Deve ter pelo menos 25 códigos de erro definidos. Atual: " + fields.length);
    }

    @Test
    @DisplayName("Códigos de erro devem ter comprimento adequado")
    void shouldHaveAppropriateLength() throws IllegalAccessException {
        // Given
        Class<ErrorCodes> clazz = ErrorCodes.class;
        Field[] fields = clazz.getDeclaredFields();

        // When & Then
        for (Field field : fields) {
            String value = (String) field.get(null);
            
            // Comprimento mínimo e máximo
            assertTrue(value.length() >= 5, 
                "Código de erro muito curto: " + value + " (mínimo 5 caracteres)");
            assertTrue(value.length() <= 50, 
                "Código de erro muito longo: " + value + " (máximo 50 caracteres)");
        }
    }

    @Test
    @DisplayName("Deve agrupar códigos por categoria")
    void shouldGroupCodesByCategory() throws IllegalAccessException {
        // Given
        Class<ErrorCodes> clazz = ErrorCodes.class;
        Field[] fields = clazz.getDeclaredFields();
        
        int validationCodes = 0;
        int authCodes = 0;
        int resourceCodes = 0;
        int businessCodes = 0;
        int systemCodes = 0;
        int rateLimitCodes = 0;
        int emailCodes = 0;
        int captchaCodes = 0;

        // When
        for (Field field : fields) {
            String value = (String) field.get(null);
            
            if (value.contains("VALIDATION") || value.contains("INVALID") || 
                value.contains("REQUIRED") || value.contains("FORMAT") || 
                value.contains("LENGTH") || value.contains("RANGE")) {
                validationCodes++;
            } else if (value.contains("AUTHENTICATION") || value.contains("CREDENTIALS") || 
                      value.contains("TOKEN") || value.contains("ACCESS") || 
                      value.contains("ACCOUNT") || value.contains("PERMISSION")) {
                authCodes++;
            } else if (value.contains("RESOURCE") || value.contains("USER") || 
                      value.contains("NOT_FOUND") || value.contains("DUPLICATE")) {
                resourceCodes++;
            } else if (value.contains("BUSINESS") || value.contains("OPERATION") || 
                      value.contains("STATE") || value.contains("CONSTRAINT")) {
                businessCodes++;
            } else if (value.contains("INTERNAL") || value.contains("DATABASE") || 
                      value.contains("EXTERNAL") || value.contains("CONFIGURATION") || 
                      value.contains("TIMEOUT")) {
                systemCodes++;
            } else if (value.contains("RATE") || value.contains("REQUESTS") || 
                      value.contains("QUOTA")) {
                rateLimitCodes++;
            } else if (value.contains("EMAIL") || value.contains("VERIFICATION")) {
                emailCodes++;
            } else if (value.contains("CAPTCHA")) {
                captchaCodes++;
            }
        }

        // Then
        assertTrue(validationCodes >= 3, "Deve ter pelo menos 3 códigos de validação");
        assertTrue(authCodes >= 3, "Deve ter pelo menos 3 códigos de autenticação");
        assertTrue(resourceCodes >= 3, "Deve ter pelo menos 3 códigos de recurso");
        assertTrue(businessCodes >= 2, "Deve ter pelo menos 2 códigos de negócio");
        assertTrue(systemCodes >= 3, "Deve ter pelo menos 3 códigos de sistema");
        assertTrue(rateLimitCodes >= 2, "Deve ter pelo menos 2 códigos de rate limit");
        assertTrue(emailCodes >= 3, "Deve ter pelo menos 3 códigos de email");
        assertTrue(captchaCodes >= 3, "Deve ter pelo menos 3 códigos de captcha");
    }

    @Test
    @DisplayName("Classe ErrorCodes deve ser final")
    void shouldBeFinalClass() {
        // Given
        Class<ErrorCodes> clazz = ErrorCodes.class;

        // Then
        assertTrue(Modifier.isFinal(clazz.getModifiers()), 
            "Classe ErrorCodes deve ser final");
    }

    @Test
    @DisplayName("Classe ErrorCodes não deve ter construtor público")
    void shouldNotHavePublicConstructor() {
        // Given
        Class<ErrorCodes> clazz = ErrorCodes.class;

        // Then
        assertEquals(0, clazz.getConstructors().length, 
            "Classe ErrorCodes não deve ter construtores públicos");
    }
}