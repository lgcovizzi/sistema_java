package com.sistema.service;

import com.sistema.entity.RefreshToken;
import com.sistema.entity.User;
import com.sistema.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Serviço para gerenciar tokens de refresh.
 * Responsável pela criação, validação, renovação e limpeza de tokens de refresh.
 */
@Service
@Transactional
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom secureRandom = new SecureRandom();
    
    // Configurações
    @Value("${app.jwt.refresh-expiration:15552000}") // 6 meses em segundos
    private long refreshTokenExpiration;
    
    @Value("${app.refresh-token.max-per-user:5}")
    private int maxTokensPerUser;
    
    @Value("${app.refresh-token.cleanup-enabled:true}")
    private boolean cleanupEnabled;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    /**
     * Cria um novo token de refresh para o usuário.
     *
     * @param user O usuário
     * @param request A requisição HTTP para extrair informações do dispositivo
     * @return O token de refresh criado
     */
    public RefreshToken createRefreshToken(User user, HttpServletRequest request) {
        logger.debug("Criando refresh token para usuário: {}", user.getEmail());
        
        // Limita o número de tokens por usuário
        limitTokensPerUser(user);
        
        // Gera token único
        String token = generateSecureToken();
        
        // Calcula data de expiração (6 meses)
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpiration);
        
        // Extrai informações da requisição
        String deviceInfo = extractDeviceInfo(request);
        String ipAddress = extractIpAddress(request);
        String userAgent = extractUserAgent(request);
        
        // Cria o refresh token
        RefreshToken refreshToken = new RefreshToken(
            token, user, expiresAt, deviceInfo, ipAddress, userAgent
        );
        
        refreshToken.setLastUsedAt(LocalDateTime.now());
        
        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        logger.info("Refresh token criado com sucesso para usuário: {} (ID: {})", user.getEmail(), saved.getId());
        
        return saved;
    }

    /**
     * Busca e valida um token de refresh.
     *
     * @param token O valor do token
     * @return Optional contendo o RefreshToken se válido
     */
    public Optional<RefreshToken> findValidRefreshToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }
        
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findValidByToken(token, LocalDateTime.now());
        
        if (refreshToken.isPresent()) {
            // Atualiza último uso
            refreshToken.get().updateLastUsed();
            refreshTokenRepository.save(refreshToken.get());
            logger.debug("Refresh token válido encontrado e atualizado");
        } else {
            logger.debug("Refresh token inválido ou expirado: {}", token.substring(0, Math.min(token.length(), 10)) + "...");
        }
        
        return refreshToken;
    }

    /**
     * Revoga um token de refresh específico.
     *
     * @param token O valor do token
     * @return true se o token foi revogado com sucesso
     */
    public boolean revokeRefreshToken(String token) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        
        if (refreshToken.isPresent() && !refreshToken.get().getIsRevoked()) {
            refreshToken.get().revoke();
            refreshTokenRepository.save(refreshToken.get());
            logger.info("Refresh token revogado: {}", refreshToken.get().getId());
            return true;
        }
        
        return false;
    }

    /**
     * Revoga todos os tokens de refresh de um usuário.
     *
     * @param user O usuário
     * @return Número de tokens revogados
     */
    public int revokeAllUserTokens(User user) {
        int revokedCount = refreshTokenRepository.revokeAllByUser(user);
        logger.info("Revogados {} refresh tokens do usuário: {}", revokedCount, user.getEmail());
        return revokedCount;
    }

    /**
     * Revoga todos os tokens de um usuário exceto o token atual.
     *
     * @param user O usuário
     * @param currentToken Token atual a ser preservado
     * @return Número de tokens revogados
     */
    public int revokeOtherUserTokens(User user, String currentToken) {
        int revokedCount = refreshTokenRepository.revokeAllByUserExcept(user, currentToken);
        logger.info("Revogados {} outros refresh tokens do usuário: {}", revokedCount, user.getEmail());
        return revokedCount;
    }

    /**
     * Lista todos os tokens válidos de um usuário.
     *
     * @param user O usuário
     * @return Lista de tokens válidos
     */
    public List<RefreshToken> getUserValidTokens(User user) {
        return refreshTokenRepository.findValidByUser(user, LocalDateTime.now());
    }

    /**
     * Obtém estatísticas de tokens de um usuário.
     *
     * @param user O usuário
     * @return Array com [total, válidos, expirados, revogados]
     */
    public long[] getUserTokenStats(User user) {
        Object[] stats = refreshTokenRepository.getTokenStatsByUser(user, LocalDateTime.now());
        return new long[] {
            ((Number) stats[0]).longValue(), // total
            ((Number) stats[1]).longValue(), // válidos
            ((Number) stats[2]).longValue(), // expirados
            ((Number) stats[3]).longValue()  // revogados
        };
    }

    /**
     * Verifica se o usuário atingiu o limite de tokens.
     *
     * @param user O usuário
     * @return true se atingiu o limite
     */
    public boolean hasReachedTokenLimit(User user) {
        long validTokens = refreshTokenRepository.countValidByUser(user, LocalDateTime.now());
        return validTokens >= maxTokensPerUser;
    }

    /**
     * Limita o número de tokens por usuário, removendo os mais antigos se necessário.
     *
     * @param user O usuário
     */
    private void limitTokensPerUser(User user) {
        List<RefreshToken> validTokens = refreshTokenRepository.findValidByUser(user, LocalDateTime.now());
        
        if (validTokens.size() >= maxTokensPerUser) {
            // Remove os tokens mais antigos
            int tokensToRemove = validTokens.size() - maxTokensPerUser + 1;
            
            for (int i = validTokens.size() - tokensToRemove; i < validTokens.size(); i++) {
                RefreshToken tokenToRevoke = validTokens.get(i);
                tokenToRevoke.revoke();
                refreshTokenRepository.save(tokenToRevoke);
            }
            
            logger.info("Removidos {} tokens antigos do usuário: {}", tokensToRemove, user.getEmail());
        }
    }

    /**
     * Gera um token seguro aleatório.
     *
     * @return Token seguro em Base64
     */
    private String generateSecureToken() {
        byte[] randomBytes = new byte[64]; // 512 bits
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Extrai informações do dispositivo da requisição.
     *
     * @param request A requisição HTTP
     * @return Informações do dispositivo
     */
    private String extractDeviceInfo(HttpServletRequest request) {
        if (request == null) {
            return "Unknown Device";
        }
        
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "Unknown Device";
        }
        
        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("mobile")) {
            return "Mobile Device";
        } else if (userAgent.contains("tablet")) {
            return "Tablet";
        } else if (userAgent.contains("postman")) {
            return "Postman";
        } else if (userAgent.contains("curl")) {
            return "cURL";
        } else {
            return "Desktop";
        }
    }

    /**
     * Extrai o endereço IP da requisição.
     *
     * @param request A requisição HTTP
     * @return Endereço IP
     */
    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "Unknown IP";
        }
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Extrai o User-Agent da requisição.
     *
     * @param request A requisição HTTP
     * @return User-Agent
     */
    private String extractUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "Unknown";
        }
        
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "Unknown";
    }

    /**
     * Limpeza automática de tokens expirados e revogados antigos.
     * Executa diariamente às 2:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredTokens() {
        if (!cleanupEnabled) {
            return;
        }
        
        logger.info("Iniciando limpeza automática de refresh tokens");
        
        try {
            // Remove tokens expirados
            int expiredRemoved = refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
            
            // Remove tokens revogados há mais de 30 dias
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
            int revokedRemoved = refreshTokenRepository.deleteOldRevokedTokens(cutoffDate);
            
            logger.info("Limpeza concluída: {} tokens expirados e {} tokens revogados antigos removidos", 
                       expiredRemoved, revokedRemoved);
            
        } catch (Exception e) {
            logger.error("Erro durante limpeza automática de tokens: {}", e.getMessage(), e);
        }
    }

    /**
     * Limpeza manual de tokens.
     *
     * @return Número total de tokens removidos
     */
    public int manualCleanup() {
        logger.info("Iniciando limpeza manual de refresh tokens");
        
        int expiredRemoved = refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        int revokedRemoved = refreshTokenRepository.deleteOldRevokedTokens(cutoffDate);
        
        int totalRemoved = expiredRemoved + revokedRemoved;
        logger.info("Limpeza manual concluída: {} tokens removidos", totalRemoved);
        
        return totalRemoved;
    }

    /**
     * Busca tokens que expiram em breve para notificação.
     *
     * @return Lista de tokens que expiram nos próximos 7 dias
     */
    public List<RefreshToken> getTokensExpiringSoon() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime soonDate = now.plusDays(7);
        return refreshTokenRepository.findTokensExpiringSoon(now, soonDate);
    }

    /**
     * Busca tokens inativos (não usados há mais de 30 dias).
     *
     * @return Lista de tokens inativos
     */
    public List<RefreshToken> getInactiveTokens() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        return refreshTokenRepository.findInactiveTokens(cutoffDate);
    }
}