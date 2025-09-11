package com.sistema.java.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utilitário para operações com tokens JWT
 * Referência: Controle de Acesso - project_rules.md
 * Referência: Segurança - project_rules.md
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    
    // Validade do token: 24 horas
    private static final long JWT_TOKEN_VALIDITY = 24 * 60 * 60 * 1000L;
    
    // Validade do refresh token: 7 dias
    private static final long JWT_REFRESH_TOKEN_VALIDITY = 7 * 24 * 60 * 60 * 1000L;
    
    @Value("${jwt.secret:sistema-java-secret-key-muito-segura-para-desenvolvimento-2024}")
    private String secret;
    
    @Value("${jwt.issuer:sistema-java}")
    private String issuer;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    
    /**
     * Extrai o username (email) do token JWT
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param token Token JWT
     * @return Username (email) do usuário
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }
    
    /**
     * Extrai a data de expiração do token JWT
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param token Token JWT
     * @return Data de expiração
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }
    
    /**
     * Extrai um claim específico do token JWT
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param token Token JWT
     * @param claimsResolver Função para extrair o claim
     * @param <T> Tipo do claim
     * @return Valor do claim
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Extrai todos os claims do token JWT
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param token Token JWT
     * @return Claims do token
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
    /**
     * Verifica se o token JWT está expirado
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param token Token JWT
     * @return true se o token está expirado
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
    
    /**
     * Gera token JWT para um usuário
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param userDetails Detalhes do usuário
     * @return Token JWT
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        
        // Adicionar informações extras ao token
        claims.put("authorities", userDetails.getAuthorities());
        claims.put("enabled", userDetails.isEnabled());
        claims.put("accountNonExpired", userDetails.isAccountNonExpired());
        claims.put("accountNonLocked", userDetails.isAccountNonLocked());
        claims.put("credentialsNonExpired", userDetails.isCredentialsNonExpired());
        
        return createToken(claims, userDetails.getUsername(), JWT_TOKEN_VALIDITY);
    }
    
    /**
     * Gera refresh token para um usuário
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param userDetails Detalhes do usuário
     * @return Refresh token
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        
        return createToken(claims, userDetails.getUsername(), JWT_REFRESH_TOKEN_VALIDITY);
    }
    
    /**
     * Cria um token JWT com claims específicos
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param claims Claims do token
     * @param subject Subject (username) do token
     * @param validity Validade em milissegundos
     * @return Token JWT
     */
    private String createToken(Map<String, Object> claims, String subject, long validity) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validity);
        
        logger.debug("Criando token JWT para usuário: {} com validade até: {}", subject, expiryDate);
        
        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuer(issuer)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }
    
    /**
     * Valida se o token JWT é válido para o usuário
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param token Token JWT
     * @param userDetails Detalhes do usuário
     * @return true se o token é válido
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);
            
            if (isValid) {
                logger.debug("Token JWT válido para usuário: {}", username);
            } else {
                logger.warn("Token JWT inválido para usuário: {} - Username match: {}, Expired: {}", 
                           username, username.equals(userDetails.getUsername()), isTokenExpired(token));
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.error("Erro ao validar token JWT: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica se um token é um refresh token
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param token Token JWT
     * @return true se é um refresh token
     */
    public Boolean isRefreshToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return "refresh".equals(claims.get("type"));
        } catch (Exception e) {
            logger.error("Erro ao verificar tipo do token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtém informações do token para logging/debugging
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param token Token JWT
     * @return Map com informações do token
     */
    public Map<String, Object> getTokenInfo(String token) {
        Map<String, Object> info = new HashMap<>();
        
        try {
            Claims claims = getAllClaimsFromToken(token);
            
            info.put("subject", claims.getSubject());
            info.put("issuer", claims.getIssuer());
            info.put("issuedAt", claims.getIssuedAt());
            info.put("expiration", claims.getExpiration());
            info.put("expired", isTokenExpired(token));
            info.put("type", claims.get("type", "access"));
            
        } catch (Exception e) {
            info.put("error", e.getMessage());
            info.put("valid", false);
        }
        
        return info;
    }
    
    /**
     * Extrai o papel/role do usuário do token
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param token Token JWT
     * @return Role do usuário ou null se não encontrado
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, String>> authorities = 
                (java.util.List<Map<String, String>>) claims.get("authorities");
            
            if (authorities != null && !authorities.isEmpty()) {
                return authorities.get(0).get("authority");
            }
            
        } catch (Exception e) {
            logger.error("Erro ao extrair role do token: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Verifica se o token tem uma role específica
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param token Token JWT
     * @param role Role a verificar (com ou sem prefixo ROLE_)
     * @return true se o token tem a role
     */
    public Boolean hasRole(String token, String role) {
        String tokenRole = getRoleFromToken(token);
        if (tokenRole == null) {
            return false;
        }
        
        // Normalizar roles para comparação
        String normalizedTokenRole = tokenRole.startsWith("ROLE_") ? tokenRole : "ROLE_" + tokenRole;
        String normalizedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        
        return normalizedTokenRole.equals(normalizedRole);
    }
    
    /**
     * Gera token para verificação de email
     * Referência: Sistema de Email com MailHog - project_rules.md
     * 
     * @param usuario Usuário para gerar o token
     * @return Token de verificação de email
     */
    public String generateEmailVerificationToken(com.sistema.java.model.entity.Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", usuario.getEmail());
        claims.put("type", "email_verification");
        claims.put("userId", usuario.getId());
        
        return createToken(claims, usuario.getEmail(), JWT_TOKEN_VALIDITY);
    }
}