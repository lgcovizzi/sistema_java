package com.sistema.debug;

import com.sistema.util.ValidationUtils;
import org.junit.jupiter.api.Test;
import java.util.regex.Pattern;

public class EmailValidationDebugTest {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9+]([a-zA-Z0-9._+%-]*[a-zA-Z0-9])?@[a-zA-Z0-9]([a-zA-Z0-9.-]*[a-zA-Z0-9])?\\.[a-zA-Z]{2,}$"
    );

    @Test
    void testEmailValidation() {
        String testEmail = "testuser" + System.currentTimeMillis() + "@example.com";
        System.out.println("Email gerado: " + testEmail);
        
        // Testar pontos consecutivos
        System.out.println("Contém pontos consecutivos: " + testEmail.contains(".."));
        
        // Testar padrão regex
        boolean matchesPattern = EMAIL_PATTERN.matcher(testEmail).matches();
        System.out.println("Matches pattern: " + matchesPattern);
        
        // Testar cada parte do email
        String[] parts = testEmail.split("@");
        if (parts.length == 2) {
            System.out.println("Parte local: " + parts[0]);
            System.out.println("Parte domínio: " + parts[1]);
            
            // Testar se a parte local está ok
            String localPart = parts[0];
            System.out.println("Local part length: " + localPart.length());
            System.out.println("Local part first char: " + localPart.charAt(0));
            System.out.println("Local part last char: " + localPart.charAt(localPart.length() - 1));
        }
        
        try {
            ValidationUtils.validateEmail(testEmail);
            System.out.println("Email válido!");
        } catch (Exception e) {
            System.out.println("Email inválido: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Testar também com isValidEmail
        boolean isValid = ValidationUtils.isValidEmail(testEmail);
        System.out.println("isValidEmail retornou: " + isValid);
    }
}