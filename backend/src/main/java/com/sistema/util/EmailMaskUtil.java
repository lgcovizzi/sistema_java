package com.sistema.util;

/**
 * Utilitário para mascarar emails mostrando apenas o início e o fim.
 */
public class EmailMaskUtil {

    /**
     * Mascara um email mostrando apenas os primeiros e últimos caracteres.
     * 
     * Exemplos:
     * - "usuario@exemplo.com" -> "us****@ex******.com"
     * - "a@b.com" -> "a****@b******.com"
     * - "teste.email@dominio.com.br" -> "te****@do******.com.br"
     * 
     * @param email o email a ser mascarado
     * @return email mascarado
     */
    public static String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "****@******.***";
        }

        String trimmedEmail = email.trim();
        
        // Verificar se é um email válido básico
        if (!trimmedEmail.contains("@") || trimmedEmail.indexOf("@") == 0 || 
            trimmedEmail.indexOf("@") == trimmedEmail.length() - 1) {
            return "****@******.***";
        }

        String[] parts = trimmedEmail.split("@");
        if (parts.length != 2) {
            return "****@******.***";
        }

        String localPart = parts[0];
        String domainPart = parts[1];

        // Mascarar parte local (antes do @)
        String maskedLocal;
        if (localPart.length() <= 2) {
            maskedLocal = localPart.charAt(0) + "****";
        } else {
            maskedLocal = localPart.substring(0, 2) + "****";
        }

        // Mascarar parte do domínio
        String maskedDomain;
        if (domainPart.contains(".")) {
            String[] domainParts = domainPart.split("\\.");
            String mainDomain = domainParts[0];
            
            // Mascarar o domínio principal
            String maskedMainDomain;
            if (mainDomain.length() <= 2) {
                maskedMainDomain = mainDomain.charAt(0) + "******";
            } else {
                maskedMainDomain = mainDomain.substring(0, 2) + "******";
            }
            
            // Reconstruir com as extensões
            StringBuilder domainBuilder = new StringBuilder(maskedMainDomain);
            for (int i = 1; i < domainParts.length; i++) {
                domainBuilder.append(".").append(domainParts[i]);
            }
            maskedDomain = domainBuilder.toString();
        } else {
            // Domínio sem ponto (caso raro)
            if (domainPart.length() <= 2) {
                maskedDomain = domainPart.charAt(0) + "******";
            } else {
                maskedDomain = domainPart.substring(0, 2) + "******";
            }
        }

        return maskedLocal + "@" + maskedDomain;
    }

    /**
     * Verifica se um email é válido para mascaramento.
     * 
     * @param email o email a verificar
     * @return true se o email pode ser mascarado
     */
    public static boolean isValidForMasking(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String trimmedEmail = email.trim();
        return trimmedEmail.contains("@") && 
               trimmedEmail.indexOf("@") > 0 && 
               trimmedEmail.indexOf("@") < trimmedEmail.length() - 1 &&
               trimmedEmail.split("@").length == 2;
    }
}