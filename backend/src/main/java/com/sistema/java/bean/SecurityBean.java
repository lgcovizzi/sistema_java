package com.sistema.java.bean;

import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;

/**
 * Bean para verificações de segurança nas páginas JSF
 * 
 * @author Sistema Java
 * @version 1.0
 */
@Component
@Scope("session")
public class SecurityBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Obtém o usuário logado da sessão
     */
    public Usuario getUsuarioLogado() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
            if (session != null) {
                return (Usuario) session.getAttribute("usuarioLogado");
            }
        }
        return null;
    }

    /**
     * Verifica se há usuário logado
     */
    public boolean isLoggedIn() {
        return getUsuarioLogado() != null;
    }

    /**
     * Verifica se o usuário é administrador ou fundador
     */
    public boolean isAdmin() {
        Usuario usuario = getUsuarioLogado();
        if (usuario == null) {
            return false;
        }
        PapelUsuario papel = usuario.getPapel();
        return papel == PapelUsuario.ADMINISTRADOR || papel == PapelUsuario.FUNDADOR;
    }

    /**
     * Verifica se o usuário é fundador
     */
    public boolean isFundador() {
        Usuario usuario = getUsuarioLogado();
        if (usuario == null) {
            return false;
        }
        return usuario.getPapel() == PapelUsuario.FUNDADOR;
    }

    /**
     * Verifica se o usuário é colaborador ou superior
     */
    public boolean isColaborador() {
        Usuario usuario = getUsuarioLogado();
        if (usuario == null) {
            return false;
        }
        PapelUsuario papel = usuario.getPapel();
        return papel == PapelUsuario.COLABORADOR || 
               papel == PapelUsuario.ADMINISTRADOR || 
               papel == PapelUsuario.FUNDADOR;
    }

    /**
     * Verifica se o usuário é apenas leitor
     */
    public boolean isLeitor() {
        Usuario usuario = getUsuarioLogado();
        if (usuario == null) {
            return false;
        }
        return usuario.getPapel() == PapelUsuario.LEITOR;
    }

    /**
     * Obtém o papel do usuário como string
     */
    public String getUserRole() {
        Usuario usuario = getUsuarioLogado();
        if (usuario == null) {
            return "GUEST";
        }
        return usuario.getPapel().name();
    }

    /**
     * Verifica se o usuário tem permissão para editar uma notícia
     */
    public boolean canEditNoticia(Long autorId) {
        Usuario usuario = getUsuarioLogado();
        if (usuario == null) {
            return false;
        }
        
        // Administradores e fundadores podem editar qualquer notícia
        if (isAdmin()) {
            return true;
        }
        
        // Colaboradores podem editar apenas suas próprias notícias
        if (isColaborador()) {
            return usuario.getId().equals(autorId);
        }
        
        return false;
    }

    /**
     * Verifica se o usuário tem permissão para deletar uma notícia
     */
    public boolean canDeleteNoticia(Long autorId) {
        Usuario usuario = getUsuarioLogado();
        if (usuario == null) {
            return false;
        }
        
        // Apenas administradores e fundadores podem deletar notícias
        if (isAdmin()) {
            return true;
        }
        
        return false;
    }

    /**
     * Verifica se o usuário tem permissão para moderar comentários
     */
    public boolean canModerateComments() {
        return isAdmin();
    }

    /**
     * Verifica se o usuário tem permissão para gerenciar usuários
     */
    public boolean canManageUsers() {
        return isAdmin();
    }

    /**
     * Verifica se o usuário tem permissão para gerenciar categorias
     */
    public boolean canManageCategories() {
        return isAdmin();
    }

    /**
     * Verifica se o usuário tem permissão para acessar relatórios
     */
    public boolean canAccessReports() {
        return isAdmin();
    }

    /**
     * Verifica se o usuário tem permissão para configurações do sistema
     */
    public boolean canAccessSystemSettings() {
        return isFundador();
    }

    /**
     * Verifica se o email do usuário foi verificado
     */
    public boolean isEmailVerified() {
        Usuario usuario = getUsuarioLogado();
        if (usuario == null) {
            return false;
        }
        return usuario.isEmailVerificado();
    }

    /**
     * Obtém o nome de exibição do usuário
     */
    public String getDisplayName() {
        Usuario usuario = getUsuarioLogado();
        if (usuario == null) {
            return "Visitante";
        }
        return usuario.getNome() + " " + usuario.getSobrenome();
    }

    /**
     * Obtém o avatar do usuário
     */
    public String getUserAvatar() {
        Usuario usuario = getUsuarioLogado();
        if (usuario == null || usuario.getAvatar() == null || usuario.getAvatar().isEmpty()) {
            return "/images/default-avatar.png";
        }
        return "/uploads/avatars/" + usuario.getAvatar();
    }
}