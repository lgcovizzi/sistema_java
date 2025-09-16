package com.sistema.service;

import com.sistema.service.base.BaseRedisService;
import com.sistema.service.interfaces.AttemptControlOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Serviço para controle de tentativas de login.
 * Estende BaseRedisService para reutilizar operações Redis.
 * Implementa AttemptControlOperations para padronizar operações de controle de tentativas.
 */
@Service
public class AttemptService extends BaseRedisService implements AttemptControlOperations {
    
    private static final Logger logger = LoggerFactory.getLogger(AttemptService.class);
    
    private static final int MAX_ATTEMPTS_BEFORE_CAPTCHA = 5;
    private static final int ATTEMPT_EXPIRY_MINUTES = 30;
    private static final int PASSWORD_RESET_RATE_LIMIT_MINUTES = 1;
    
    private static final String LOGIN_ATTEMPT_PREFIX = "login_attempts:";
    private static final String PASSWORD_RESET_ATTEMPT_PREFIX = "password_reset_attempts:";
    private static final String PASSWORD_RESET_RATE_LIMIT_PREFIX = "password_reset_rate_limit:";
    private static final String CAPTCHA_REQUIRED_PREFIX = "captcha_required:";
    
    /**
     * Registra uma tentativa de login falhada.
     * 
     * @param identifier identificador único (IP, email, etc.)
     * @return número atual de tentativas
     */
    public int recordLoginAttempt(String identifier) {
        String key = LOGIN_ATTEMPT_PREFIX + identifier;
        return recordAttempt(key, identifier, "login");
    }
    
    /**
     * Registra uma tentativa de recuperação de senha falhada.
     * 
     * @param identifier identificador único (IP, email, etc.)
     * @return número atual de tentativas
     */
    public int recordPasswordResetAttempt(String identifier) {
        String key = PASSWORD_RESET_ATTEMPT_PREFIX + identifier;
        return recordAttempt(key, identifier, "password_reset");
    }
    
    /**
     * Verifica se captcha é necessário para login.
     * 
     * @param identifier identificador único
     * @return true se captcha é necessário
     */
    public boolean isCaptchaRequiredForLogin(String identifier) {
        String attemptKey = LOGIN_ATTEMPT_PREFIX + identifier;
        return isCaptchaRequired(attemptKey);
    }
    
    /**
     * Verifica se captcha é necessário para recuperação de senha.
     * 
     * @param identifier identificador único
     * @return true se captcha é necessário
     */
    public boolean isCaptchaRequiredForPasswordReset(String identifier) {
        String attemptKey = PASSWORD_RESET_ATTEMPT_PREFIX + identifier;
        return isCaptchaRequired(attemptKey);
    }
    
    /**
     * Limpa tentativas de login após sucesso.
     * 
     * @param identifier identificador único
     */
    public void clearLoginAttempts(String identifier) {
        String attemptKey = LOGIN_ATTEMPT_PREFIX + identifier;
        String captchaKey = CAPTCHA_REQUIRED_PREFIX + "login:" + identifier;
        
        redisTemplate.delete(attemptKey);
        redisTemplate.delete(captchaKey);
        
        logger.info("Tentativas de login limpas para identificador: {}", identifier);
    }
    
    /**
     * Limpa tentativas de recuperação de senha após sucesso.
     * 
     * @param identifier identificador único
     */
    public void clearPasswordResetAttempts(String identifier) {
        String attemptKey = PASSWORD_RESET_ATTEMPT_PREFIX + identifier;
        String captchaKey = CAPTCHA_REQUIRED_PREFIX + "password_reset:" + identifier;
        
        redisTemplate.delete(attemptKey);
        redisTemplate.delete(captchaKey);
        
        logger.info("Tentativas de recuperação de senha limpas para identificador: {}", identifier);
    }
    
    /**
     * Obtém número atual de tentativas de login.
     * 
     * @param identifier identificador único
     * @return número de tentativas
     */
    public int getLoginAttempts(String identifier) {
        String key = LOGIN_ATTEMPT_PREFIX + identifier;
        return getAttempts(key);
    }
    
    /**
     * Obtém número atual de tentativas de recuperação de senha.
     * 
     * @param identifier identificador único
     * @return número de tentativas
     */
    public int getPasswordResetAttempts(String identifier) {
        String key = PASSWORD_RESET_ATTEMPT_PREFIX + identifier;
        return getAttempts(key);
    }

    /**
     * Verifica se o rate limiting de recuperação de senha está ativo.
     * 
     * @param identifier identificador único (IP)
     * @return true se ainda está no período de rate limiting
     */
    public boolean isPasswordResetRateLimited(String identifier) {
        String key = PASSWORD_RESET_RATE_LIMIT_PREFIX + identifier;
        try {
            Object rateLimitObj = redisTemplate.opsForValue().get(key);
            return rateLimitObj != null;
        } catch (Exception e) {
            logger.error("Erro ao verificar rate limiting para identificador: {}", identifier, e);
            return false;
        }
    }

