package com.sistema.java.config;

import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.List;

/**
 * Interceptador para controle de acesso baseado em papéis de usuário
 * 
 * @author Sistema Java
 * @version 1.0
 */
@Component
public class SecurityInterceptor implements HandlerInterceptor {

    // URLs públicas que não requerem autenticação
    private static final List<String> PUBLIC_URLS = Arrays.asList(
        "/",
        "/login",
        "/registro",
        "/verificar-email",
        "/esqueci-senha",
        "/reset-senha",
        "/noticias",
        "/noticia",
        "/categoria",
        "/sobre",
        "/contato",
        "/resources",
        "/javax.faces.resource",
        "/css",
        "/js",
        "/images",
        "/uploads"
    );

    // URLs que requerem apenas login (qualquer usuário autenticado)
    private static final List<String> USER_URLS = Arrays.asList(
        "/perfil",
        "/editar-perfil",
        "/alterar-senha",
        "/meus-comentarios",
        "/logout"
    );

    // URLs que requerem papel COLABORADOR ou superior
    private static final List<String> COLABORADOR_URLS = Arrays.asList(
        "/criar-noticia",
        "/editar-noticia",
        "/minhas-noticias"
    );

    // URLs que requerem papel ADMINISTRADOR ou superior
    private static final List<String> ADMIN_URLS = Arrays.asList(
        "/dashboard",
        "/gerenciar-usuarios",
        "/gerenciar-noticias",
        "/gerenciar-categorias",
        "/gerenciar-comentarios",
        "/relatorios",
        "/configuracoes"
    );

    // URLs que requerem papel FUNDADOR
    private static final List<String> FUNDADOR_URLS = Arrays.asList(
        "/sistema",
        "/backup",
        "/logs",
        "/manutencao"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        
        // Remove o context path da URI
        if (contextPath != null && !contextPath.isEmpty()) {
            requestURI = requestURI.substring(contextPath.length());
        }

        // Verifica se é uma URL pública
        if (isPublicUrl(requestURI)) {
            return true;
        }

        // Obtém a sessão e o usuário logado
        HttpSession session = request.getSession(false);
        Usuario usuarioLogado = null;
        
        if (session != null) {
            usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        }

        // Se não há usuário logado, redireciona para login
        if (usuarioLogado == null) {
            response.sendRedirect(contextPath + "/login?redirect=" + requestURI);
            return false;
        }

        // Verifica se o usuário está ativo
        if (!usuarioLogado.isAtivo()) {
            session.invalidate();
            response.sendRedirect(contextPath + "/login?error=conta_inativa");
            return false;
        }

        // Verifica se o email foi verificado (exceto para URLs de usuário básico)
        if (!usuarioLogado.isEmailVerificado() && !isUserUrl(requestURI)) {
            response.sendRedirect(contextPath + "/perfil?error=email_nao_verificado");
            return false;
        }

        // Verifica permissões baseadas no papel do usuário
        PapelUsuario papel = usuarioLogado.getPapel();
        
        if (isFundadorUrl(requestURI)) {
            if (papel != PapelUsuario.FUNDADOR) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso negado");
                return false;
            }
        } else if (isAdminUrl(requestURI)) {
            if (!hasAdminAccess(papel)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso negado");
                return false;
            }
        } else if (isColaboradorUrl(requestURI)) {
            if (!hasColaboradorAccess(papel)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso negado");
                return false;
            }
        } else if (isUserUrl(requestURI)) {
            // Qualquer usuário autenticado pode acessar
            return true;
        }

        return true;
    }

    /**
     * Verifica se a URL é pública
     */
    private boolean isPublicUrl(String url) {
        return PUBLIC_URLS.stream().anyMatch(url::startsWith);
    }

    /**
     * Verifica se a URL requer apenas login
     */
    private boolean isUserUrl(String url) {
        return USER_URLS.stream().anyMatch(url::startsWith);
    }

    /**
     * Verifica se a URL requer papel COLABORADOR ou superior
     */
    private boolean isColaboradorUrl(String url) {
        return COLABORADOR_URLS.stream().anyMatch(url::startsWith);
    }

    /**
     * Verifica se a URL requer papel ADMINISTRADOR ou superior
     */
    private boolean isAdminUrl(String url) {
        return ADMIN_URLS.stream().anyMatch(url::startsWith);
    }

    /**
     * Verifica se a URL requer papel FUNDADOR
     */
    private boolean isFundadorUrl(String url) {
        return FUNDADOR_URLS.stream().anyMatch(url::startsWith);
    }

    /**
     * Verifica se o usuário tem acesso de colaborador ou superior
     */
    private boolean hasColaboradorAccess(PapelUsuario papel) {
        return papel == PapelUsuario.COLABORADOR ||
               papel == PapelUsuario.ADMINISTRADOR ||
               papel == PapelUsuario.FUNDADOR;
    }

    /**
     * Verifica se o usuário tem acesso de administrador ou superior
     */
    private boolean hasAdminAccess(PapelUsuario papel) {
        return papel == PapelUsuario.ADMINISTRADOR ||
               papel == PapelUsuario.FUNDADOR;
    }
}