package com.sistema.java.bean;

import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.entity.Noticia;
import com.sistema.java.model.entity.Comentario;
import com.sistema.java.model.dto.NoticiaDTO;
import com.sistema.java.model.dto.ComentarioDTO;
import com.sistema.java.model.enums.PapelUsuario;
import com.sistema.java.service.AuthService;
import com.sistema.java.service.NoticiaService;
import com.sistema.java.service.ComentarioService;
import com.sistema.java.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Managed Bean para dashboard do usuário
 * Referência: Controle de Acesso - project_rules.md
 * Referência: Padrões de Desenvolvimento - project_rules.md
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
    
    // Dados do dashboard
    private Usuario usuarioLogado;
    private List<NoticiaDTO> ultimasNoticias;
    private List<ComentarioDTO> ultimosComentarios;
    private Map<String, Long> estatisticas;
    
    // Flags de carregamento
    private boolean dadosCarregados = false;
    
    @PostConstruct
    public void init() {
        try {
            logger.info("Inicializando DashboardBean");
            carregarDadosDashboard();
        } catch (Exception e) {
            logger.error("Erro ao inicializar dashboard", e);
        }
    }
    
    /**
     * Carrega os dados do dashboard baseado no papel do usuário
     * Referência: Controle de Acesso - project_rules.md
     */
    public void carregarDadosDashboard() {
        try {
            usuarioLogado = authService.getUsuarioLogado();
            
            if (usuarioLogado == null) {
                logger.warn("Usuário não está logado ao acessar dashboard");
                return;
            }
            
            logger.info("Carregando dashboard para usuário: {} ({})", 
                       usuarioLogado.getEmail(), usuarioLogado.getPapel());
            
            // Carregar dados baseado no papel do usuário
            switch (usuarioLogado.getPapel()) {
                case ADMINISTRADOR:
                case FUNDADOR:
                    carregarDashboardAdmin();
                    break;
                case COLABORADOR:
                    carregarDashboardColaborador();
                    break;
                case ASSOCIADO:
                    carregarDashboardAssociado();
                    break;
                case PARCEIRO:
                    carregarDashboardParceiro();
                    break;
                case USUARIO:
                default:
                    carregarDashboardUsuario();
                    break;
            }
            
            dadosCarregados = true;
            
        } catch (Exception e) {
            logger.error("Erro ao carregar dados do dashboard", e);
            dadosCarregados = false;
        }
    }
    
    /**
     * Carrega dashboard para administradores
     */
    private void carregarDashboardAdmin() {
        try {
            // Estatísticas gerais do sistema
            estatisticas = new HashMap<>();
            estatisticas.put("totalUsuarios", usuarioService.countTotal());
            estatisticas.put("totalNoticias", noticiaService.countTotal());
            estatisticas.put("totalComentarios", comentarioService.countTotal());
            estatisticas.put("comentariosPendentes", comentarioService.countPendentes());
            
            // Últimas notícias (todas)
             ultimasNoticias = noticiaService.findAll(
                 PageRequest.of(0, 5)).getContent();
            
            // Últimos comentários (todos)
             ultimosComentarios = comentarioService.findAll(
                 PageRequest.of(0, 5)).getContent();
            
            logger.info("Dashboard admin carregado com {} notícias e {} comentários", 
                       ultimasNoticias.size(), ultimosComentarios.size());
            
        } catch (Exception e) {
            logger.error("Erro ao carregar dashboard admin", e);
        }
    }
    
    /**
     * Carrega dashboard para colaboradores
     */
    private void carregarDashboardColaborador() {
        try {
            // Estatísticas de conteúdo
            estatisticas = new HashMap<>();
            estatisticas.put("minhasNoticias", noticiaService.findByAutor(usuarioLogado.getId(),
                 PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements());
            estatisticas.put("totalNoticias", noticiaService.countTotal());
            estatisticas.put("comentariosPendentes", comentarioService.countPendentes());
            
            // Minhas últimas notícias
             ultimasNoticias = noticiaService.findByAutor(usuarioLogado.getId(),
                 PageRequest.of(0, 5)).getContent();
            
            // Comentários para moderar
             ultimosComentarios = comentarioService.findPendentes(
                 PageRequest.of(0, 5)).getContent();
            
            logger.info("Dashboard colaborador carregado para usuário: {}", usuarioLogado.getEmail());
            
        } catch (Exception e) {
            logger.error("Erro ao carregar dashboard colaborador", e);
        }
    }
    
    /**
     * Carrega dashboard para associados
     */
    private void carregarDashboardAssociado() {
        try {
            // Estatísticas básicas
            estatisticas = new HashMap<>();
            estatisticas.put("totalNoticias", noticiaService.countPublicadas());
            estatisticas.put("meusComentarios", comentarioService.findByAutor(usuarioLogado.getId(),
                 PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements());
            
            // Últimas notícias publicadas
             ultimasNoticias = noticiaService.findPublicadas(
                 PageRequest.of(0, 5)).getContent();
            
            // Meus últimos comentários
             ultimosComentarios = comentarioService.findByAutor(usuarioLogado.getId(),
                 PageRequest.of(0, 5)).getContent();
            
            logger.info("Dashboard associado carregado para usuário: {}", usuarioLogado.getEmail());
            
        } catch (Exception e) {
            logger.error("Erro ao carregar dashboard associado", e);
        }
    }
    
    /**
     * Carrega dashboard para parceiros
     */
    private void carregarDashboardParceiro() {
        try {
            // Estatísticas específicas para parceiros
            estatisticas = new HashMap<>();
            estatisticas.put("totalNoticias", noticiaService.contarPublicadas());
            estatisticas.put("meusComentarios", comentarioService.findByAutor(usuarioLogado.getId(),
                PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements());
            
            // Últimas notícias publicadas
            ultimasNoticias = noticiaService.findPublicadas(
                PageRequest.of(0, 5)).getContent();
            
            // Meus últimos comentários
            ultimosComentarios = comentarioService.findByAutor(usuarioLogado.getId(),
                PageRequest.of(0, 5)).getContent();
            
            logger.info("Dashboard parceiro carregado para usuário: {}", usuarioLogado.getEmail());
            
        } catch (Exception e) {
            logger.error("Erro ao carregar dashboard parceiro", e);
        }
    }
    
    /**
     * Carrega dashboard para usuários comuns
     */
    private void carregarDashboardUsuario() {
        try {
            // Estatísticas básicas
            estatisticas = new HashMap<>();
            estatisticas.put("totalNoticias", noticiaService.contarPublicadas());
            estatisticas.put("meusComentarios", comentarioService.findByAutor(usuarioLogado.getId(),
                PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements());
            
            // Últimas notícias publicadas
            ultimasNoticias = noticiaService.findPublicadas(
                PageRequest.of(0, 5)).getContent();
            
            // Meus últimos comentários
            ultimosComentarios = comentarioService.findByAutor(usuarioLogado.getId(),
                PageRequest.of(0, 5)).getContent();
            
            logger.info("Dashboard usuário carregado para usuário: {}", usuarioLogado.getEmail());
            
        } catch (Exception e) {
            logger.error("Erro ao carregar dashboard usuário", e);
        }
    }
    
    /**
     * Atualiza os dados do dashboard
     */
    public void atualizarDados() {
        logger.info("Atualizando dados do dashboard");
        carregarDadosDashboard();
    }
    
    // Métodos de verificação de permissões
    public boolean podeGerenciarUsuarios() {
        return usuarioLogado != null && 
               (usuarioLogado.getPapel() == PapelUsuario.ADMINISTRADOR || 
                usuarioLogado.getPapel() == PapelUsuario.FUNDADOR);
    }
    
    public boolean podeCriarNoticias() {
        return usuarioLogado != null && 
               (usuarioLogado.getPapel() == PapelUsuario.ADMINISTRADOR || 
                usuarioLogado.getPapel() == PapelUsuario.FUNDADOR || 
                usuarioLogado.getPapel() == PapelUsuario.COLABORADOR);
    }
    
    public boolean podeModerarComentarios() {
        return usuarioLogado != null && 
               (usuarioLogado.getPapel() == PapelUsuario.ADMINISTRADOR || 
                usuarioLogado.getPapel() == PapelUsuario.FUNDADOR || 
                usuarioLogado.getPapel() == PapelUsuario.COLABORADOR);
    }
    
    public boolean isAdmin() {
        return usuarioLogado != null && 
               (usuarioLogado.getPapel() == PapelUsuario.ADMINISTRADOR || 
                usuarioLogado.getPapel() == PapelUsuario.FUNDADOR);
    }
    
    // Getters e Setters
    public Usuario getUsuarioLogado() {
        return usuarioLogado;
    }
    
    public void setUsuarioLogado(Usuario usuarioLogado) {
        this.usuarioLogado = usuarioLogado;
    }
    
    public List<NoticiaDTO> getUltimasNoticias() {
        return ultimasNoticias;
    }

    public void setUltimasNoticias(List<NoticiaDTO> ultimasNoticias) {
        this.ultimasNoticias = ultimasNoticias;
    }

    public List<ComentarioDTO> getUltimosComentarios() {
        return ultimosComentarios;
    }

    public void setUltimosComentarios(List<ComentarioDTO> ultimosComentarios) {
        this.ultimosComentarios = ultimosComentarios;
    }
    
    public Map<String, Long> getEstatisticas() {
        return estatisticas;
    }
    
    public void setEstatisticas(Map<String, Long> estatisticas) {
        this.estatisticas = estatisticas;
    }
    
    public boolean isDadosCarregados() {
        return dadosCarregados;
    }
    
    public void setDadosCarregados(boolean dadosCarregados) {
        this.dadosCarregados = dadosCarregados;
    }
}