package com.sistema.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

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
        ValidationUtils.validateNotNull(input, "input");
        
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
        
        String sanitized = input.trim()
                   .replaceAll("[<>\"'&]", "") // Remove caracteres HTML perigosos
                   .replaceAll("\\s+", " ")    // Normaliza espaços
                   .replaceAll("[\\x00-\\x1F\\x7F]", ""); // Remove caracteres de controle
        
        // Remove padrões SQL injection comuns (case insensitive)
        sanitized = sanitized.replaceAll("(?i)(DROP\\s+TABLE|DELETE\\s+FROM|INSERT\\s+INTO|UPDATE\\s+SET|UNION\\s+SELECT|SELECT\\s+\\*)", "");
        // Remove comentários SQL
        sanitized = sanitized.replaceAll("--.*", "");
        
        return sanitized.trim();
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
     * Converte uma string hexadecimal em array de bytes.
     * 
     * @param hex string hexadecimal
     * @return array de bytes
     */
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
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
     * Valida se um endereço IP é válido (IPv4 ou IPv6).
     * 
     * @param ip endereço IP para validar
     * @return true se o IP for válido
     */
    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        
        ip = ip.trim();
        
        // Validação de IPv4
        if (isValidIpv4(ip)) {
            return true;
        }
        
        // Validação de IPv6
        return isValidIpv6(ip);
    }
    
    private static boolean isValidIpv4(String ip) {
        // Padrão básico para IPv4
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        if (!ip.matches(ipv4Pattern)) {
            return false;
        }
        
        // Verificação adicional para evitar leading zeros inválidos
        String[] parts = ip.split("\\.");
        for (String part : parts) {
            // Não permitir leading zeros exceto para "0"
            if (part.length() > 1 && part.startsWith("0")) {
                return false;
            }
        }
        
        return true;
    }
    
    private static boolean isValidIpv6(String ip) {
        // Casos especiais
        if ("::".equals(ip) || "::1".equals(ip)) {
            return true;
        }
        
        // Formato completo: 8 grupos de 4 dígitos hexadecimais
        String fullPattern = "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$";
        
        // Formato com :: (compressão de zeros) - padrões mais específicos
        String[] compressedPatterns = {
            "^([0-9a-fA-F]{1,4}:){1,7}:$",                    // fe80::
            "^:([0-9a-fA-F]{1,4}:){1,7}[0-9a-fA-F]{1,4}$",   // ::1234:5678
            "^([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}$",   // 2001:db8::1
            "^([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}$", // 2001:db8::8a2e:370:7334
            "^([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}$",
            "^([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}$",
            "^([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}$",
            "^[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})$"
        };
        
        if (ip.matches(fullPattern)) {
            return true;
        }
        
        for (String pattern : compressedPatterns) {
            if (ip.matches(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Mascara um endereço de email.
     * 
     * @param email endereço de email para mascarar
     * @return email mascarado
     */
    public static String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return email;
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return maskSensitiveData(email, 2);
        }
        
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "*" + domain;
        }
        
        return localPart.charAt(0) + "*".repeat(localPart.length() - 2) + localPart.charAt(localPart.length() - 1) + domain;
    }
    
    /**
     * Mascara um número de cartão de crédito.
     * 
     * @param creditCard número do cartão de crédito
     * @return cartão mascarado
     */
    public static String maskCreditCard(String creditCard) {
        if (creditCard == null || creditCard.trim().isEmpty()) {
            return creditCard;
        }
        
        String cleanCard = creditCard.replaceAll("\\s|-", "");
        if (cleanCard.length() < 8) {
            return maskSensitiveData(creditCard, 2);
        }
        
        return cleanCard.substring(0, 4) + "*".repeat(cleanCard.length() - 8) + cleanCard.substring(cleanCard.length() - 4);
    }
    
    /**
     * Mascara um número de telefone.
     * 
     * @param phone número de telefone
     * @return telefone mascarado
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return phone;
        }
        
        String cleanPhone = phone.replaceAll("\\D", "");
        if (cleanPhone.length() < 6) {
            return maskSensitiveData(phone, 2);
        }
        
        if (cleanPhone.length() <= 8) {
            return cleanPhone.substring(0, 2) + "*".repeat(cleanPhone.length() - 4) + cleanPhone.substring(cleanPhone.length() - 2);
        }
        
        return cleanPhone.substring(0, 2) + "*".repeat(cleanPhone.length() - 6) + cleanPhone.substring(cleanPhone.length() - 4);
    }
    
    /**
     * Mascara um número de CPF.
     * 
     * @param cpf número do CPF
     * @return CPF mascarado
     */
    public static String maskCpf(String cpf) {
        if (cpf == null || cpf.trim().isEmpty()) {
            return cpf;
        }
        
        String cleanCpf = cpf.replaceAll("\\D", "");
        if (cleanCpf.length() != 11) {
            return maskSensitiveData(cpf, 3);
        }
        
        return cleanCpf.substring(0, 3) + ".***.***-" + cleanCpf.substring(9);
    }
    
    /**
     * Criptografa uma string usando AES.
     * 
     * @param plaintext texto a ser criptografado
     * @param key chave de criptografia
     * @return texto criptografado em base64
     */
    public static String encrypt(String plaintext, String key) {
        if (plaintext == null || key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Plaintext and key cannot be null or empty");
        }
        
        try {
            // Gera uma chave AES a partir da string fornecida
            String hashedKey = hashSHA256(key);
            // Converte hex string para bytes e pega os primeiros 32 bytes (256 bits)
            byte[] keyBytes = hexToBytes(hashedKey);
            byte[] key32 = new byte[32];
            System.arraycopy(keyBytes, 0, key32, 0, Math.min(keyBytes.length, 32));
            SecretKeySpec secretKey = new SecretKeySpec(key32, "AES");
            
            // Gera um IV aleatório
            byte[] iv = new byte[16];
            SECURE_RANDOM.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            // Configura o cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            
            // Criptografa o texto
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Combina IV + dados criptografados
            byte[] encryptedWithIv = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(encrypted, 0, encryptedWithIv, iv.length, encrypted.length);
            
            return Base64.getEncoder().encodeToString(encryptedWithIv);
            
        } catch (Exception e) {
            throw new RuntimeException("Erro na criptografia: " + e.getMessage(), e);
        }
    }
    
    /**
     * Descriptografa uma string usando AES.
     * 
     * @param encryptedText texto criptografado em base64
     * @param key chave de descriptografia
     * @return texto descriptografado
     */
    public static String decrypt(String encryptedText, String key) {
        if (encryptedText == null || key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Encrypted text and key cannot be null or empty");
        }
        
        try {
            // Decodifica o texto base64
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedText);
            
            // Extrai o IV (primeiros 16 bytes)
            byte[] iv = new byte[16];
            System.arraycopy(encryptedWithIv, 0, iv, 0, iv.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            // Extrai os dados criptografados
            byte[] encrypted = new byte[encryptedWithIv.length - 16];
            System.arraycopy(encryptedWithIv, 16, encrypted, 0, encrypted.length);
            
            // Gera a chave AES
            String hashedKey = hashSHA256(key);
            // Converte hex string para bytes e pega os primeiros 32 bytes (256 bits)
            byte[] keyBytes = hexToBytes(hashedKey);
            byte[] key32 = new byte[32];
            System.arraycopy(keyBytes, 0, key32, 0, Math.min(keyBytes.length, 32));
            SecretKeySpec secretKey = new SecretKeySpec(key32, "AES");
            
            // Configura o cipher
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            
            // Descriptografa
            byte[] decrypted = cipher.doFinal(encrypted);
            
            return new String(decrypted, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new SecurityException("Erro na descriptografia: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verifica se uma string contém padrões suspeitos.
     * 
     * @param input string a verificar
     * @return true se contém padrões suspeitos, false caso contrário
     */
    public static boolean containsSuspiciousPattern(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String lowerInput = input.toLowerCase();
        
        // Padrões de script malicioso
        String[] scriptPatterns = {
            "<script", "javascript:", "vbscript:", "onload=", "onerror=",
            "alert(", "confirm(", "prompt(", "document.cookie",
            "eval(", "expression(", "<iframe", "<object", "<embed"
        };
        
        // Padrões de SQL injection mais específicos
        String[] sqlPatterns = {
            "' or ", "' and ", "' union ", "' select ", "' drop ",
            "' delete ", "' insert ", "' update ", "-- ", "/*", "*/",
            "drop table", "delete from", "insert into", "update set"
        };
        
        // Padrões de path traversal
        String[] pathPatterns = {
            "../", "..\\", "/etc/passwd", "c:\\windows", "cmd.exe",
            "powershell.exe", "/bin/bash", "/bin/sh"
        };
        
        // Verifica padrões de script
        for (String pattern : scriptPatterns) {
            if (lowerInput.contains(pattern)) {
                return true;
            }
        }
        
        // Verifica padrões de SQL injection
        for (String pattern : sqlPatterns) {
            if (lowerInput.contains(pattern)) {
                return true;
            }
        }
        
        // Verifica padrões de path traversal
        for (String pattern : pathPatterns) {
            if (lowerInput.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
}