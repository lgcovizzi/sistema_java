package com.sistema.service;

import com.sistema.config.RSAKeyManager;
import com.sistema.entity.User;
import com.sistema.service.base.BaseService;
import com.sistema.service.interfaces.TokenOperations;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Serviço para geração e validação de tokens JWT usando chaves RSA.
 * Utiliza o RSAKeyManager para obter as chaves criptográficas.
 * Implementa TokenOperations para padronizar operações de token.
 */
@Service
public class JwtService extends BaseService implements TokenOperations {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private final RSAKeyManager rsaKeyManager;

    // Configurações de tempo de vida dos tokens
    @Value("${app.jwt.access-token.expiration:3600}")
    private long accessTokenExpirationSeconds;

    @Value("${app.jwt.refresh-token.expiration:15552000}") // 6 meses em segundos
    private long refreshTokenExpirationSeconds;

    @Value("${app.jwt.issuer:sistema-java}")
    private String issuer;

    @Autowired
    public JwtService(RSAKeyManager rsaKeyManager) {
        this.rsaKeyManager = rsaKeyManager;
    }

    /**
     * Gera um token de acesso JWT para o usuário.
     * 
     * @param user o usuário para o qual gerar o token
     * @return token JWT de acesso
     */
    public String generateAccessToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("type", "access");
        extraClaims.put("userId", user.getId());
        extraClaims.put("roles", user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        extraClaims.put("email", user.getEmail());
        extraClaims.put("fullName", user.getFullName());
        
        return generateToken(extraClaims, user.getUsername(), accessTokenExpirationSeconds);
    }

