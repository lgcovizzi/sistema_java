package com.sistema.service.interfaces;

import java.util.Map;

/**
 * Interface para operações de captcha padronizadas.
 * Define métodos comuns para geração, validação e gerenciamento de captchas.
 */
public interface CaptchaOperations {
    
    /**
     * Gera um novo captcha.
     * 
     * @return mapa contendo ID do captcha e imagem em base64
     */
    Map<String, String> generateCaptcha();
    
    /**
     * Valida uma resposta de captcha.
     * 
     * @param captchaId ID do captcha
     * @param userResponse resposta do usuário
     * @return true se válido
     */
    boolean validateCaptcha(String captchaId, String userResponse);
    
    /**
     * Verifica se um captcha existe e está válido.
     * 
     * @param captchaId ID do captcha
     * @return true se existe
     */
    boolean captchaExists(String captchaId);
    
    /**
     * Remove um captcha do cache.
     * 
     * @param captchaId ID do captcha
     * @return true se removido com sucesso
     */
    boolean removeCaptcha(String captchaId);
    
    /**
     * Gera um captcha com configurações específicas.
     * 
     * @param width largura da imagem
     * @param height altura da imagem
     * @param textLength comprimento do texto
     * @return mapa contendo ID do captcha e imagem em base64
     */
    Map<String, String> generateCaptcha(int width, int height, int textLength);
    
    /**
     * Obtém configurações padrão do captcha.
     * 
     * @return mapa com configurações
     */
    Map<String, Object> getDefaultCaptchaConfig();
    
    /**
     * Obtém tempo de vida padrão do captcha em minutos.
     * 
     * @return minutos de TTL
     */
    int getCaptchaTTLMinutes();
    
    /**
     * Limpa captchas expirados.
     * 
     * @return número de captchas removidos
     */
    long cleanupExpiredCaptchas();
    
    /**
     * Obtém estatísticas de uso de captcha.
     * 
     * @return mapa com estatísticas
     */
    Map<String, Object> getCaptchaStatistics();
    
    /**
     * Valida formato do ID do captcha.
     * 
     * @param captchaId ID a ser validado
     * @return true se formato válido
     */
    boolean isValidCaptchaId(String captchaId);
    
    /**
     * Valida formato da resposta do captcha.
     * 
     * @param userResponse resposta a ser validada
     * @return true se formato válido
     */
    boolean isValidCaptchaResponse(String userResponse);
}