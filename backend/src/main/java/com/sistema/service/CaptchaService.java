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
    public CaptchaData generateCaptchaData() {
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
    public boolean validateCaptchaInternal(String captchaId, String userAnswer) {
        try {
            // Validar entrada usando utilitários
            ValidationUtils.validateNotBlank(captchaId, "ID do captcha é obrigatório");
            ValidationUtils.validateNotBlank(userAnswer, "Resposta do captcha é obrigatória");
            
            String key = CAPTCHA_PREFIX + captchaId;
            String storedHash = getStringValue(key);
            
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
    public boolean captchaExistsInternal(String captchaId) {
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
    public long cleanupExpiredCaptchasInternal() {
        try {
            return cleanupKeysByPattern(CAPTCHA_PREFIX + "*");
        } catch (Exception e) {
            logger.error("Erro ao limpar captchas expirados", e);
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
    
    /**
     * Remove um captcha específico.
     * 
     * @param captchaId ID do captcha
     * @return true se removido com sucesso
     */
    public boolean removeCaptchaInternal(String captchaId) {
        try {
            ValidationUtils.validateNotBlank(captchaId, "ID do captcha é obrigatório");
            String key = CAPTCHA_PREFIX + captchaId;
            boolean existed = keyExists(key);
            if (existed) {
                removeKey(key);
                logger.info("Captcha removido: {}", captchaId);
            }
            return existed;
        } catch (Exception e) {
            logger.error("Erro ao remover captcha: {}", captchaId, e);
            return false;
        }
    }
    
    // ========================================
    // Implementação da Interface CaptchaOperations
    // ========================================
    
    @Override
    public Map<String, String> generateCaptcha() {
        CaptchaData data = generateCaptchaData();
        Map<String, String> result = new HashMap<>();
        result.put("id", data.getId());
        result.put("imageBase64", data.getImageBase64());
        return result;
    }
    
    /**
     * Gera um captcha de teste com resposta conhecida (apenas para desenvolvimento).
     * 
     * @return dados do captcha de teste incluindo a resposta
     */
    public Map<String, String> generateTestCaptcha() {
        try {
            // Resposta conhecida para teste
            String testAnswer = "TEST1";
            String captchaId = UUID.randomUUID().toString().replace("-", "");
            
            // Criar imagem simples para teste
            BufferedImage image = new BufferedImage(200, 50, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2d = image.createGraphics();
            
            // Fundo branco
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, 200, 50);
            
            // Texto preto
            g2d.setColor(java.awt.Color.BLACK);
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 20));
            g2d.drawString(testAnswer, 70, 30);
            
            // Borda
            g2d.setColor(java.awt.Color.BLACK);
            g2d.drawRect(0, 0, 199, 49);
            
            g2d.dispose();
            
            // Converter para base64
            String base64Image = convertImageToBase64(image);
            
            // Armazenar hash da resposta no Redis
            String key = CAPTCHA_PREFIX + captchaId;
            String hashedAnswer = SecurityUtils.hashSHA256(testAnswer.toLowerCase());
            storeWithTTL(key, hashedAnswer, Duration.ofMinutes(CAPTCHA_EXPIRY_MINUTES));
            
            logger.info("Captcha de teste gerado com ID: {} e resposta: {}", captchaId, testAnswer);
            
            Map<String, String> result = new HashMap<>();
            result.put("id", captchaId);
            result.put("imageBase64", base64Image);
            result.put("answer", testAnswer);
            return result;
            
        } catch (Exception e) {
            logger.error("Erro ao gerar captcha de teste", e);
            throw new RuntimeException("Erro ao gerar captcha de teste", e);
        }
    }
    
    @Override
    public boolean validateCaptcha(String captchaId, String userResponse) {
        return validateCaptchaInternal(captchaId, userResponse);
    }
    
    @Override
    public boolean captchaExists(String captchaId) {
        return captchaExistsInternal(captchaId);
    }
    
    @Override
    public boolean removeCaptcha(String captchaId) {
        return removeCaptchaInternal(captchaId);
    }
    
    @Override
    public Map<String, String> generateCaptcha(int width, int height, int textLength) {
        // Para esta implementação, usa configurações padrão
        // Em uma implementação mais avançada, poderia ser dinâmica
        logger.warn("Geração de captcha com configurações customizadas não suportada - usando padrão");
        return generateCaptcha();
    }
    
    @Override
    public Map<String, Object> getDefaultCaptchaConfig() {
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
    public int getCaptchaTTLMinutes() {
        return CAPTCHA_EXPIRY_MINUTES;
    }
    
    @Override
    public long cleanupExpiredCaptchas() {
        return cleanupExpiredCaptchasInternal();
    }
    
    @Override
    public Map<String, Object> getCaptchaStatistics() {
        CaptchaStatistics stats = getStatistics();
        Map<String, Object> result = new HashMap<>();
        result.put("activeCaptchas", stats.getActiveCaptchas());
        result.put("expiryMinutes", stats.getExpiryMinutes());
        return result;
    }
    
    @Override
    public boolean isValidCaptchaId(String captchaId) {
        try {
            return captchaId != null && !captchaId.trim().isEmpty() && captchaId.length() >= 16;
        } catch (Exception e) {
            logger.error("Erro ao validar ID do captcha: {}", captchaId, e);
            return false;
        }
    }
    
    @Override
    public boolean isValidCaptchaResponse(String userResponse) {
        try {
            return userResponse != null && !userResponse.trim().isEmpty() && 
                   userResponse.trim().length() >= 3 && userResponse.trim().length() <= 10;
        } catch (Exception e) {
            logger.error("Erro ao validar resposta do captcha", e);
            return false;
        }
    }
}