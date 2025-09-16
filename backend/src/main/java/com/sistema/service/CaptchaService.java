package com.sistema.service;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import com.sistema.service.base.BaseRedisService;
import com.sistema.service.interfaces.CaptchaOperations;
import com.sistema.util.SecurityUtils;
import com.sistema.util.ValidationUtils;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * Serviço para geração e validação de captchas usando Kaptcha.
 * Estende BaseRedisService para reutilizar operações Redis.
 * Implementa CaptchaOperations para padronizar operações de captcha.
 */
@Service
public class CaptchaService extends BaseRedisService implements CaptchaOperations {
    
    private static final int CAPTCHA_EXPIRY_MINUTES = 10;
    private static final String CAPTCHA_PREFIX = "captcha:";
    
    private final DefaultKaptcha kaptcha;
    
    public CaptchaService() {
        this.kaptcha = new DefaultKaptcha();
        
        Properties properties = new Properties();
        properties.setProperty("kaptcha.image.width", "200");
        properties.setProperty("kaptcha.image.height", "50");
        properties.setProperty("kaptcha.textproducer.char.string", "ABCDEFGHJKLMNPQRSTUVWXYZ23456789");
        properties.setProperty("kaptcha.textproducer.char.length", "5");
        properties.setProperty("kaptcha.textproducer.font.names", "Arial");
        properties.setProperty("kaptcha.textproducer.font.size", "40");
        properties.setProperty("kaptcha.textproducer.font.color", "black");
        properties.setProperty("kaptcha.textproducer.char.space", "5");
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.DefaultNoise");
        properties.setProperty("kaptcha.noise.color", "blue");
        properties.setProperty("kaptcha.background.clear.from", "lightGray");
        properties.setProperty("kaptcha.background.clear.to", "white");
        properties.setProperty("kaptcha.border", "yes");
        properties.setProperty("kaptcha.border.color", "black");
        
        Config config = new Config(properties);
        kaptcha.setConfig(config);
    }
    
    /**
     * Gera um novo captcha.
     * 
     * @return objeto CaptchaData com ID e imagem em base64
     */
    public CaptchaData generateCaptcha() {
        try {
            // Gerar texto do captcha
            String captchaText = kaptcha.createText();
            
            // Gerar imagem do captcha
            BufferedImage captchaImage = kaptcha.createImage(captchaText);
            
            // Converter imagem para base64
            String base64Image = convertImageToBase64(captchaImage);
            
            // Gerar ID único para o captcha usando utilitário de segurança
            String captchaId = SecurityUtils.generateSecureToken(32);
            
            // Armazenar hash da resposta no Redis com TTL usando método da classe base
            String hashedAnswer = SecurityUtils.hashSHA256(captchaText.toLowerCase());
            String key = CAPTCHA_PREFIX + captchaId;
            storeWithTTL(key, hashedAnswer, Duration.ofMinutes(CAPTCHA_EXPIRY_MINUTES));
            
            logger.info("Captcha gerado com ID: {}", captchaId);
            
            return new CaptchaData(captchaId, base64Image);
            
        } catch (Exception e) {
            logger.error("Erro ao gerar captcha", e);
            throw new RuntimeException("Erro ao gerar captcha", e);
        }
    }
    
