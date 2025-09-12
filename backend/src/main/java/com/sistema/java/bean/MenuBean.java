package com.sistema.java.bean;

import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;
import com.sistema.java.service.AuthService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Managed Bean para gerenciamento de menus e navegação.
 * Referência: Implementar beans JSF - project_rules.md
 * Referência: Controle de Acesso - project_rules.md
 */
@Named("menuBean")
@SessionScoped
public class MenuBean implements Serializable {
    
    private static final Logger logger = LoggerFactory.getLogger(MenuBean.class);
    
    @Inject
    private AuthService authService;
    
    // Itens de menu
    private List<MenuItem> menuPrincipal;
    private List<MenuItem> menuAdmin;
    private List<MenuItem> menuUsuario;
    
    // Estado do menu
    private boolean menuMobileAberto = false;
    private boolean menuUsuarioAberto = false;
    
    @PostConstruct
    public void init() {
        try {
            construirMenus();
        } catch (Exception e) {
            logger.error("Erro ao inicializar MenuBean", e);
        }
    }
    
    /**
     * Constrói todos os menus do sistema
     * Referência: Controle de Acesso - project_rules.md
     */
    private void construirMenus() {
        construirMenuPrincipal();
        construirMenuAdmin();
        construirMenuUsuario();
    }
    
    /**
     * Constrói menu principal (header)
     */
    private void construirMenuPrincipal() {
        menuPrincipal = new ArrayList<>();
        
        // Home - sempre visível
        menuPrincipal.add(new MenuItem("Home", "/index.xhtml", "pi pi-home", true));
        
        // Notícias - sempre visível
        menuPrincipal.add(new MenuItem("Notícias", "/noticias.xhtml", "pi pi-file", true));
        
        // Dashboard - apenas para usuários autenticados
        if (authService.isLoggedIn()) {
            Usuario usuario = authService.getUsuarioLogado();
            if (usuario != null && usuario.canAccessDashboard()) {
                menuPrincipal.add(new MenuItem("Dashboard", "/dashboard.xhtml", "pi pi-chart-line", true));
            }
        }
        
        // Admin - apenas para papéis administrativos
        if (podeAcessarAdmin()) {
            menuPrincipal.add(new MenuItem("Administração", "#", "pi pi-cog", true, construirSubmenuAdmin()));
        }
    }
    
    /**
     * Constrói submenu de administração
     */
    private List<MenuItem> construirSubmenuAdmin() {
        List<MenuItem> submenu = new ArrayList<>();
        
        if (podeGerenciarUsuarios()) {
            submenu.add(new MenuItem("Usuários", "/admin/usuarios.xhtml", "pi pi-users", true));
        }
        
        if (podeCriarNoticias()) {
            submenu.add(new MenuItem("Notícias", "/admin/noticias.xhtml", "pi pi-file-edit", true));
        }
        
        if (podeGerenciarCategorias()) {
            submenu.add(new MenuItem("Categorias", "/admin/categorias.xhtml", "pi pi-tags", true));
        }
        
        if (podeModerarComentarios()) {
            submenu.add(new MenuItem("Comentários", "/admin/comentarios.xhtml", "pi pi-comments", true));
        }
        
        return submenu;
    }
    
