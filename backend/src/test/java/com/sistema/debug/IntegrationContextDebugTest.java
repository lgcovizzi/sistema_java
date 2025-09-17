package com.sistema.debug;

import com.sistema.service.AuthService;
import com.sistema.util.CpfGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class IntegrationContextDebugTest {

    @Autowired
    private AuthService authService;

    private String testEmail;
    private String testCpf;

    @BeforeEach
    void setUp() {
        testEmail = "testuser" + System.currentTimeMillis() + "@example.com";
        testCpf = CpfGenerator.generateCpf();
        
        System.out.println("=== SETUP DEBUG ===");
        System.out.println("Email gerado: " + testEmail);
        System.out.println("CPF gerado: " + testCpf);
        System.out.println("Email length: " + testEmail.length());
        System.out.println("Email contains ..: " + testEmail.contains(".."));
        
        // Verificar cada caractere do email
        System.out.println("Caracteres do email:");
        for (int i = 0; i < testEmail.length(); i++) {
            char c = testEmail.charAt(i);
            System.out.println("  [" + i + "] = '" + c + "' (ASCII: " + (int)c + ")");
        }
    }

    @Test
    void testEmailValidationInSpringContext() {
        System.out.println("=== TESTE NO CONTEXTO SPRING ===");
        
        try {
            // Usar o método direto do AuthService que aceita parâmetros individuais
            authService.register(testEmail, "MinhaSenh@123", "João", "Silva", testCpf);
            System.out.println("✅ Registro bem-sucedido!");
        } catch (Exception e) {
            System.out.println("❌ Erro no registro: " + e.getMessage());
            System.out.println("Tipo da exceção: " + e.getClass().getSimpleName());
            e.printStackTrace();
        }
    }
}