    /**
     * Valida uma resposta de captcha.
     * 
     * @param captchaId ID do captcha
     * @param userAnswer resposta do usuário
     * @return true se a resposta estiver correta
     */
    public boolean validateCaptcha(String captchaId, String userAnswer) {
        try {
            // Validar entrada usando utilitários
            ValidationUtils.validateNotBlank(captchaId, "ID do captcha é obrigatório");
            ValidationUtils.validateNotBlank(userAnswer, "Resposta do captcha é obrigatória");
            
            String key = CAPTCHA_PREFIX + captchaId;
            String storedHash = getValue(key);
            
            if (storedHash == null) {
                logger.warn("Captcha não encontrado ou expirado: {}", captchaId);
                return false;
            }
            
            // Comparar hash da resposta do usuário com o hash armazenado
            String userAnswerHash = SecurityUtils.hashSHA256(userAnswer.trim().toLowerCase());
            boolean isValid = storedHash.equals(userAnswerHash);
            
            if (isValid) {
                // Remover captcha após validação bem-sucedida (uso único)
                removeKey(key);
                logger.info("Captcha validado com sucesso: {}", captchaId);
            } else {
                logger.warn("Resposta incorreta para captcha: {}", captchaId);
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.error("Erro ao validar captcha: {}", captchaId, e);
            return false;
        }
    }
    
    /**
     * Verifica se um captcha existe e não expirou.
     * 
     * @param captchaId ID do captcha
     * @return true se o captcha existir
     */
    public boolean captchaExists(String captchaId) {
        try {
            ValidationUtils.validateNotBlank(captchaId, "ID do captcha é obrigatório");
            String key = CAPTCHA_PREFIX + captchaId;
            return keyExists(key);
        } catch (Exception e) {
            logger.error("Erro ao verificar existência do captcha: {}", captchaId, e);
            return false;
        }
    }
    
    /**
     * Remove captchas expirados (limpeza manual).
     * O Redis já remove automaticamente com TTL, mas este método pode ser usado para limpeza forçada.
     * 
     * @return número de captchas removidos
     */
    public long cleanupExpiredCaptchas() {
        try {
            // O Redis já remove automaticamente com TTL
            // Este método é mantido para compatibilidade
            logger.info("Limpeza de captchas executada (TTL automático do Redis)");
            return 0;
        } catch (Exception e) {
            logger.error("Erro na limpeza de captchas", e);
            return 0;
        }
    }
    
    /**
     * Obtém estatísticas de captchas.
     * 
     * @return estatísticas dos captchas
     */
    public CaptchaStatistics getStatistics() {
        try {
            // Contar captchas ativos
            String pattern = CAPTCHA_PREFIX + "*";
            long activeCaptchas = redisTemplate.keys(pattern).size();
            
            return new CaptchaStatistics(activeCaptchas, CAPTCHA_EXPIRY_MINUTES);
            
        } catch (Exception e) {
            logger.error("Erro ao obter estatísticas de captchas", e);
            return new CaptchaStatistics(0, CAPTCHA_EXPIRY_MINUTES);
        }
    }
    
    /**
     * Converte uma imagem BufferedImage para string base64.
     */
    private String convertImageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
    

    
    /**
     * Classe para dados do captcha.
     */
    public static class CaptchaData {
        private final String id;
        private final String imageBase64;
        
        public CaptchaData(String id, String imageBase64) {
            this.id = id;
            this.imageBase64 = imageBase64;
        }
        
        public String getId() { return id; }
        public String getImageBase64() { return imageBase64; }
    }
    
    /**
     * Classe para estatísticas de captchas.
     */
    public static class CaptchaStatistics {
        private final long activeCaptchas;
        private final int expiryMinutes;
        
        public CaptchaStatistics(long activeCaptchas, int expiryMinutes) {
            this.activeCaptchas = activeCaptchas;
            this.expiryMinutes = expiryMinutes;
        }
        
        public long getActiveCaptchas() { return activeCaptchas; }
        public int getExpiryMinutes() {
            return expiryMinutes;
        }
    }
    
    // Implementação da interface CaptchaOperations
    
    @Override
    public CaptchaData createCaptcha() {
        return generateCaptcha();
    }
    
    @Override
    public boolean verifyCaptcha(String captchaId, String userInput) {
        return validateCaptcha(captchaId, userInput);
    }
    
    @Override
    public boolean isCaptchaValid(String captchaId) {
        return captchaExists(captchaId);
    }
    
    @Override
    public void invalidateCaptcha(String captchaId) {
        try {
            String key = CAPTCHA_PREFIX + captchaId;
            removeKey(key);
            logInfo("Captcha invalidado: {}", captchaId);
        } catch (Exception e) {
            logError("Erro ao invalidar captcha: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, Object> getCaptchaConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("expiryMinutes", CAPTCHA_EXPIRY_MINUTES);
        config.put("imageWidth", 200);
        config.put("imageHeight", 50);
        config.put("textLength", 5);
        config.put("fontName", "Arial");
        config.put("fontSize", 40);
        return config;
    }
    
    @Override
    public void updateCaptchaConfiguration(Map<String, Object> newConfig) {
        // Para esta implementação, a configuração é estática
        // Em uma implementação mais avançada, poderia ser dinâmica
        logWarn("Atualização de configuração de captcha não suportada nesta implementação");
    }
    
    @Override
    public long cleanupExpiredCaptchas() {
        return cleanupExpiredCaptchas();
    }
    
    @Override
    public Map<String, Object> getCaptchaStatistics() {
        CaptchaStatistics stats = getStatistics();
        Map<String, Object> result = new HashMap<>();
        result.put("activeCaptchas", stats.getActiveCaptchas());
        result.put("expiryMinutes", stats.getExpiryMinutes());
        return result;
    }
}