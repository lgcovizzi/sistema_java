package com.sistema.service;

import com.sistema.service.base.BaseRedisService;
import com.sistema.service.interfaces.SecurityOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Serviço responsável por gerenciar a blacklist de tokens JWT revogados.
 * Utiliza Redis para armazenar tokens revogados com TTL baseado na expiração do token.
 * Implementa SecurityOperations para padronizar operações de segurança.
 */
@Service
public class TokenBlacklistService extends BaseRedisService implements SecurityOperations {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private JwtService jwtService;
    
    /**
     * Adiciona um token à blacklist.
     * O token será armazenado no Redis com TTL baseado na sua data de expiração.
     * 
     * @param token Token JWT a ser revogado
     * @return true se o token foi adicionado com sucesso, false caso contrário
     */
    public boolean revokeToken(String token) {
        try {
            // Extrai o JTI (ID único do token) ou usa hash do token como fallback
            String tokenId = extractTokenIdentifier(token);
            
            // Calcula o TTL baseado na expiração do token
            Date expiration = jwtService.extractExpiration(token);
            long ttlSeconds = calculateTTL(expiration);
            
            if (ttlSeconds <= 0) {
                logger.warn("Token já expirado, não será adicionado à blacklist: {}", tokenId);
                return false;
            }
            
            // Adiciona à blacklist com TTL
            String key = BLACKLIST_PREFIX + tokenId;
            redisTemplate.opsForValue().set(key, "revoked", ttlSeconds, TimeUnit.SECONDS);
            
            logger.info("Token revogado e adicionado à blacklist: {} (TTL: {}s)", tokenId, ttlSeconds);
            return true;
            
        } catch (Exception e) {
            logger.error("Erro ao revogar token: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Verifica se um token está na blacklist.
     * 
     * @param token Token JWT a ser verificado
     * @return true se o token está revogado, false caso contrário
     */
    public boolean isTokenRevoked(String token) {
        try {
            String tokenId = extractTokenIdentifier(token);
            String key = BLACKLIST_PREFIX + tokenId;
            
            Boolean exists = redisTemplate.hasKey(key);
            boolean isRevoked = Boolean.TRUE.equals(exists);
            
            if (isRevoked) {
                logger.debug("Token encontrado na blacklist: {}", tokenId);
            }
            
            return isRevoked;
            
        } catch (Exception e) {
            logger.error("Erro ao verificar blacklist para token: {}", e.getMessage(), e);
            // Em caso de erro, considera o token como não revogado para não bloquear usuários válidos
            return false;
        }
    }
    
    /**
     * Remove um token da blacklist (usado principalmente para testes).
     * 
     * @param token Token a ser removido da blacklist
     * @return true se removido com sucesso, false caso contrário
     */
    public boolean removeFromBlacklist(String token) {
        try {
            String tokenId = extractTokenIdentifier(token);
            String key = BLACKLIST_PREFIX + tokenId;
            
            Boolean deleted = redisTemplate.delete(key);
            boolean success = Boolean.TRUE.equals(deleted);
            
            if (success) {
                logger.info("Token removido da blacklist: {}", tokenId);
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Erro ao remover token da blacklist: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Revoga todos os tokens de um usuário específico.
     * Adiciona uma entrada na blacklist baseada no usuário e timestamp.
     * 
     * @param username Nome do usuário
     * @return true se a operação foi bem-sucedida
     */
    public boolean revokeAllUserTokens(String username) {
        try {
            // Cria uma entrada de revogação global para o usuário
            String key = BLACKLIST_PREFIX + "user:" + username;
            long currentTime = System.currentTimeMillis();
            
            // Armazena o timestamp de revogação (tokens emitidos antes deste momento são inválidos)
            redisTemplate.opsForValue().set(key, currentTime, Duration.ofDays(30));
            
            logger.info("Todos os tokens do usuário {} foram revogados a partir de {}", username, new Date(currentTime));
            return true;
            
        } catch (Exception e) {
            logger.error("Erro ao revogar todos os tokens do usuário {}: {}", username, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Verifica se um token foi emitido antes da revogação global do usuário.
     * 
     * @param token Token a ser verificado
     * @param username Nome do usuário
     * @return true se o token foi revogado globalmente
     */
    public boolean isTokenGloballyRevoked(String token, String username) {
        try {
            String key = BLACKLIST_PREFIX + "user:" + username;
            Object revocationTime = redisTemplate.opsForValue().get(key);
            
            if (revocationTime == null) {
                return false;
            }
            
            long revocationTimestamp = Long.parseLong(revocationTime.toString());
            Date tokenIssuedAt = jwtService.extractIssuedAt(token);
            
            if (tokenIssuedAt == null) {
                return false;
            }
            
            boolean isRevoked = tokenIssuedAt.getTime() < revocationTimestamp;
            
            if (isRevoked) {
                logger.debug("Token do usuário {} foi revogado globalmente", username);
            }
            
            return isRevoked;
            
        } catch (Exception e) {
            logger.error("Erro ao verificar revogação global para usuário {}: {}", username, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Obtém estatísticas da blacklist.
     * 
     * @return Número aproximado de tokens na blacklist
     */
    public long getBlacklistSize() {
        try {
            return redisTemplate.keys(BLACKLIST_PREFIX + "*").size();
        } catch (Exception e) {
            logger.error("Erro ao obter tamanho da blacklist: {}", e.getMessage(), e);
            return -1;
        }
    }
    
    /**
     * Extrai um identificador único do token.
     * Tenta usar o JTI se disponível, caso contrário usa hash do token.
     * 
     * @param token Token JWT
     * @return Identificador único do token
     */
    private String extractTokenIdentifier(String token) {
        try {
            // Tenta extrair JTI (JWT ID) se disponível
            String jti = jwtService.extractJti(token);
            if (jti != null && !jti.isEmpty()) {
                return jti;
            }
        } catch (Exception e) {
            logger.debug("JTI não disponível, usando hash do token: {}", e.getMessage());
        }
        
        // Fallback: usa hash do token como identificador
        return String.valueOf(token.hashCode());
    }
    
    /**
     * Calcula o TTL em segundos baseado na data de expiração do token.
     * 
     * @param expiration Data de expiração do token
     * @return TTL em segundos
     */
    private long calculateTTL(Date expiration) {
        if (expiration == null) {
            // Se não há expiração, usa TTL padrão de 24 horas
            return Duration.ofHours(24).getSeconds();
        }
        
        Instant now = Instant.now();
        Instant expirationInstant = expiration.toInstant();
        
        if (expirationInstant.isBefore(now)) {
            return 0; // Token já expirado
        }
        
        return Duration.between(now, expirationInstant).getSeconds();
    }
    
    // Implementação da interface SecurityOperations
    
    @Override
    public boolean authenticateUser(String username, String password) {
        // TokenBlacklistService não lida com autenticação direta
        // Este método deve ser implementado por um serviço de autenticação
        throw new UnsupportedOperationException("Autenticação não é responsabilidade do TokenBlacklistService");
    }
    
    @Override
    public boolean validateTokenSecurity(String token) {
        return !isTokenRevoked(token);
    }
    
    @Override
    public boolean revokeTokenSecurity(String token) {
        return revokeToken(token);
    }
    
    @Override
    public boolean revokeAllUserTokensSecurity(String username) {
        return revokeAllUserTokens(username);
    }
    
    @Override
    public boolean hasPermission(String username, String permission) {
        // TokenBlacklistService não lida com permissões
        // Este método deve ser implementado por um serviço de autorização
        throw new UnsupportedOperationException("Verificação de permissões não é responsabilidade do TokenBlacklistService");
    }
    
    @Override
    public boolean hasRole(String username, String role) {
        // TokenBlacklistService não lida com roles
        // Este método deve ser implementado por um serviço de autorização
        throw new UnsupportedOperationException("Verificação de roles não é responsabilidade do TokenBlacklistService");
    }
    
    @Override
    public boolean hasAnyRole(String username, String... roles) {
        // TokenBlacklistService não lida com roles
        // Este método deve ser implementado por um serviço de autorização
        throw new UnsupportedOperationException("Verificação de roles não é responsabilidade do TokenBlacklistService");
    }
    
    @Override
    public void logSecurityEvent(String event, String username, String details) {
        logInfo("Evento de segurança: {} - Usuário: {} - Detalhes: {}", event, username, details);
    }
    
    @Override
    public void logSecurityEvent(String event, String username) {
        logSecurityEvent(event, username, "");
    }
}