    /**
     * Constrói menu administrativo (sidebar)
     */
    private void construirMenuAdmin() {
        menuAdmin = new ArrayList<>();
        
        // Dashboard
        menuAdmin.add(new MenuItem("Dashboard", "/admin/dashboard.xhtml", "pi pi-chart-line", true));
        
        // Gestão de Conteúdo
        if (podeCriarNoticias()) {
            List<MenuItem> submenuConteudo = new ArrayList<>();
            submenuConteudo.add(new MenuItem("Todas as Notícias", "/admin/noticias.xhtml", "pi pi-list", true));
            submenuConteudo.add(new MenuItem("Nova Notícia", "/admin/noticias/nova.xhtml", "pi pi-plus", true));
            
            if (podeGerenciarCategorias()) {
                submenuConteudo.add(new MenuItem("Categorias", "/admin/categorias.xhtml", "pi pi-tags", true));
            }
            
            menuAdmin.add(new MenuItem("Conteúdo", "#", "pi pi-file-edit", true, submenuConteudo));
        }
        
        // Gestão de Usuários
        if (podeGerenciarUsuarios()) {
            List<MenuItem> submenuUsuarios = new ArrayList<>();
            submenuUsuarios.add(new MenuItem("Todos os Usuários", "/admin/usuarios.xhtml", "pi pi-users", true));
            submenuUsuarios.add(new MenuItem("Novo Usuário", "/admin/usuarios/novo.xhtml", "pi pi-user-plus", true));
            
            menuAdmin.add(new MenuItem("Usuários", "#", "pi pi-users", true, submenuUsuarios));
        }
        
        // Moderação
        if (podeModerarComentarios()) {
            menuAdmin.add(new MenuItem("Comentários", "/admin/comentarios.xhtml", "pi pi-comments", true));
        }
        
        // Configurações
        if (authService.hasRole(PapelUsuario.ADMINISTRADOR) || authService.hasRole(PapelUsuario.FUNDADOR)) {
            List<MenuItem> submenuConfig = new ArrayList<>();
            submenuConfig.add(new MenuItem("Sistema", "/admin/configuracoes/sistema.xhtml", "pi pi-cog", true));
            submenuConfig.add(new MenuItem("Email", "/admin/configuracoes/email.xhtml", "pi pi-envelope", true));
            
            menuAdmin.add(new MenuItem("Configurações", "#", "pi pi-cog", true, submenuConfig));
        }
    }
    
    /**
     * Constrói menu do usuário (dropdown)
     */
    private void construirMenuUsuario() {
        menuUsuario = new ArrayList<>();
        
        if (authService.isLoggedIn()) {
            // Perfil
            menuUsuario.add(new MenuItem("Meu Perfil", "/perfil.xhtml", "pi pi-user", true));
            
            // Dashboard (se aplicável)
            Usuario usuario = authService.getUsuarioLogado();
            if (usuario != null && usuario.canAccessDashboard()) {
                menuUsuario.add(new MenuItem("Dashboard", "/dashboard.xhtml", "pi pi-chart-line", true));
            }
            
            // Separador
            menuUsuario.add(new MenuItem("-", "#", "", false));
            
            // Logout
            menuUsuario.add(new MenuItem("Sair", "/logout", "pi pi-sign-out", true));
        } else {
            // Login
            menuUsuario.add(new MenuItem("Entrar", "/login.xhtml", "pi pi-sign-in", true));
            menuUsuario.add(new MenuItem("Cadastrar", "/registro.xhtml", "pi pi-user-plus", true));
        }
    }
    
    /**
     * Atualiza menus quando estado de autenticação muda
     */
    public void atualizarMenus() {
        construirMenus();
    }
    
    /**
     * Método alternativo para atualizar menu (usado em testes)
     */
    public void atualizarMenu() {
        atualizarMenus();
    }
    
    /**
     * Alterna estado do menu mobile
     */
    public void alternarMenuMobile() {
        menuMobileAberto = !menuMobileAberto;
    }
    
    /**
     * Alterna estado do menu do usuário
     */
    public void alternarMenuUsuario() {
        menuUsuarioAberto = !menuUsuarioAberto;
    }
    
    /**
     * Fecha todos os menus
     */
    public void fecharMenus() {
        menuMobileAberto = false;
        menuUsuarioAberto = false;
    }
    
    // Métodos de verificação de permissões
    public boolean podeAcessarAdmin() {
        return authService.hasRole(PapelUsuario.ADMINISTRADOR) || 
               authService.hasRole(PapelUsuario.FUNDADOR) || 
               authService.hasRole(PapelUsuario.COLABORADOR);
    }
    