    /**
     * Gera um token de refresh JWT para o usuário.
     * 
     * @param user o usuário para o qual gerar o token
     * @return token JWT de refresh
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("type", "refresh");
        
        return generateToken(extraClaims, user.getUsername(), refreshTokenExpirationSeconds);
    }

    /**
     * Gera um token JWT com claims personalizados.
     * 
     * @param extraClaims claims adicionais
     * @param username nome do usuário
     * @param expirationSeconds tempo de expiração em segundos
     * @return token JWT
     */
    private String generateToken(Map<String, Object> extraClaims, String username, long expirationSeconds) {
        try {
            PrivateKey privateKey = rsaKeyManager.getPrivateKey();
            
            Instant now = Instant.now();
            Instant expiration = now.plus(expirationSeconds, ChronoUnit.SECONDS);
            
            String token = Jwts.builder()
                    .claims(extraClaims)
                    .subject(username)
                    .issuer(issuer)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(expiration))
                    .signWith(privateKey, Jwts.SIG.RS256)
                    .compact();
            
            logger.debug("Token JWT gerado para usuário: {}", username);
            return token;
            
        } catch (Exception e) {
            logger.error("Erro ao gerar token JWT para usuário: {}", username, e);
            throw new RuntimeException("Erro ao gerar token JWT", e);
        }
    }

    /**
     * Extrai o username do token JWT.
     * 
     * @param token o token JWT
     * @return username
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrai a data de expiração do token JWT.
     * 
     * @param token o token JWT
     * @return data de expiração
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrai o tipo do token (access ou refresh).
     * 
     * @param token o token JWT
     * @return tipo do token
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    /**
     * Extrai as roles do token JWT.
     * 
     * @param token o token JWT
     * @return lista de roles
     */
    @SuppressWarnings("unchecked")
    public java.util.List<String> extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", java.util.List.class));
    }

    /**
     * Extrai a data de emissão do token JWT.
     * 
     * @param token o token JWT
     * @return data de emissão
     */
    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    /**
     * Extrai o JTI (JWT ID) do token JWT.
     * 
     * @param token o token JWT
     * @return JTI do token ou null se não estiver presente
     */
    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    /**
     * Extrai um claim específico do token JWT.
     * 
     * @param token o token JWT
     * @param claimsResolver função para extrair o claim
     * @param <T> tipo do claim
     * @return valor do claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrai todos os claims do token JWT.
     * 
     * @param token o token JWT
     * @return claims do token
     */
    private Claims extractAllClaims(String token) {
        try {
            PublicKey publicKey = rsaKeyManager.getPublicKey();
            
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
                    
        } catch (ExpiredJwtException e) {
            logger.warn("Token JWT expirado: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            logger.error("Token JWT não suportado: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            logger.error("Token JWT malformado: {}", e.getMessage());
            throw e;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            logger.error("Falha na validação da assinatura do token JWT: {}", e.getMessage());
            throw e;
        } catch (SecurityException e) {
            logger.error("Falha na validação da assinatura do token JWT: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("Token JWT inválido: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Erro ao processar token JWT: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao processar token JWT", e);
        }
    }

    /**
     * Verifica se o token JWT está expirado.
     * 
     * @param token o token JWT
     * @return true se expirado
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Valida se o token JWT é válido para o usuário.
     * 
     * @param token o token JWT
     * @param user o usuário
     * @return true se válido
     */
    public boolean isTokenValid(String token, User user) {
        try {
            final String username = extractUsername(token);
            return (username.equals(user.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            logger.warn("Token inválido para usuário {}: {}", user.getUsername(), e.getMessage());
            return false;
        }
    }

    /**
     * Valida se o token é um token de acesso válido.
     * 
     * @param token o token JWT
     * @return true se for um token de acesso válido
     * @throws SignatureException se a assinatura for inválida
     * @throws MalformedJwtException se o token for malformado
     * @throws ExpiredJwtException se o token estiver expirado
     */
    public boolean isValidAccessToken(String token) {
        String tokenType = extractTokenType(token);
        return "access".equals(tokenType) && !isTokenExpired(token);
    }

    /**
     * Valida se o token é um token de acesso válido (versão segura que não lança exceções).
     * 
     * @param token o token JWT
     * @return true se for um token de acesso válido
     */
    public boolean isValidAccessTokenSafe(String token) {
        try {
            String tokenType = extractTokenType(token);
            return "access".equals(tokenType) && !isTokenExpired(token);
        } catch (Exception e) {
            logger.warn("Token de acesso inválido: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Valida se o token é um token de refresh válido.
     * 
     * @param token o token JWT
     * @return true se for um token de refresh válido
     */
    public boolean isValidRefreshToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return "refresh".equals(tokenType) && !isTokenExpired(token);
        } catch (Exception e) {
            logger.warn("Token de refresh inválido: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Obtém informações sobre o token (para debugging/logging).
     * 
     * @param token o token JWT
     * @return mapa com informações do token
     */
    public Map<String, Object> getTokenInfo(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Map<String, Object> info = new HashMap<>();
            info.put("username", claims.getSubject());
            info.put("userId", claims.get("userId"));
            info.put("email", claims.get("email"));
            info.put("roles", claims.get("roles"));
            info.put("tokenType", claims.get("type"));
            info.put("issuer", claims.getIssuer());
            info.put("issuedAt", claims.getIssuedAt());
            info.put("expiresAt", claims.getExpiration());
            info.put("expired", isTokenExpired(token));
            return info;
        } catch (Exception e) {
            logger.error("Erro ao obter informações do token: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Obtém o tempo restante até a expiração do token em segundos.
     * 
     * @param token o token JWT
     * @return segundos até expiração, ou 0 se já expirado
     */
    public long getTimeToExpiration(String token) {
        try {
            Date expiration = extractExpiration(token);
            long timeToExpiration = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            return Math.max(0, timeToExpiration);
        } catch (Exception e) {
            return 0;
        }
    }
    
    // Implementação da interface TokenOperations
    
    @Override
    public String generateToken(String username, Map<String, Object> claims, long expirationSeconds) {
        return generateToken(claims, username, expirationSeconds);
    }
    
    @Override
    public String generateAccessToken(String username) {
        // Para compatibilidade, criamos um User temporário
        User tempUser = new User();
        tempUser.setUsername(username);
        return generateAccessToken(tempUser);
    }
    
    @Override
    public String generateRefreshToken(String username) {
        // Para compatibilidade, criamos um User temporário
        User tempUser = new User();
        tempUser.setUsername(username);
        return generateRefreshToken(tempUser);
    }
    
    @Override
    public String extractSubject(String token) {
        return extractUsername(token);
    }
    
    @Override
    public Date extractExpirationDate(String token) {
        return extractExpiration(token);
    }
    
    @Override
    public Date extractIssuedDate(String token) {
        return extractIssuedAt(token);
    }
    
    @Override
    public String extractTokenId(String token) {
        return extractJti(token);
    }
    
    @Override
    public String extractType(String token) {
        return extractTokenType(token);
    }
    
    @Override
    public java.util.List<String> extractUserRoles(String token) {
        return extractRoles(token);
    }
    
    @Override
    public <T> T extractCustomClaim(String token, String claimName, Class<T> claimType) {
        return extractClaim(token, claims -> claims.get(claimName, claimType));
    }
    
    @Override
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean validateTokenForUser(String token, String username) {
        try {
            String tokenUsername = extractUsername(token);
            return tokenUsername.equals(username) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean isExpired(String token) {
        return isTokenExpired(token);
    }
    
    @Override
    public boolean isAccessToken(String token) {
        return isValidAccessTokenSafe(token);
    }
    
    @Override
    public boolean isRefreshToken(String token) {
        return isValidRefreshToken(token);
    }
    
    @Override
    public Map<String, Object> getTokenInformation(String token) {
        return getTokenInfo(token);
    }
    
    @Override
    public long getSecondsToExpiration(String token) {
        return getTimeToExpiration(token);
    }
}