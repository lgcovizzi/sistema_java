package com.sistema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistema.repository.UserRepository;
import com.sistema.util.CpfGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Testes de integração para validação de senhas no AuthController.
 */
@SpringBootTest(properties = {
    "management.health.redis.enabled=false",
    "spring.data.redis.repositories.enabled=false"
})
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController - Password Validation Integration Tests")
class AuthControllerPasswordValidationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        
        objectMapper = new ObjectMapper();
        
        // Limpar dados de teste
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve registrar usuário com senha válida")
    void shouldRegisterUserWithValidPassword() throws Exception {
        // Given
        Map<String, String> registrationData = createValidRegistrationData();
        registrationData.put("password", "MinhaSenh@123"); // Senha válida
        
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationData)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Senha@123",      // 9 caracteres
        "P@ssw0rd",       // 9 caracteres
        "Teste123!",      // 10 caracteres
        "AbC123@def",     // 10 caracteres
        "MinhaSenh@123"   // 14 caracteres
    })
    @DisplayName("Deve aceitar senhas válidas com diferentes tamanhos")
    void shouldAcceptValidPasswordsWithDifferentLengths(String password) throws Exception {
        // Given
        Map<String, String> registrationData = createValidRegistrationData();
        registrationData.put("email", "test" + System.currentTimeMillis() + "@example.com"); // Email único
        registrationData.put("cpf", generateUniqueCpf()); // CPF único
        registrationData.put("password", password);
        
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationData)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",               // Vazia
        "   ",            // Apenas espaços
        "123456",         // 6 caracteres
        "1234567",        // 7 caracteres
        "Abc123@"         // 7 caracteres
    })
    @DisplayName("Deve rejeitar senhas com menos de 8 caracteres")
    void shouldRejectPasswordsWithLessThan8Characters(String password) throws Exception {
        // Given
        Map<String, String> registrationData = createValidRegistrationData();
        registrationData.put("password", password);
        
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationData)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.error").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "MINHASENHA123@", // Sem minúscula
        "minhasenha123@", // Sem maiúscula
        "MinhaSenh@",     // Sem número
        "MinhaSenh123"    // Sem caractere especial
    })
    @DisplayName("Deve rejeitar senhas que não atendem critérios de complexidade")
    void shouldRejectPasswordsNotMeetingComplexityRequirements(String password) throws Exception {
        // Given
        Map<String, String> registrationData = createValidRegistrationData();
        registrationData.put("password", password);
        
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationData)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Senha deve conter pelo menos")))
                .andExpect(jsonPath("$.error").value("REGISTRATION_ERROR"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Teste123!",      // Com !
        "Teste123@",      // Com @
        "Teste123#",      // Com #
        "Teste123$",      // Com $
        "Teste123%",      // Com %
        "Teste123^",      // Com ^
        "Teste123&",      // Com &
        "Teste123*",      // Com *
        "Teste123_",      // Com _
        "Teste123+",      // Com +
        "Teste123-",      // Com -
        "Teste123=",      // Com =
        "Teste123?",      // Com ?
        "Teste123/"       // Com /
    })
    @DisplayName("Deve aceitar senhas com diferentes caracteres especiais")
    void shouldAcceptPasswordsWithDifferentSpecialCharacters(String password) throws Exception {
        // Given
        Map<String, String> registrationData = createValidRegistrationData();
        registrationData.put("email", "test" + System.currentTimeMillis() + "@example.com"); // Email único
        registrationData.put("cpf", generateUniqueCpf()); // CPF único
        registrationData.put("password", password);
        
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationData)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    @DisplayName("Deve rejeitar senha nula")
    void shouldRejectNullPassword() throws Exception {
        // Given
        Map<String, String> registrationData = createValidRegistrationData();
        registrationData.put("password", null);
        
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationData)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve mostrar mensagem específica para senha inválida")
    void shouldShowSpecificMessageForInvalidPassword() throws Exception {
        // Given
        Map<String, String> registrationData = createValidRegistrationData();
        registrationData.put("password", "senhafraca"); // Sem maiúscula, número e caractere especial
        
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationData)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Senha deve conter pelo menos: 1 letra minúscula, 1 maiúscula, 1 número e 1 caractere especial")))
                .andExpect(jsonPath("$.error").value("REGISTRATION_ERROR"));
    }

    @Test
    @DisplayName("Deve aceitar senha com múltiplos critérios de cada tipo")
    void shouldAcceptPasswordWithMultipleCriteriaOfEachType() throws Exception {
        // Given
        Map<String, String> registrationData = createValidRegistrationData();
        registrationData.put("password", "AbCdEf123456!@#$"); // Múltiplos de cada critério
        
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationData)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    @DisplayName("Deve aceitar senha longa com todos os critérios")
    void shouldAcceptLongPasswordWithAllCriteria() throws Exception {
        // Given
        Map<String, String> registrationData = createValidRegistrationData();
        registrationData.put("password", "EstaSenhaEhMuitoLongaETemTodosOsCriterios123!@#");
        
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationData)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    private Map<String, String> createValidRegistrationData() {
        Map<String, String> data = new HashMap<>();
        data.put("email", "test@example.com");
        data.put("firstName", "Test");
        data.put("lastName", "User");
        data.put("cpf", "12345678901");
        return data;
    }

    private String generateUniqueCpf() {
        // Gera um CPF válido único usando o CpfGenerator
        return CpfGenerator.generateCpf(); // Retorna CPF sem formatação
    }
}