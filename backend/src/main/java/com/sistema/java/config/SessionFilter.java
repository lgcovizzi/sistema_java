package com.sistema.java.config;

import com.sistema.java.model.entity.Usuario;
import com.sistema.java.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Filtro para gerenciamento de sessões e controle de acesso
 * 
 * @author Sistema Java
 * @version 1.0
 */
@Component
public class SessionFilter implements Filter {

    @Autowired
    private UsuarioService usuarioService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Inicialização do filtro
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        // Verifica se há usuário na sessão
        if (session != null) {
            Usuario usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
            
            if (usuarioLogado != null) {
                try {
                    // Verifica se o usuário ainda existe e está ativo
                    Optional<Usuario> usuarioAtual = usuarioService.findById(usuarioLogado.getId());
                    
                    if (usuarioAtual.isPresent() && usuarioAtual.get().isAtivo()) {
                        // Atualiza o usuário na sessão com dados mais recentes
                        Usuario usuarioAtualizado = usuarioAtual.get();
                        session.setAttribute("usuarioLogado", usuarioAtualizado);
                        
                        // Atualiza último acesso
                        usuarioService.atualizarUltimoAcesso(usuarioAtualizado.getId());
                        
                        // Define atributos úteis para as páginas
                        httpRequest.setAttribute("usuarioLogado", usuarioAtualizado);
                        httpRequest.setAttribute("isLoggedIn", true);
                        httpRequest.setAttribute("userRole", usuarioAtualizado.getPapel().name());
                        httpRequest.setAttribute("isAdmin", 
                            usuarioAtualizado.getPapel().name().equals("ADMINISTRADOR") ||
                            usuarioAtualizado.getPapel().name().equals("FUNDADOR"));
                        httpRequest.setAttribute("isColaborador", 
                            usuarioAtualizado.getPapel().name().equals("COLABORADOR") ||
                            usuarioAtualizado.getPapel().name().equals("ADMINISTRADOR") ||
                            usuarioAtualizado.getPapel().name().equals("FUNDADOR"));
                    } else {
                        // Usuário não existe mais ou foi desativado
                        session.invalidate();
                        httpRequest.setAttribute("isLoggedIn", false);
                    }
                } catch (Exception e) {
                    // Em caso de erro, remove o usuário da sessão
                    session.removeAttribute("usuarioLogado");
                    httpRequest.setAttribute("isLoggedIn", false);
                }
            } else {
                httpRequest.setAttribute("isLoggedIn", false);
            }
        } else {
            httpRequest.setAttribute("isLoggedIn", false);
        }

        // Define headers de segurança
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Previne cache de páginas sensíveis
        String requestURI = httpRequest.getRequestURI();
        if (isSensitivePage(requestURI)) {
            httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            httpResponse.setHeader("Pragma", "no-cache");
            httpResponse.setHeader("Expires", "0");
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Limpeza do filtro
    }

    /**
     * Verifica se a página é sensível e não deve ser cacheada
     */
    private boolean isSensitivePage(String uri) {
        return uri.contains("/perfil") ||
               uri.contains("/dashboard") ||
               uri.contains("/admin") ||
               uri.contains("/gerenciar") ||
               uri.contains("/editar") ||
               uri.contains("/criar") ||
               uri.contains("/alterar-senha") ||
               uri.contains("/configuracoes");
    }
}