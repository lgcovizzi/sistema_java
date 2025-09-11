package com.sistema.java.bean;

import com.sistema.java.model.Usuario;
import com.sistema.java.model.dto.ComentarioDTO;
import com.sistema.java.model.dto.NoticiaDTO;
import com.sistema.java.model.enums.Role;
import com.sistema.java.service.AuthService;
import com.sistema.java.service.ComentarioService;
import com.sistema.java.service.NoticiaService;
import com.sistema.java.service.UsuarioService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ViewScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Managed Bean para o dashboard principal.
 * Referência: Implementar beans JSF - project_rules.md
 * Referência: Controle de Acesso - project_rules.md
 */
@Named("dashboardBean")
@ViewScoped
public class DashboardBean implements Serializable {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardBean.class);
    
    @Inject
    private AuthService authService;
    
    @Inject
    private NoticiaService noticiaService;
    
    @Inject
    private ComentarioService comentarioService;
    
    @Inject
    private UsuarioService usuarioService;
    
    // Dados do usuário
    private Usuario usuarioLogado;
    
    // Estatísticas gerais
    private Map<String, Long> estatisticas;
    
    // Dados específicos por papel
    private List<NoticiaDTO> minhasNoticias;
    private List<NoticiaDTO> noticiasRecentes;
    private List<ComentarioDTO> comentariosPendentes;
    private List<ComentarioDTO> meusComentarios;
    
    // Configurações
    private static final int LIMITE_NOTICIAS_RECENTES = 5;
    private static final int LIMITE_COMENTARIOS = 10;
    
    @PostConstruct
    public void init() {
        try {
            carregarDadosUsuario();
            carregarEstatisticas();
            carregarDadosEspecificos();
        } catch (Exception e) {
            logger.error("Erro ao inicializar DashboardBean", e);
            addErrorMessage("Erro ao carregar dashboard.");
        }
    }
    
    /**
     * Carrega dados do usuário logado
     * Referência: Controle de Acesso - project_rules.md
     */
    private void carregarDadosUsuario() {
        usuarioLogado = authService.getUsuarioLogado();
        if (usuarioLogado == null) {
            addErrorMessage("Usuário não encontrado. Faça login novamente.");
        }
    }
    
    /**
     * Carrega estatísticas baseadas no papel do usuário
     * Referência: Controle de Acesso - project_rules.md
     */
    private void carregarEstatisticas() {
        estatisticas = new HashMap<>();
        
        try {
            Role role = usuarioLogado.getRole();
            
            switch (role) {
                case ADMINISTRADOR:
                case FUNDADOR:
                    // Estatísticas administrativas completas
                    estatisticas.put("totalUsuarios", usuarioService.contarTodos());
                    estatisticas.put("usuariosAtivos", usuarioService.contarAtivos());
                    estatisticas.put("totalNoticias", noticiaService.contarTodas());
                    estatisticas.put("noticiasPublicadas", noticiaService.contarPublicadas());
                    estatisticas.put("totalComentarios", comentarioService.contarTodos());
                    estatisticas.put("comentariosPendentes", comentarioService.contarPendentes());
                    break;
                    
                case COLABORADOR:
                    // Estatísticas de conteúdo
                    estatisticas.put("minhasNoticias", noticiaService.contarPorAutor(usuarioLogado.getId()));
                    estatisticas.put("noticiasPublicadas", noticiaService.contarPublicadasPorAutor(usuarioLogado.getId()));
                    estatisticas.put("totalComentarios", comentarioService.contarTodos());
                    estatisticas.put("comentariosPendentes", comentarioService.contarPendentes());
                    break;
                    
                case ASSOCIADO:
                case PARCEIRO:
                    // Estatísticas básicas
                    estatisticas.put("noticiasPublicadas", noticiaService.contarPublicadas());
                    estatisticas.put("meusComentarios", comentarioService.contarPorAutor(usuarioLogado.getId()));
                    break;
                    
                case USUARIO:
                    // Estatísticas mínimas
                    estatisticas.put("noticiasPublicadas", noticiaService.contarPublicadas());
                    estatisticas.put("meusComentarios", comentarioService.contarPorAutor(usuarioLogado.getId()));
                    break;
                    
                default:
                    // Sem estatísticas para convidados
                    break;
            }
            
        } catch (Exception e) {
            logger.error("Erro ao carregar estatísticas", e);
            addErrorMessage("Erro ao carregar estatísticas.");
        }
    }
    
    /**
     * Carrega dados específicos baseados no papel do usuário
     * Referência: Controle de Acesso - project_rules.md
     */
    private void carregarDadosEspecificos() {
        try {
            Role role = usuarioLogado.getRole();
            
            // Carregar notícias recentes para todos os papéis
            noticiasRecentes = noticiaService.listarRecentes(LIMITE_NOTICIAS_RECENTES);
            
            switch (role) {
                case ADMINISTRADOR:
                case FUNDADOR:
                    // Comentários pendentes para moderação
                    comentariosPendentes = comentarioService.listarPendentes(LIMITE_COMENTARIOS);
                    break;
                    
                case COLABORADOR:
                    // Minhas notícias e comentários pendentes
                    minhasNoticias = noticiaService.listarPorAutor(usuarioLogado.getId(), LIMITE_NOTICIAS_RECENTES);
                    comentariosPendentes = comentarioService.listarPendentes(LIMITE_COMENTARIOS);
                    break;
                    
                case ASSOCIADO:
                case PARCEIRO:
                case USUARIO:
                    // Meus comentários
                    meusComentarios = comentarioService.listarPorAutor(usuarioLogado.getId(), LIMITE_COMENTARIOS);
                    break;
                    
                default:
                    // Sem dados específicos para convidados
                    break;
            }
            
        } catch (Exception e) {
            logger.error("Erro ao carregar dados específicos", e);
            addErrorMessage("Erro ao carregar dados do dashboard.");
        }
    }
    
    /**
     * Atualiza dados do dashboard
     */
    public void atualizarDados() {
        carregarEstatisticas();
        carregarDadosEspecificos();
        addInfoMessage("Dashboard atualizado com sucesso!");
    }
    
    /**
     * Verifica se usuário pode acessar área administrativa
     */
    public boolean podeAcessarAdmin() {
        return authService.hasRole(Role.ADMINISTRADOR) || 
               authService.hasRole(Role.FUNDADOR) || 
               authService.hasRole(Role.COLABORADOR);
    }
    
    /**
     * Verifica se usuário pode moderar comentários
     */
    public boolean podeModerarComentarios() {
        return authService.canModerateComments();
    }
    
    /**
     * Verifica se usuário pode criar notícias
     */
    public boolean podeCriarNoticias() {
        return authService.hasRole(Role.ADMINISTRADOR) || 
               authService.hasRole(Role.FUNDADOR) || 
               authService.hasRole(Role.COLABORADOR);
    }
    
    /**
     * Verifica se usuário pode gerenciar usuários
     */
    public boolean podeGerenciarUsuarios() {
        return authService.hasRole(Role.ADMINISTRADOR) || 
               authService.hasRole(Role.FUNDADOR);
    }
    
    /**
     * Obtém saudação personalizada baseada no horário
     */
    public String getSaudacao() {
        LocalDateTime agora = LocalDateTime.now();
        int hora = agora.getHour();
        
        String periodo;
        if (hora < 12) {
            periodo = "Bom dia";
        } else if (hora < 18) {
            periodo = "Boa tarde";
        } else {
            periodo = "Boa noite";
        }
        
        return periodo + ", " + usuarioLogado.getNome() + "!";
    }
    
    /**
     * Obtém descrição do papel do usuário
     */
    public String getDescricaoPapel() {
        if (usuarioLogado == null) return "";
        
        return switch (usuarioLogado.getRole()) {
            case ADMINISTRADOR -> "Administrador do Sistema";
            case FUNDADOR -> "Fundador";
            case COLABORADOR -> "Colaborador";
            case ASSOCIADO -> "Associado";
            case PARCEIRO -> "Parceiro";
            case USUARIO -> "Usuário";
            case CONVIDADO -> "Convidado";
        };
    }
    
    /**
     * Navega para página de criação de notícia
     */
    public String criarNoticia() {
        if (podeCriarNoticias()) {
            return "/admin/noticias/nova?faces-redirect=true";
        }
        addErrorMessage("Você não tem permissão para criar notícias.");
        return null;
    }
    
    /**
     * Navega para página de gerenciamento de usuários
     */
    public String gerenciarUsuarios() {
        if (podeGerenciarUsuarios()) {
            return "/admin/usuarios?faces-redirect=true";
        }
        addErrorMessage("Você não tem permissão para gerenciar usuários.");
        return null;
    }
    
    /**
     * Navega para página de moderação de comentários
     */
    public String moderarComentarios() {
        if (podeModerarComentarios()) {
            return "/admin/comentarios?faces-redirect=true";
        }
        addErrorMessage("Você não tem permissão para moderar comentários.");
        return null;
    }
    
    // Métodos utilitários para mensagens
    private void addInfoMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", message));
    }
    
    private void addErrorMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", message));
    }
    
    // Getters e Setters
    public Usuario getUsuarioLogado() {
        return usuarioLogado;
    }
    
    public Map<String, Long> getEstatisticas() {
        return estatisticas;
    }
    
    public List<NoticiaDTO> getMinhasNoticias() {
        return minhasNoticias;
    }
    
    public List<NoticiaDTO> getNoticiasRecentes() {
        return noticiasRecentes;
    }
    
    public List<ComentarioDTO> getComentariosPendentes() {
        return comentariosPendentes;
    }
    
    public List<ComentarioDTO> getMeusComentarios() {
        return meusComentarios;
    }
}