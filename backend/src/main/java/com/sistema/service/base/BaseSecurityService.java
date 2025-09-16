package com.sistema.service.base;

import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.util.ValidationUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Classe base para serviços relacionados à segurança.
 * Fornece operações comuns de autenticação, autorização e validação de segurança.
 */
public abstract class BaseSecurityService extends BaseService {
    
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._-]{3,20}$"
    );
    
    /**
     * Obtém o usuário autenticado atual.
     * 
     * @return usuário autenticado
     * @throws RuntimeException se não há usuário autenticado
     */
    protected User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Usuário não autenticado");
        }
        
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            throw new RuntimeException("Principal não é uma instância de UserDetails");
        }
        
        UserDetails userDetails = (UserDetails) principal;
        String username = userDetails.getUsername();
        
        // Assumindo que o username é o email
        return findUserByEmailRequired(username);
    }
    
    /**
     * Obtém o ID do usuário autenticado atual.
     * 
     * @return ID do usuário autenticado
     */
    protected Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
    
    /**
     * Verifica se o usuário atual tem uma role específica.
     * 
     * @param role role a verificar
     * @return true se o usuário tem a role
     */
    protected boolean hasRole(UserRole role) {
        try {
            User currentUser = getCurrentUser();
            return currentUser.getRole().equals(role);
        } catch (Exception e) {
            logger.warn("Erro ao verificar role do usuário: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica se o usuário atual é administrador.
     * 
     * @return true se é administrador
     */
    protected boolean isAdmin() {
        return hasRole(UserRole.ADMIN);
    }
    
    /**
     * Verifica se o usuário atual pode acessar dados de outro usuário.
     * Administradores podem acessar qualquer usuário, usuários comuns apenas seus próprios dados.
     * 
     * @param targetUserId ID do usuário alvo
     * @return true se pode acessar
     */
    protected boolean canAccessUser(Long targetUserId) {
        try {
            User currentUser = getCurrentUser();
            return currentUser.getRole().equals(UserRole.ADMIN) || 
                   currentUser.getId().equals(targetUserId);
        } catch (Exception e) {
            logger.warn("Erro ao verificar acesso ao usuário {}: {}", targetUserId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Valida se o usuário atual pode acessar dados de outro usuário.
     * 
     * @param targetUserId ID do usuário alvo
     * @throws RuntimeException se não pode acessar
     */
    protected void validateUserAccess(Long targetUserId) {
        if (!canAccessUser(targetUserId)) {
            String message = String.format("Acesso negado ao usuário ID: %d", targetUserId);
            logger.warn(message);
            throw new RuntimeException(message);
        }
    }
    
    /**
     * Valida se o usuário atual é administrador.
     * 
     * @throws RuntimeException se não é administrador
     */
    protected void requireAdmin() {
        if (!isAdmin()) {
            String message = "Acesso negado: permissões de administrador necessárias";
            logger.warn(message);
            throw new RuntimeException(message);
        }
    }
    
    /**
     * Gera um token aleatório seguro.
     * 
     * @param length comprimento do token em bytes
     * @return token em base64
     */
    protected String generateSecureToken(int length) {
        ValidationUtils.validatePositive(length, "length");
        
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    /**
     * Gera um token aleatório de 32 bytes.
     * 
     * @return token em base64
     */
    protected String generateSecureToken() {
        return generateSecureToken(32);
    }
    
    /**
     * Valida formato de email.
     * 
     * @param email email a validar
     * @throws IllegalArgumentException se formato inválido
     */
    protected void validateEmailFormat(String email) {
        validateNotEmpty(email, "email");
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Formato de email inválido: " + email);
        }
    }
    
    /**
     * Valida formato de username.
     * 
     * @param username username a validar
     * @throws IllegalArgumentException se formato inválido
     */
    protected void validateUsernameFormat(String username) {
        validateNotEmpty(username, "username");
        
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                "Username deve ter 3-20 caracteres e conter apenas letras, números, pontos, hífens e underscores"
            );
        }
    }
    
    /**
     * Valida se uma string é um token válido.
     * 
     * @param token token a validar
     * @throws IllegalArgumentException se token inválido
     */
    protected void validateTokenFormat(String token) {
        validateNotEmpty(token, "token");
        
        // Verifica se é base64 válido
        try {
            Base64.getUrlDecoder().decode(token);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Formato de token inválido");
        }
        
        // Verifica comprimento mínimo (24 bytes = 32 caracteres em base64)
        if (token.length() < 32) {
            throw new IllegalArgumentException("Token muito curto");
        }
    }
    
    /**
     * Sanitiza uma string removendo caracteres perigosos.
     * 
     * @param input string a sanitizar
     * @return string sanitizada
     */
    protected String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        return input.trim()
                   .replaceAll("[<>\"'&]", "") // Remove caracteres HTML perigosos
                   .replaceAll("\\s+", " ");   // Normaliza espaços
    }
    
    /**
     * Valida se um IP é válido.
     * 
     * @param ip endereço IP a validar
     * @throws IllegalArgumentException se IP inválido
     */
    protected void validateIpAddress(String ip) {
        validateNotEmpty(ip, "ip");
        
        // Regex simples para IPv4
        Pattern ipPattern = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        );
        
        if (!ipPattern.matcher(ip).matches()) {
            throw new IllegalArgumentException("Formato de IP inválido: " + ip);
        }
    }
    
    /**
     * Gera um hash seguro para uma string.
     * 
     * @param input string a fazer hash
     * @param salt salt para o hash
     * @return hash em hexadecimal
     */
    protected String generateSecureHash(String input, String salt) {
        validateNotEmpty(input, "input");
        validateNotEmpty(salt, "salt");
        
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes());
            byte[] hash = digest.digest(input.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar hash: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verifica se uma string corresponde a um hash.
     * 
     * @param input string original
     * @param salt salt usado no hash
     * @param expectedHash hash esperado
     * @return true se corresponde
     */
    protected boolean verifyHash(String input, String salt, String expectedHash) {
        try {
            String actualHash = generateSecureHash(input, salt);
            return actualHash.equals(expectedHash);
        } catch (Exception e) {
            logger.warn("Erro ao verificar hash: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Método abstrato para buscar usuário por email.
     * Deve ser implementado pelas classes filhas.
     * 
     * @param email email do usuário
     * @return usuário encontrado
     */
    protected abstract User findUserByEmailRequired(String email);
}