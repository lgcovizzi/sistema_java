package com.sistema.java.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Entry point para tratamento de tentativas de acesso não autorizadas
 * Referência: Controle de Acesso - project_rules.md
 * Referência: Segurança - project_rules.md
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Método chamado quando uma requisição não autenticada tenta acessar um recurso protegido
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param request Requisição HTTP
     * @param response Resposta HTTP
     * @param authException Exceção de autenticação
     * @throws IOException Se houver erro na escrita da resposta
     */
    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException {
        
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String remoteAddr = getClientIpAddress(request);
        
        logger.warn("Tentativa de acesso não autorizado: {} {} de IP: {} - Motivo: {}", 
                   method, requestURI, remoteAddr, authException.getMessage());
        
        // Configurar resposta HTTP
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        
        // Criar corpo da resposta de erro
        Map<String, Object> errorResponse = createErrorResponse(request, authException);
        
        // Escrever resposta JSON
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
    
    /**
     * Cria o corpo da resposta de erro
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param request Requisição HTTP
     * @param authException Exceção de autenticação
     * @return Map com detalhes do erro
     */
    private Map<String, Object> createErrorResponse(HttpServletRequest request, 
                                                   AuthenticationException authException) {
        Map<String, Object> errorResponse = new HashMap<>();
        
        errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", "Acesso negado. Autenticação necessária.");
        errorResponse.put("path", request.getRequestURI());
        errorResponse.put("method", request.getMethod());
        
        // Adicionar detalhes específicos baseados no tipo de erro
        String detailedMessage = getDetailedMessage(authException, request);
        if (detailedMessage != null) {
            errorResponse.put("details", detailedMessage);
        }
        
        // Adicionar informações de como proceder
        errorResponse.put("action", getRecommendedAction(request));
        
        return errorResponse;
    }
    
    /**
     * Obtém mensagem detalhada baseada no tipo de exceção
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param authException Exceção de autenticação
     * @param request Requisição HTTP
     * @return Mensagem detalhada ou null
     */
    private String getDetailedMessage(AuthenticationException authException, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || authHeader.trim().isEmpty()) {
            return "Token de autenticação não fornecido. Inclua o header 'Authorization: Bearer <token>'";
        }
        
        if (!authHeader.startsWith("Bearer ")) {
            return "Formato de token inválido. Use 'Authorization: Bearer <token>'";
        }
        
        if (authException.getMessage().contains("expired")) {
            return "Token de autenticação expirado. Faça login novamente.";
        }
        
        if (authException.getMessage().contains("invalid")) {
            return "Token de autenticação inválido. Verifique se o token está correto.";
        }
        
        return "Falha na autenticação. Verifique suas credenciais.";
    }
    
    /**
     * Obtém ação recomendada baseada no endpoint acessado
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param request Requisição HTTP
     * @return Ação recomendada
     */
    private String getRecommendedAction(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        if (path.startsWith("/api/admin/")) {
            return "Este endpoint requer privilégios de ADMINISTRADOR. Faça login com uma conta administrativa.";
        }
        
        if (path.startsWith("/api/fundador/")) {
            return "Este endpoint requer privilégios de FUNDADOR ou ADMINISTRADOR.";
        }
        
        if (path.startsWith("/api/colaborador/")) {
            return "Este endpoint requer privilégios de COLABORADOR ou superior.";
        }
        
        if (path.startsWith("/api/parceiro/")) {
            return "Este endpoint requer privilégios de PARCEIRO ou superior.";
        }
        
        if (path.startsWith("/api/associado/")) {
            return "Este endpoint requer privilégios de ASSOCIADO ou superior.";
        }
        
        if (path.startsWith("/api/")) {
            return "Faça login em /api/auth/login para obter um token de acesso.";
        }
        
        return "Acesse /api/auth/login para autenticar-se no sistema.";
    }
    
    /**
     * Obtém o endereço IP real do cliente, considerando proxies
     * Referência: Segurança - project_rules.md
     * 
     * @param request Requisição HTTP
     * @return Endereço IP do cliente
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}