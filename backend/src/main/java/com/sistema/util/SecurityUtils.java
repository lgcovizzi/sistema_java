package com.sistema.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Utilitários para operações de segurança.
 * Fornece métodos estáticos para operações comuns de segurança em toda a aplicação.
 */
public final class SecurityUtils {
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final Pattern SAFE_STRING_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    
    private SecurityUtils() {
        // Classe utilitária - construtor privado
    }
    
    /**
     * Gera um token aleatório seguro.
     * 
     * @param length comprimento do token em bytes
     * @return token em base64 URL-safe
     */
    public static String generateSecureToken(int length) {
        ValidationUtils.validatePositive(length, "length");
        
        byte[] randomBytes = new byte[length];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    /**
     * Gera um token aleatório de 32 bytes.
     * 
     * @return token em base64 URL-safe
     */
    public static String generateSecureToken() {
        return generateSecureToken(32);
    }
    
    /**
     * Gera um salt aleatório para hash de senhas.
     * 
     * @return salt em base64
     */
    public static String generateSalt() {
        return generateSecureToken(16);
    }
    
    /**
     * Gera hash SHA-256 de uma string.
     * 
     * @param input string a fazer hash
     * @return hash em hexadecimal
     */
    public static String generateHash(String input) {
        ValidationUtils.validateNotEmpty(input, "input");
        
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo de hash não disponível: " + HASH_ALGORITHM, e);
        }
    }
    
    /**
     * Gera um hash SHA-256 da entrada fornecida.
     * Alias para generateHash para compatibilidade.
     * 
     * @param input string de entrada
     * @return hash SHA-256 em hexadecimal
     */
    public static String hashSHA256(String input) {
        return generateHash(input);
    }
    
    /**
     * Gera hash SHA-256 de uma string com salt.
     * 
     * @param input string a fazer hash
     * @param salt salt para o hash
     * @return hash em hexadecimal
     */
    public static String generateHash(String input, String salt) {
        ValidationUtils.validateNotEmpty(input, "input");
        ValidationUtils.validateNotEmpty(salt, "salt");
        
        return generateHash(salt + input);
    }
    
    /**
     * Verifica se uma string corresponde a um hash.
     * 
     * @param input string original
     * @param expectedHash hash esperado
     * @return true se corresponde
     */
    public static boolean verifyHash(String input, String expectedHash) {
        try {
            String actualHash = generateHash(input);
            return constantTimeEquals(actualHash, expectedHash);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Verifica se uma string corresponde a um hash com salt.
     * 
     * @param input string original
     * @param salt salt usado no hash
     * @param expectedHash hash esperado
     * @return true se corresponde
     */
    public static boolean verifyHash(String input, String salt, String expectedHash) {
        try {
            String actualHash = generateHash(input, salt);
            return constantTimeEquals(actualHash, expectedHash);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Compara duas strings em tempo constante para evitar timing attacks.
     * 
     * @param a primeira string
     * @param b segunda string
     * @return true se são iguais
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }
    
    /**
     * Sanitiza uma string removendo caracteres perigosos.
     * 
     * @param input string a sanitizar
     * @return string sanitizada
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        return input.trim()
                   .replaceAll("[<>\"'&]", "") // Remove caracteres HTML perigosos
                   .replaceAll("\\s+", " ")    // Normaliza espaços
                   .replaceAll("[\\x00-\\x1F\\x7F]", ""); // Remove caracteres de controle
    }
    
    /**
     * Verifica se uma string contém apenas caracteres seguros.
     * 
     * @param input string a verificar
     * @return true se é segura
     */
    public static boolean isSafeString(String input) {
        return input != null && SAFE_STRING_PATTERN.matcher(input).matches();
    }
    
    /**
     * Escapa caracteres especiais para uso em HTML.
     * 
     * @param input string a escapar
     * @return string escapada
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
    
    /**
     * Escapa caracteres especiais para uso em SQL.
     * 
     * @param input string a escapar
     * @return string escapada
     */
    public static String escapeSql(String input) {
        if (input == null) {
            return null;
        }
        
        return input.replace("'", "''")
                   .replace("\\", "\\\\")
                   .replace("\0", "\\0")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    /**
     * Mascara dados sensíveis mostrando apenas alguns caracteres.
     * 
     * @param data dados a mascarar
     * @param visibleChars quantidade de caracteres visíveis no início e fim
     * @return dados mascarados
     */
    public static String maskSensitiveData(String data, int visibleChars) {
        if (data == null || data.length() <= visibleChars * 2) {
            return data;
        }
        
        String start = data.substring(0, visibleChars);
        String end = data.substring(data.length() - visibleChars);
        String middle = "*".repeat(data.length() - visibleChars * 2);
        
        return start + middle + end;
    }
    
    /**
     * Mascara dados sensíveis com padrão padrão (2 caracteres visíveis).
     * 
     * @param data dados a mascarar
     * @return dados mascarados
     */
    public static String maskSensitiveData(String data) {
        return maskSensitiveData(data, 2);
    }
    
    /**
     * Gera um ID único baseado em timestamp e randomness.
     * 
     * @return ID único
     */
    public static String generateUniqueId() {
        long timestamp = System.currentTimeMillis();
        String random = generateSecureToken(8);
        return timestamp + "_" + random;
    }
    
    /**
     * Valida se um token tem formato válido.
     * 
     * @param token token a validar
     * @return true se é válido
     */
    public static boolean isValidToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Verifica se é base64 válido
            Base64.getUrlDecoder().decode(token);
            // Verifica comprimento mínimo (16 bytes = ~22 caracteres em base64)
            return token.length() >= 22;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Converte array de bytes para string hexadecimal.
     * 
     * @param bytes array de bytes
     * @return string hexadecimal
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Gera um número aleatório seguro dentro de um intervalo.
     * 
     * @param min valor mínimo (inclusivo)
     * @param max valor máximo (exclusivo)
     * @return número aleatório
     */
    public static int generateSecureRandomInt(int min, int max) {
        ValidationUtils.validateRange(min, Integer.MIN_VALUE, max - 1, "min");
        ValidationUtils.validateRange(max, min + 1, Integer.MAX_VALUE, "max");
        
        return SECURE_RANDOM.nextInt(max - min) + min;
    }
    
    /**
     * Gera uma string aleatória com caracteres alfanuméricos.
     * 
     * @param length comprimento da string
     * @return string aleatória
     */
    public static String generateRandomString(int length) {
        ValidationUtils.validatePositive(length, "length");
        
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(chars.length());
            result.append(chars.charAt(index));
        }
        
        return result.toString();
    }
    
    /**
     * Verifica se um endereço IP é válido (IPv4).
     * 
     * @param ip endereço IP
     * @return true se é válido
     */
    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        
        Pattern ipPattern = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        );
        
        return ipPattern.matcher(ip).matches();
    }
}