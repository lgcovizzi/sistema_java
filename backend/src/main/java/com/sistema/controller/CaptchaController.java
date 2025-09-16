package com.sistema.controller;

import com.sistema.service.CaptchaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller para endpoints de captcha.
 */
@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {
    
    private static final Logger logger = LoggerFactory.getLogger(CaptchaController.class);
    
    @Autowired
    private CaptchaService captchaService;
    
    /**
     * Gera um novo captcha.
     * 
     * @return dados do captcha (ID e imagem em base64)
     */
    @GetMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateCaptcha() {
        try {
            CaptchaService.CaptchaData captchaData = captchaService.generateCaptcha();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("captchaId", captchaData.getId());
            response.put("imageDataUrl", "data:image/png;base64," + captchaData.getImageBase64());
            
            logger.debug("Captcha gerado via API: {}", captchaData.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao gerar captcha via API", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Erro interno do servidor");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Gera um novo captcha e retorna a imagem diretamente.
     * 
     * @return imagem PNG do captcha
     */
    @GetMapping(value = "/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateCaptchaImage() {
        try {
            CaptchaService.CaptchaData captchaData = captchaService.generateCaptcha();
            
            // Decodificar base64 para bytes
            byte[] imageBytes = Base64.getDecoder().decode(captchaData.getImageBase64());
            
            // Adicionar ID do captcha no header para o frontend usar
            return ResponseEntity.ok()
                .header("X-Captcha-Id", captchaData.getId())
                .body(imageBytes);
            
        } catch (Exception e) {
            logger.error("Erro ao gerar imagem de captcha via API", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Valida um captcha.
     * 
     * @param request dados de validação
     * @return resultado da validação
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateCaptcha(@RequestBody CaptchaValidationRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (request.getCaptchaId() == null || request.getAnswer() == null) {
                response.put("success", false);
                response.put("error", "Parâmetros obrigatórios não fornecidos");
                return ResponseEntity.badRequest().body(response);
            }
            
            boolean isValid = captchaService.validateCaptcha(request.getCaptchaId(), request.getAnswer());
            
            response.put("success", true);
            response.put("valid", isValid);
            
            if (isValid) {
                logger.debug("Captcha validado com sucesso via API: {}", request.getCaptchaId());
            } else {
                logger.warn("Captcha inválido via API: {}", request.getCaptchaId());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao validar captcha via API", e);
            
            response.put("success", false);
            response.put("error", "Erro interno do servidor");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Verifica se um captcha existe.
     * 
     * @param captchaId ID do captcha
     * @return status de existência
     */
    @GetMapping("/exists/{captchaId}")
    public ResponseEntity<Map<String, Object>> checkCaptchaExists(@PathVariable String captchaId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean exists = captchaService.captchaExists(captchaId);
            
            response.put("success", true);
            response.put("exists", exists);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao verificar existência de captcha via API", e);
            
            response.put("success", false);
            response.put("error", "Erro interno do servidor");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Obtém estatísticas de captchas.
     * 
     * @return estatísticas
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getCaptchaStatistics() {
        try {
            CaptchaService.CaptchaStatistics stats = captchaService.getStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("activeCaptchas", stats.getActiveCaptchas());
            response.put("expiryMinutes", stats.getExpiryMinutes());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao obter estatísticas de captcha via API", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Erro interno do servidor");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Remove um captcha específico.
     * 
     * @param captchaId ID do captcha
     * @return confirmação de remoção
     */
    @DeleteMapping("/{captchaId}")
    public ResponseEntity<Map<String, Object>> removeCaptcha(@PathVariable String captchaId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            captchaService.removeCaptcha(captchaId);
            
            response.put("success", true);
            response.put("message", "Captcha removido com sucesso");
            
            logger.debug("Captcha removido via API: {}", captchaId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao remover captcha via API", e);
            
            response.put("success", false);
            response.put("error", "Erro interno do servidor");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Limpa captchas expirados.
     * 
     * @return confirmação de limpeza
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupExpiredCaptchas() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            long removedCount = captchaService.cleanupExpiredCaptchas();
            
            response.put("success", true);
            response.put("message", "Limpeza de captchas expirados concluída");
            response.put("removedCount", removedCount);
            
            logger.info("Limpeza de captchas executada via API");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao executar limpeza de captchas via API", e);
            
            response.put("success", false);
            response.put("error", "Erro interno do servidor");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Classe para request de validação de captcha.
     */
    public static class CaptchaValidationRequest {
        private String captchaId;
        private String answer;
        
        public CaptchaValidationRequest() {}
        
        public CaptchaValidationRequest(String captchaId, String answer) {
            this.captchaId = captchaId;
            this.answer = answer;
        }
        
        public String getCaptchaId() { return captchaId; }
        public void setCaptchaId(String captchaId) { this.captchaId = captchaId; }
        
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
    }
}