    public boolean podeGerenciarUsuarios() {
        return authService.hasRole(PapelUsuario.ADMINISTRADOR) || 
               authService.hasRole(PapelUsuario.FUNDADOR);
    }
    
    public boolean podeCriarNoticias() {
        return authService.hasRole(PapelUsuario.ADMINISTRADOR) || 
               authService.hasRole(PapelUsuario.FUNDADOR) || 
               authService.hasRole(PapelUsuario.COLABORADOR);
    }
    
    public boolean podeGerenciarCategorias() {
        return authService.hasRole(PapelUsuario.ADMINISTRADOR) || 
               authService.hasRole(PapelUsuario.FUNDADOR) || 
               authService.hasRole(PapelUsuario.COLABORADOR);
    }
    
    public boolean podeModerarComentarios() {
        return authService.canModerateComments();
    }
    
    public boolean isUsuarioLogado() {
        return authService.isLoggedIn();
    }
    
    public Usuario getUsuarioLogado() {
        return authService.getUsuarioLogado();
    }
    
    /**
     * Retorna todos os itens de menu disponíveis para o usuário atual
     */
    public List<MenuItem> getMenuItems() {
        List<MenuItem> allItems = new ArrayList<>();
        if (menuPrincipal != null) {
            allItems.addAll(menuPrincipal);
        }
        if (podeAcessarAdmin() && menuAdmin != null) {
            allItems.addAll(menuAdmin);
        }
        if (isUsuarioLogado() && menuUsuario != null) {
            allItems.addAll(menuUsuario);
        }
        return allItems;
    }
    
    // Getters e Setters
    public List<MenuItem> getMenuPrincipal() {
        return menuPrincipal;
    }
    
    public List<MenuItem> getMenuAdmin() {
        return menuAdmin;
    }
    
    public List<MenuItem> getMenuUsuario() {
        return menuUsuario;
    }
    
    public boolean isMenuMobileAberto() {
        return menuMobileAberto;
    }
    
    public void setMenuMobileAberto(boolean menuMobileAberto) {
        this.menuMobileAberto = menuMobileAberto;
    }
    
    public boolean isMenuUsuarioAberto() {
        return menuUsuarioAberto;
    }
    
    public void setMenuUsuarioAberto(boolean menuUsuarioAberto) {
        this.menuUsuarioAberto = menuUsuarioAberto;
    }
    
    /**
     * Classe interna para representar itens de menu
     */
    public static class MenuItem implements Serializable {
        private String label;
        private String url;
        private String icon;
        private boolean visible;
        private List<MenuItem> submenu;
        
        public MenuItem(String label, String url, String icon, boolean visible) {
            this.label = label;
            this.url = url;
            this.icon = icon;
            this.visible = visible;
            this.submenu = new ArrayList<>();
        }
        
        public MenuItem(String label, String url, String icon, boolean visible, List<MenuItem> submenu) {
            this.label = label;
            this.url = url;
            this.icon = icon;
            this.visible = visible;
            this.submenu = submenu != null ? submenu : new ArrayList<>();
        }
        
        public boolean hasSubmenu() {
            return submenu != null && !submenu.isEmpty();
        }
        
        public boolean isSeparator() {
            return "-".equals(label);
        }
        
        // Getters e Setters
        public String getLabel() {
            return label;
        }
        
        public void setLabel(String label) {
            this.label = label;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getIcon() {
            return icon;
        }
        
        public void setIcon(String icon) {
            this.icon = icon;
        }
        
        public boolean isVisible() {
            return visible;
        }
        
        public void setVisible(boolean visible) {
            this.visible = visible;
        }
        
        public List<MenuItem> getSubmenu() {
            return submenu;
        }
        
        public void setSubmenu(List<MenuItem> submenu) {
            this.submenu = submenu;
        }
    }
}