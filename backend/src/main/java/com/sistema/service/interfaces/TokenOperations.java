package com.sistema.service.interfaces;

import com.sistema.entity.User;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Interface para operações de token padronizadas.
 * Define métodos comuns para geração, validação e manipulação de tokens JWT.
 */
public interface TokenOperations {
    
    /**
     * Gera um token de acesso para o usuário.
     * 
     * @param user usuário para o qual gerar o token
     * @return token de acesso
     */
    String generateAccessToken(User user);
    
    /**
     * Gera um token de refresh para o usuário.
     * 
     * @param user usuário para o qual gerar o token
     * @return token de refresh
     */
    String generateRefreshToken(User user);
    
    /**
     * Extrai o subject (email) do token.
     * 
     * @param token token JWT
     * @return email do usuário
     */
    String extractSubject(String token);
    
    /**
     * Extrai a data de expiração do token.
     * 
     * @param token token JWT
     * @return data de expiração
     */
    Date extractExpiration(String token);
    
    /**
     * Extrai o tipo do token.
     * 
     * @param token token JWT
     * @return tipo do token (access, refresh)
     */
    String extractTokenType(String token);
    
    /**
     * Extrai as roles do token.
     * 
     * @param token token JWT
     * @return lista de roles
     */
    List<String> extractRoles(String token);
    
    /**
     * Extrai a data de emissão do token.
     * 
     * @param token token JWT
     * @return data de emissão
     */
    Date extractIssuedAt(String token);
    
    /**
     * Verifica se o token está expirado.
     * 
     * @param token token JWT
     * @return true se expirado
     */
    boolean isTokenExpired(String token);
    
    /**
     * Valida se o token é válido para o usuário.
     * 
     * @param token token JWT
     * @param user usuário
     * @return true se válido
     */
    boolean isTokenValid(String token, User user);
    
    /**
     * Verifica se é um token de acesso válido.
     * 
     * @param token token JWT
     * @return true se é um token de acesso válido
     */
    boolean isValidAccessToken(String token);
    
    /**
     * Verifica se é um token de refresh válido.
     * 
     * @param token token JWT
     * @return true se é um token de refresh válido
     */
    boolean isValidRefreshToken(String token);
    
    /**
     * Obtém informações detalhadas do token.
     * 
     * @param token token JWT
     * @return mapa com informações do token
     */
    Map<String, Object> getTokenInfo(String token);
    
    /**
     * Obtém tempo restante até expiração em segundos.
     * 
     * @param token token JWT
     * @return segundos até expiração
     */
    long getTimeToExpiration(String token);
    
    /**
     * Obtém tempo de expiração do token de acesso em segundos.
     * 
     * @return segundos de expiração
     */
    long getAccessTokenExpiration();
    
    /**
     * Obtém tempo de expiração do token de refresh em segundos.
     * 
     * @return segundos de expiração
     */
    long getRefreshTokenExpiration();
}