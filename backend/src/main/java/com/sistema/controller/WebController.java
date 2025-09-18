package com.sistema.controller;

import com.sistema.service.ToastService;
import com.sistema.service.EmailVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {

    private static final Logger logger = LoggerFactory.getLogger(WebController.class);

    @Autowired
    private ToastService toastService;
    
    @Autowired
    private EmailVerificationService emailVerificationService;

    @GetMapping("/web")
    public String index(Model model) {
        model.addAttribute("appName", "Sistema Java");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("status", "Online");
        return "index";
    }
    
    @GetMapping("/toast-demo")
    public String toastDemo(Model model) {
        model.addAttribute("appName", "Sistema Java");
        model.addAttribute("version", "1.0.0");
        
        // Adiciona mensagens toast para demonstração
        model.addAttribute("toastMessages", toastService.getAndClearMessages());
        
        return "toast-demo";
    }
    
    /**
     * Página de verificação de email através de token.
     * 
     * @param token token de verificação
     * @param model modelo para o template
     * @return nome do template
     */
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, Model model) {
        try {
            logger.info("Tentativa de verificação de email com token: {}", token.substring(0, Math.min(token.length(), 10)) + "...");
            
            boolean verified = emailVerificationService.verifyEmailToken(token);
            
            if (verified) {
                model.addAttribute("success", true);
                model.addAttribute("appName", "Sistema Java");
                logger.info("Email verificado com sucesso para token: {}", token.substring(0, Math.min(token.length(), 10)) + "...");
            } else {
                model.addAttribute("error", true);
                model.addAttribute("errorMessage", "Token de verificação inválido ou expirado");
                model.addAttribute("appName", "Sistema Java");
                logger.warn("Token de verificação inválido para: {}", token.substring(0, Math.min(token.length(), 10)) + "...");
            }
            
        } catch (Exception e) {
            logger.error("Erro ao verificar email", e);
            model.addAttribute("error", true);
            model.addAttribute("errorMessage", "Erro interno do servidor. Tente novamente mais tarde.");
            model.addAttribute("appName", "Sistema Java");
        }
        
        return "email-verification-result";
    }

}