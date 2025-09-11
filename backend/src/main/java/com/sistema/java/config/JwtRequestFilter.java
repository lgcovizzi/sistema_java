package com.sistema.java.config;

import com.sistema.java.service.UserDetailsServiceImpl;
import com.sistema.java.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro para interceptar requisições e validar tokens JWT
 * Referência: Controle de Acesso - project_rules.md
 * Referência: Segurança - project_rules.md
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtil jwtUtil;
    
    public JwtRequestFilter(UserDetailsServiceImpl userDetailsService, JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }
    
    /**
     * Filtra cada requisição para validar token JWT
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param request Requisição HTTP
     * @param response Resposta HTTP
     * @param chain Cadeia de filtros
     * @throws ServletException Se houver erro no servlet
     * @throws IOException Se houver erro de I/O
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain chain) throws ServletException, IOException {
        
        final String requestTokenHeader = request.getHeader(AUTHORIZATION_HEADER);
        final String requestURI = request.getRequestURI();
        final String method = request.getMethod();
        
        String username = null;
        String jwtToken = null;
        
        // Verificar se o header Authorization está presente e tem o formato correto
        if (requestTokenHeader != null && requestTokenHeader.startsWith(BEARER_PREFIX)) {
            jwtToken = requestTokenHeader.substring(BEARER_PREFIX.length());
            
            try {
                username = jwtUtil.getUsernameFromToken(jwtToken);
                logger.debug("Token JWT encontrado para usuário: {} na requisição: {} {}", username, method, requestURI);
                
            } catch (IllegalArgumentException e) {
                logger.warn("Não foi possível obter username do token JWT: {}", e.getMessage());
            } catch (ExpiredJwtException e) {
                logger.warn("Token JWT expirado para requisição: {} {} - Expirou em: {}", method, requestURI, e.getClaims().getExpiration());
            } catch (MalformedJwtException e) {
                logger.warn("Token JWT malformado na requisição: {} {} - {}", method, requestURI, e.getMessage());
            } catch (UnsupportedJwtException e) {
                logger.warn("Token JWT não suportado na requisição: {} {} - {}", method, requestURI, e.getMessage());
            } catch (SignatureException e) {
                logger.warn("Assinatura do token JWT inválida na requisição: {} {} - {}", method, requestURI, e.getMessage());
            } catch (Exception e) {
                logger.error("Erro inesperado ao processar token JWT na requisição: {} {} - {}", method, requestURI, e.getMessage(), e);
            }
        } else if (requiresAuthentication(requestURI)) {
            logger.debug("Header Authorization não encontrado ou formato inválido para requisição protegida: {} {}", method, requestURI);
        }
        
        // Validar token e configurar contexto de segurança
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // Validar se o token é válido para este usuário
                if (jwtUtil.validateToken(jwtToken, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, 
                            null, 
                            userDetails.getAuthorities()
                        );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    logger.debug("Usuário autenticado com sucesso: {} para requisição: {} {}", username, method, requestURI);
                } else {
                    logger.warn("Token JWT inválido para usuário: {} na requisição: {} {}", username, method, requestURI);
                }
                
            } catch (Exception e) {
                logger.error("Erro ao validar token JWT para usuário: {} na requisição: {} {} - {}", username, method, requestURI, e.getMessage(), e);
            }
        }
        
        // Continuar com a cadeia de filtros
        chain.doFilter(request, response);
    }
    
    /**
     * Verifica se a requisição requer autenticação
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param requestURI URI da requisição
     * @return true se requer autenticação
     */
    private boolean requiresAuthentication(String requestURI) {
        // Endpoints públicos que não requerem autenticação
        String[] publicEndpoints = {
            "/api/auth/login",
            "/api/auth/registro",
            "/api/noticias/publicas",
            "/api/email/mailhog-info",
            "/actuator/health",
            "/error",
            "/resources/",
            "/static/",
            "/css/",
            "/js/",
            "/images/",
            "/javax.faces.resource/",
            "/",
            "/index.xhtml",
            "/pages/publico/"
        };
        
        for (String endpoint : publicEndpoints) {
            if (requestURI.startsWith(endpoint)) {
                return false;
            }
        }
        
        // Se não é um endpoint público, requer autenticação
        return requestURI.startsWith("/api/") || 
               requestURI.startsWith("/pages/dashboard/") ||
               requestURI.startsWith("/pages/perfil/") ||
               requestURI.startsWith("/pages/admin/") ||
               requestURI.startsWith("/pages/fundador/") ||
               requestURI.startsWith("/pages/colaborador/") ||
               requestURI.startsWith("/pages/parceiro/") ||
               requestURI.startsWith("/pages/associado/");
    }
    
    /**
     * Determina se este filtro deve ser aplicado à requisição
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param request Requisição HTTP
     * @return false para aplicar o filtro (comportamento padrão)
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Pular filtro para recursos estáticos específicos que sabemos que não precisam
        if (path.endsWith(".css") || 
            path.endsWith(".js") || 
            path.endsWith(".png") || 
            path.endsWith(".jpg") || 
            path.endsWith(".jpeg") || 
            path.endsWith(".gif") || 
            path.endsWith(".ico") ||
            path.endsWith(".svg") ||
            path.endsWith(".woff") ||
            path.endsWith(".woff2") ||
            path.endsWith(".ttf") ||
            path.endsWith(".eot")) {
            return true;
        }
        
        // Pular filtro para endpoints públicos
        return !requiresAuthentication(path);
    }
}