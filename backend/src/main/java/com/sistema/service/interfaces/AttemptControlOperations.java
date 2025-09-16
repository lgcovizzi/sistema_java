package com.sistema.service.interfaces;

/**
 * Interface para operações de controle de tentativas padronizadas.
 * Define métodos comuns para controle de tentativas de login e operações sensíveis.
 */
public interface AttemptControlOperations {
    
    /**
     * Registra uma tentativa de login falhada.
     * 
     * @param identifier identificador único (IP, email, etc.)
     * @return número atual de tentativas
     */
    int recordLoginAttempt(String identifier);
    
    /**
     * Registra uma tentativa de operação sensível falhada.
     * 
     * @param identifier identificador único
     * @param operationType tipo de operação (password_reset, etc.)
     * @return número atual de tentativas
     */
    int recordOperationAttempt(String identifier, String operationType);
    
    /**
     * Verifica se captcha é necessário para login.
     * 
     * @param identifier identificador único
     * @return true se captcha é necessário
     */
    boolean isCaptchaRequiredForLogin(String identifier);
    
    /**
     * Verifica se captcha é necessário para uma operação específica.
     * 
     * @param identifier identificador único
     * @param operationType tipo de operação
     * @return true se captcha é necessário
     */
    boolean isCaptchaRequiredForOperation(String identifier, String operationType);
    
    /**
     * Limpa tentativas de login após sucesso.
     * 
     * @param identifier identificador único
     */
    void clearLoginAttempts(String identifier);
    
    /**
     * Limpa tentativas de operação após sucesso.
     * 
     * @param identifier identificador único
     * @param operationType tipo de operação
     */
    void clearOperationAttempts(String identifier, String operationType);
    
    /**
     * Obtém número atual de tentativas de login.
     * 
     * @param identifier identificador único
     * @return número de tentativas
     */
    int getLoginAttempts(String identifier);
    
    /**
     * Obtém número atual de tentativas de operação.
     * 
     * @param identifier identificador único
     * @param operationType tipo de operação
     * @return número de tentativas
     */
    int getOperationAttempts(String identifier, String operationType);
    
    /**
     * Verifica se uma operação está limitada por rate limiting.
     * 
     * @param identifier identificador único
     * @param operationType tipo de operação
     * @return true se limitada
     */
    boolean isOperationRateLimited(String identifier, String operationType);
    
    /**
     * Registra uma operação bem-sucedida para rate limiting.
     * 
     * @param identifier identificador único
     * @param operationType tipo de operação
     */
    void recordOperationSuccess(String identifier, String operationType);
    
    /**
     * Obtém tempo restante de rate limiting em segundos.
     * 
     * @param identifier identificador único
     * @param operationType tipo de operação
     * @return segundos restantes
     */
    long getRateLimitRemainingSeconds(String identifier, String operationType);
    
    /**
     * Cria identificador único baseado em múltiplos fatores.
     * 
     * @param ipAddress endereço IP
     * @param additionalInfo informação adicional (email, username)
     * @return identificador único
     */
    String createIdentifier(String ipAddress, String additionalInfo);
    
    /**
     * Obtém estatísticas de tentativas para um identificador.
     * 
     * @param identifier identificador único
     * @return objeto com estatísticas
     */
    AttemptStatistics getAttemptStatistics(String identifier);
    
    /**
     * Obtém limite máximo de tentativas antes de ativar captcha.
     * 
     * @return número máximo de tentativas
     */
    int getMaxAttemptsBeforeCaptcha();
    
    /**
     * Classe para estatísticas de tentativas.
     */
    class AttemptStatistics {
        private final String identifier;
        private final int loginAttempts;
        private final int operationAttempts;
        private final boolean loginCaptchaRequired;
        private final boolean operationCaptchaRequired;
        private final boolean operationRateLimited;
        private final long rateLimitRemaining;
        private final int maxAttemptsBeforeCaptcha;
        
        public AttemptStatistics(String identifier, int loginAttempts, int operationAttempts,
                               boolean loginCaptchaRequired, boolean operationCaptchaRequired,
                               boolean operationRateLimited, long rateLimitRemaining,
                               int maxAttemptsBeforeCaptcha) {
            this.identifier = identifier;
            this.loginAttempts = loginAttempts;
            this.operationAttempts = operationAttempts;
            this.loginCaptchaRequired = loginCaptchaRequired;
            this.operationCaptchaRequired = operationCaptchaRequired;
            this.operationRateLimited = operationRateLimited;
            this.rateLimitRemaining = rateLimitRemaining;
            this.maxAttemptsBeforeCaptcha = maxAttemptsBeforeCaptcha;
        }
        
        // Getters
        public String getIdentifier() { return identifier; }
        public int getLoginAttempts() { return loginAttempts; }
        public int getOperationAttempts() { return operationAttempts; }
        public boolean isLoginCaptchaRequired() { return loginCaptchaRequired; }
        public boolean isOperationCaptchaRequired() { return operationCaptchaRequired; }
        public boolean isOperationRateLimited() { return operationRateLimited; }
        public long getRateLimitRemaining() { return rateLimitRemaining; }
        public int getMaxAttemptsBeforeCaptcha() { return maxAttemptsBeforeCaptcha; }
    }
}