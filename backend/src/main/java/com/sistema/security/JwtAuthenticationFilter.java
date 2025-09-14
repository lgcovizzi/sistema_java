package com.sistema.security;

import com.sistema.service.JwtService;
import com.sistema.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Filtro de autenticação JWT que intercepta todas as requisições HTTP
 * para validar tokens JWT e configurar o contexto de segurança.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);
            
            if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticateUser(jwt, request);
            }
        } catch (Exception e) {
            logger.error("Erro durante autenticação JWT: {}", e.getMessage());
            // Não interrompe a cadeia de filtros, apenas loga o erro
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrai o token JWT do cabeçalho Authorization da requisição.
     *
     * @param request A requisição HTTP
     * @return O token JWT ou null se não encontrado
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    /**
     * Autentica o usuário baseado no token JWT válido.
     *
     * @param jwt O token JWT
     * @param request A requisição HTTP
     */
    private void authenticateUser(String jwt, HttpServletRequest request) {
        try {
            // Valida o token e extrai o username
            if (!jwtService.isTokenValid(jwt)) {
                logger.debug("Token JWT inválido ou expirado");
                return;
            }

            String username = jwtService.extractUsername(jwt);
            if (username == null) {
                logger.debug("Username não encontrado no token JWT");
                return;
            }

            // Carrega os detalhes do usuário
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            // Cria o objeto de autenticação
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            
            // Adiciona detalhes da requisição
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            // Define a autenticação no contexto de segurança
            SecurityContextHolder.getContext().setAuthentication(authToken);
            
            logger.debug("Usuário autenticado com sucesso: {}", username);
            
        } catch (Exception e) {
            logger.error("Erro ao autenticar usuário: {}", e.getMessage());
        }
    }

    /**
     * Determina se este filtro deve ser aplicado à requisição.
     * Por padrão, aplica a todas as requisições exceto algumas específicas.
     *
     * @param request A requisição HTTP
     * @return true se o filtro deve ser aplicado
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Não aplica o filtro para endpoints públicos
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/health") ||
               path.startsWith("/api/info") ||
               path.startsWith("/actuator") ||
               path.startsWith("/error") ||
               path.equals("/") ||
               path.equals("/api-simple");
    }

    /**
     * Limpa o contexto de segurança em caso de erro.
     */
    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Verifica se a requisição contém um token JWT válido.
     *
     * @param request A requisição HTTP
     * @return true se contém token JWT
     */
    public boolean hasJwtToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        return bearerToken != null && bearerToken.startsWith(BEARER_PREFIX);
    }

    /**
     * Extrai informações do usuário autenticado do contexto de segurança.
     *
     * @return O usuário autenticado ou null
     */
    public static UserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return (UserDetails) authentication.getPrincipal();
        }
        
        return null;
    }

    /**
     * Verifica se há um usuário autenticado no contexto atual.
     *
     * @return true se há usuário autenticado
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && 
               authentication.isAuthenticated() && 
               !(authentication.getPrincipal() instanceof String);
    }
}