    /**
     * Registra uma tentativa de recuperação de senha bem-sucedida para rate limiting.
     * 
     * @param identifier identificador único (IP)
     */
    public void recordPasswordResetSuccess(String identifier) {
        String key = PASSWORD_RESET_RATE_LIMIT_PREFIX + identifier;
        try {
            // Registra o rate limiting por 1 minuto
            redisTemplate.opsForValue().set(key, "true", Duration.ofMinutes(PASSWORD_RESET_RATE_LIMIT_MINUTES));
            
            logger.info("Rate limiting de recuperação de senha ativado para identificador: {} por {} minuto(s)", 
                       identifier, PASSWORD_RESET_RATE_LIMIT_MINUTES);
        } catch (Exception e) {
            logger.error("Erro ao registrar rate limiting para identificador: {}", identifier, e);
        }
    }

    /**
     * Obtém o tempo restante do rate limiting em segundos.
     * 
     * @param identifier identificador único (IP)
     * @return tempo restante em segundos, ou 0 se não há rate limiting
     */
    public long getPasswordResetRateLimitRemainingSeconds(String identifier) {
        String key = PASSWORD_RESET_RATE_LIMIT_PREFIX + identifier;
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception e) {
            logger.error("Erro ao obter TTL do rate limiting para identificador: {}", identifier, e);
            return 0;
        }
    }
    
    /**
     * Registra uma tentativa falhada.
     * 
     * @param key chave Redis
     * @param identifier identificador para logs
     * @param type tipo de tentativa (login, password_reset)
     * @return número atual de tentativas
     */
    private int recordAttempt(String key, String identifier, String type) {
        try {
            long attempts = incrementValueWithTTL(key, Duration.ofMinutes(ATTEMPT_EXPIRY_MINUTES));
            
            int currentAttempts = (int) attempts;
            
            logWarn("Tentativa {} falhada para identificador: {} - Total: {}", 
                       type, identifier, currentAttempts);
            
            // Ativa captcha se atingir o limite
            if (currentAttempts >= MAX_ATTEMPTS_BEFORE_CAPTCHA) {
                activateCaptcha(identifier, type);
            }
            
            return currentAttempts;
            
        } catch (Exception e) {
            logError("Erro ao registrar tentativa {} para identificador: {}", type, identifier, e);
            return 0;
        }
    }
    
    /**
     * Verifica se captcha é necessário baseado no número de tentativas.
     * 
     * @param attemptKey chave das tentativas
     * @return true se captcha é necessário
     */
    private boolean isCaptchaRequired(String attemptKey) {
        try {
            int attempts = getIntegerValue(attemptKey);
            return attempts >= MAX_ATTEMPTS_BEFORE_CAPTCHA;
        } catch (Exception e) {
            logError("Erro ao verificar necessidade de captcha para chave: {}", attemptKey, e);
            return false;
        }
    }
    
    /**
     * Obtém número de tentativas.
     * 
     * @param key chave Redis
     * @return número de tentativas
     */
    private int getAttempts(String key) {
        try {
            return getIntegerValue(key);
        } catch (Exception e) {
            logError("Erro ao obter tentativas para chave: {}", key, e);
            return 0;
        }
    }
    
    /**
     * Ativa captcha para um identificador e tipo específico.
     * 
     * @param identifier identificador único
     * @param type tipo de operação
     */
    private void activateCaptcha(String identifier, String type) {
        String captchaKey = CAPTCHA_REQUIRED_PREFIX + type + ":" + identifier;
        
        storeWithTTLMinutes(captchaKey, "true", ATTEMPT_EXPIRY_MINUTES);
        
        logWarn("Captcha ativado para {} - identificador: {} após {} tentativas", 
                   type, identifier, MAX_ATTEMPTS_BEFORE_CAPTCHA);
    }
    
    /**
     * Cria identificador único baseado no IP e email (quando disponível).
     * 
     * @param ipAddress endereço IP
     * @param email email do usuário (opcional)
     * @return identificador único
     */
    public String createIdentifier(String ipAddress, String email) {
        if (email != null && !email.trim().isEmpty()) {
            return ipAddress + ":" + email.toLowerCase().trim();
        }
        return ipAddress;
    }
    
    /**
     * Obtém estatísticas de tentativas para monitoramento.
     * 
     * @param identifier identificador único
     * @return objeto com estatísticas
     */
    public AttemptStatistics getStatistics(String identifier) {
        int loginAttempts = getLoginAttempts(identifier);
        int passwordResetAttempts = getPasswordResetAttempts(identifier);
        boolean loginCaptchaRequired = isCaptchaRequiredForLogin(identifier);
        boolean passwordResetCaptchaRequired = isCaptchaRequiredForPasswordReset(identifier);
        boolean passwordResetRateLimited = isPasswordResetRateLimited(identifier);
        long passwordResetRateLimitRemaining = getPasswordResetRateLimitRemainingSeconds(identifier);
        
        return new AttemptStatistics(
            identifier,
            loginAttempts,
            passwordResetAttempts,
            loginCaptchaRequired,
            passwordResetCaptchaRequired,
            passwordResetRateLimited,
            passwordResetRateLimitRemaining,
            MAX_ATTEMPTS_BEFORE_CAPTCHA
        );
    }
    
    /**
     * Classe para estatísticas de tentativas.
     */
    public static class AttemptStatistics {
        private final String identifier;
        private final int loginAttempts;
        private final int passwordResetAttempts;
        private final boolean loginCaptchaRequired;
        private final boolean passwordResetCaptchaRequired;
        private final boolean passwordResetRateLimited;
        private final long passwordResetRateLimitRemaining;
        private final int maxAttemptsBeforeCaptcha;
        
        public AttemptStatistics(String identifier, int loginAttempts, int passwordResetAttempts,
                               boolean loginCaptchaRequired, boolean passwordResetCaptchaRequired,
                               boolean passwordResetRateLimited, long passwordResetRateLimitRemaining,
                               int maxAttemptsBeforeCaptcha) {
            this.identifier = identifier;
            this.loginAttempts = loginAttempts;
            this.passwordResetAttempts = passwordResetAttempts;
            this.loginCaptchaRequired = loginCaptchaRequired;
            this.passwordResetCaptchaRequired = passwordResetCaptchaRequired;
            this.passwordResetRateLimited = passwordResetRateLimited;
            this.passwordResetRateLimitRemaining = passwordResetRateLimitRemaining;
            this.maxAttemptsBeforeCaptcha = maxAttemptsBeforeCaptcha;
        }
        
        // Getters
        public String getIdentifier() { return identifier; }
        public int getLoginAttempts() { return loginAttempts; }
        public int getPasswordResetAttempts() { return passwordResetAttempts; }
        public boolean isLoginCaptchaRequired() { return loginCaptchaRequired; }
        public boolean isPasswordResetCaptchaRequired() { return passwordResetCaptchaRequired; }
        public boolean isPasswordResetRateLimited() { return passwordResetRateLimited; }
        public long getPasswordResetRateLimitRemaining() { return passwordResetRateLimitRemaining; }
        public int getMaxAttemptsBeforeCaptcha() { return maxAttemptsBeforeCaptcha; }
    }
    
    // ========================================
    // Implementação da Interface AttemptControlOperations
    // ========================================
    
    @Override
    public int recordAttemptControl(String identifier, String type) {
        switch (type.toLowerCase()) {
            case "login":
                return recordLoginAttempt(identifier);
            case "password_reset":
                return recordPasswordResetAttempt(identifier);
            default:
                logWarn("Tipo de tentativa desconhecido: {}", type);
                return 0;
        }
    }
    
    @Override
    public boolean isCaptchaRequiredControl(String identifier, String type) {
        switch (type.toLowerCase()) {
            case "login":
                return isCaptchaRequiredForLogin(identifier);
            case "password_reset":
                return isCaptchaRequiredForPasswordReset(identifier);
            default:
                logWarn("Tipo de verificação de captcha desconhecido: {}", type);
                return false;
        }
    }
    
    @Override
    public void clearAttemptsControl(String identifier, String type) {
        switch (type.toLowerCase()) {
            case "login":
                clearLoginAttempts(identifier);
                break;
            case "password_reset":
                clearPasswordResetAttempts(identifier);
                break;
            default:
                logWarn("Tipo de limpeza de tentativas desconhecido: {}", type);
        }
    }
    
    @Override
    public int getAttemptsControl(String identifier, String type) {
        switch (type.toLowerCase()) {
            case "login":
                return getLoginAttempts(identifier);
            case "password_reset":
                return getPasswordResetAttempts(identifier);
            default:
                logWarn("Tipo de obtenção de tentativas desconhecido: {}", type);
                return 0;
        }
    }
    
    @Override
    public boolean isRateLimitedControl(String identifier, String type) {
        if ("password_reset".equalsIgnoreCase(type)) {
            return isPasswordResetRateLimited(identifier);
        }
        // Para outros tipos, não há rate limiting implementado
        return false;
    }
    
    @Override
    public void recordSuccessControl(String identifier, String type) {
        switch (type.toLowerCase()) {
            case "login":
                clearLoginAttempts(identifier);
                break;
            case "password_reset":
                recordPasswordResetSuccess(identifier);
                break;
            default:
                logWarn("Tipo de registro de sucesso desconhecido: {}", type);
        }
    }
    
    @Override
    public long getRateLimitRemainingSecondsControl(String identifier, String type) {
        if ("password_reset".equalsIgnoreCase(type)) {
            return getPasswordResetRateLimitRemainingSeconds(identifier);
        }
        // Para outros tipos, não há rate limiting
        return 0;
    }
    
    @Override
    public String createIdentifierControl(String ipAddress, String email) {
        return createIdentifier(ipAddress, email);
    }
}