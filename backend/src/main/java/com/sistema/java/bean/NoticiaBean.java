package com.sistema.java.bean;

import com.sistema.java.model.dto.CategoriaDTO;
import com.sistema.java.model.dto.ComentarioDTO;
import com.sistema.java.model.dto.NoticiaDTO;
import com.sistema.java.service.CategoriaService;
import com.sistema.java.service.ComentarioService;
import com.sistema.java.service.NoticiaService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ViewScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Managed Bean para páginas de notícias (listagem e detalhes).
 * Responsável por gerenciar a exibição e interação com notícias.
 */
@Named("noticiaBean")
@ViewScoped
public class NoticiaBean implements Serializable {
    
    private static final Logger logger = LoggerFactory.getLogger(NoticiaBean.class);
    
    @Inject
    private NoticiaService noticiaService;
    
    @Inject
    private CategoriaService categoriaService;
    
    @Inject
    private ComentarioService comentarioService;
    
    // Propriedades para listagem
    private LazyDataModel<NoticiaDTO> noticiasLazy;
    private List<CategoriaDTO> categorias;
    private String filtroTermo;
    private Long filtroCategoria;
    private String filtroOrdenacao = "recentes";
    private String modoVisualizacao = "grid"; // grid ou lista
    
    // Propriedades para detalhes
    private NoticiaDTO noticiaAtual;
    private List<NoticiaDTO> noticiasRelacionadas;
    private List<ComentarioDTO> comentarios;
    private ComentarioDTO novoComentario;
    private boolean comentarioEnviado;
    private String mensagemComentario;
    
    // Configurações
    private static final int ITENS_POR_PAGINA = 12;
    private static final int LIMITE_RELACIONADAS = 4;
    private static final int LIMITE_COMENTARIOS = 10;
    
    @PostConstruct
    public void init() {
        try {
            inicializarDados();
            configurarLazyModel();
        } catch (Exception e) {
            logger.error("Erro ao inicializar NoticiaBean", e);
        }
    }
    
    /**
     * Inicializa os dados necessários
     */
    private void inicializarDados() {
        carregarCategorias();
        inicializarNovoComentario();
    }
    
    /**
     * Carrega as categorias ativas para filtros
     */
    private void carregarCategorias() {
        try {
            this.categorias = categoriaService.buscarAtivas();
            logger.debug("Carregadas {} categorias para filtros", categorias.size());
        } catch (Exception e) {
            logger.error("Erro ao carregar categorias", e);
            this.categorias = new ArrayList<>();
        }
    }
    
    /**
     * Configura o modelo lazy para paginação
     */
    private void configurarLazyModel() {
        this.noticiasLazy = new LazyDataModel<NoticiaDTO>() {
            @Override
            public int count(Map<String, Object> filterBy) {
                try {
                    return (int) noticiaService.contarComFiltros(filtroTermo, filtroCategoria);
                } catch (Exception e) {
                    logger.error("Erro ao contar notícias", e);
                    return 0;
                }
            }
            
            @Override
            public List<NoticiaDTO> load(int first, int pageSize, Map<String, Object> sortBy, Map<String, Object> filterBy) {
                try {
                    int pagina = first / pageSize;
                    String ordenacao = determinarOrdenacao(sortBy);
                    
                    return noticiaService.buscarComFiltros(
                        filtroTermo, 
                        filtroCategoria, 
                        ordenacao, 
                        pagina, 
                        pageSize
                    );
                } catch (Exception e) {
                    logger.error("Erro ao carregar notícias", e);
                    return new ArrayList<>();
                }
            }
        };
        
        this.noticiasLazy.setRowCount(ITENS_POR_PAGINA);
    }
    
    /**
     * Determina a ordenação baseada nos parâmetros
     */
    private String determinarOrdenacao(Map<String, Object> sortBy) {
        if (sortBy != null && !sortBy.isEmpty()) {
            String campo = sortBy.keySet().iterator().next();
            SortOrder ordem = (SortOrder) sortBy.get(campo);
            return campo + (ordem == SortOrder.DESCENDING ? " DESC" : " ASC");
        }
        
        return switch (filtroOrdenacao) {
            case "antigas" -> "dataPublicacao ASC";
            case "titulo" -> "titulo ASC";
            case "comentarios" -> "totalComentarios DESC";
            default -> "dataPublicacao DESC";
        };
    }
    
    /**
     * Aplica os filtros de pesquisa
     */
    public void aplicarFiltros() {
        try {
            configurarLazyModel();
            logger.debug("Filtros aplicados - Termo: {}, Categoria: {}, Ordenação: {}", 
                        filtroTermo, filtroCategoria, filtroOrdenacao);
        } catch (Exception e) {
            logger.error("Erro ao aplicar filtros", e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao aplicar filtros");
        }
    }
    
    /**
     * Limpa todos os filtros
     */
    public void limparFiltros() {
        this.filtroTermo = null;
        this.filtroCategoria = null;
        this.filtroOrdenacao = "recentes";
        aplicarFiltros();
    }
    
    /**
     * Carrega os detalhes de uma notícia específica
     */
    public void carregarDetalhes(Long noticiaId) {
        try {
            this.noticiaAtual = noticiaService.buscarPorId(noticiaId);
            
            if (noticiaAtual == null) {
                logger.warn("Notícia não encontrada: {}", noticiaId);
                adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Notícia não encontrada");
                return;
            }
            
            // Incrementa visualizações
            noticiaService.incrementarVisualizacoes(noticiaId);
            
            carregarNoticiasRelacionadas();
            carregarComentarios();
            
            logger.debug("Detalhes carregados para notícia: {}", noticiaId);
            
        } catch (Exception e) {
            logger.error("Erro ao carregar detalhes da notícia: " + noticiaId, e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao carregar notícia");
        }
    }
    
    /**
     * Carrega notícias relacionadas
     */
    private void carregarNoticiasRelacionadas() {
        try {
            if (noticiaAtual != null) {
                this.noticiasRelacionadas = noticiaService.buscarRelacionadas(
                    noticiaAtual.getId(), 
                    LIMITE_RELACIONADAS
                );
            }
        } catch (Exception e) {
            logger.error("Erro ao carregar notícias relacionadas", e);
            this.noticiasRelacionadas = new ArrayList<>();
        }
    }
    
    /**
     * Carrega comentários da notícia atual
     */
    private void carregarComentarios() {
        try {
            if (noticiaAtual != null) {
                this.comentarios = comentarioService.buscarPorNoticia(
                    noticiaAtual.getId(), 
                    true, // apenas aprovados
                    0, 
                    LIMITE_COMENTARIOS
                );
            }
        } catch (Exception e) {
            logger.error("Erro ao carregar comentários", e);
            this.comentarios = new ArrayList<>();
        }
    }
    
    /**
     * Inicializa um novo comentário
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Formulário de comentário deve adaptar-se ao tema ativo
     */
    private void inicializarNovoComentario() {
        this.novoComentario = new ComentarioDTO();
        this.comentarioEnviado = false;
        this.mensagemComentario = null;
    }
    
    /**
     * Envia um novo comentário
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Feedback visual deve manter consistência com tema selecionado
     */
    public void enviarComentario() {
        try {
            if (noticiaAtual == null) {
                adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Notícia não encontrada");
                return;
            }
            
            if (novoComentario.getConteudo() == null || novoComentario.getConteudo().trim().isEmpty()) {
                adicionarMensagem(FacesMessage.SEVERITY_WARN, "Por favor, digite seu comentário");
                return;
            }
            
            if (novoComentario.getAutorNome() == null || novoComentario.getAutorNome().trim().isEmpty()) {
                adicionarMensagem(FacesMessage.SEVERITY_WARN, "Por favor, informe seu nome");
                return;
            }
            
            if (novoComentario.getAutorEmail() == null || novoComentario.getAutorEmail().trim().isEmpty()) {
                adicionarMensagem(FacesMessage.SEVERITY_WARN, "Por favor, informe seu e-mail");
                return;
            }
            
            // Configura dados do comentário
            novoComentario.setNoticiaId(noticiaAtual.getId());
            novoComentario.setDataCriacao(new Date());
            novoComentario.setAprovado(false); // Aguarda moderação
            
            // Salva o comentário
            comentarioService.criar(novoComentario);
            
            this.comentarioEnviado = true;
            this.mensagemComentario = "Comentário enviado com sucesso! Aguarde a moderação.";
            
            // Limpa o formulário
            inicializarNovoComentario();
            
            logger.info("Novo comentário enviado para notícia: {}", noticiaAtual.getId());
            
        } catch (Exception e) {
            logger.error("Erro ao enviar comentário", e);
            this.mensagemComentario = "Erro ao enviar comentário. Tente novamente.";
            this.comentarioEnviado = false;
        }
    }
    
    /**
     * Alterna o modo de visualização (grid/lista)
     */
    public void alternarModoVisualizacao() {
        this.modoVisualizacao = "grid".equals(modoVisualizacao) ? "lista" : "grid";
    }
    
    /**
     * Verifica se há notícias para exibir
     */
    public boolean hasNoticias() {
        return noticiasLazy != null && noticiasLazy.getRowCount() > 0;
    }
    
    /**
     * Verifica se há comentários para exibir
     */
    public boolean hasComentarios() {
        return comentarios != null && !comentarios.isEmpty();
    }
    
    /**
     * Obtém o número total de comentários da notícia atual
     */
    public int getTotalComentarios() {
        if (noticiaAtual == null) return 0;
        
        try {
            return comentarioService.contarPorNoticia(noticiaAtual.getId(), true);
        } catch (Exception e) {
            logger.error("Erro ao contar comentários", e);
            return 0;
        }
    }
    
    /**
     * Formata o resumo da notícia
     */
    public String getResumoFormatado(NoticiaDTO noticia, int limite) {
        if (noticia == null) return "";
        
        String resumo = noticia.getResumo();
        if (resumo == null || resumo.trim().isEmpty()) {
            resumo = noticia.getConteudo();
        }
        
        if (resumo == null) return "";
        
        if (resumo.length() <= limite) {
            return resumo;
        }
        
        return resumo.substring(0, limite) + "...";
    }
    
    /**
     * Obtém as categorias da notícia como string
     */
    public String getCategoriasTexto(NoticiaDTO noticia) {
        if (noticia == null || noticia.getCategorias() == null || noticia.getCategorias().isEmpty()) {
            return "";
        }
        
        return noticia.getCategorias().stream()
            .map(CategoriaDTO::getNome)
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
    }
    
    /**
     * Adiciona mensagem ao contexto JSF
     */
    private void adicionarMensagem(FacesMessage.Severity severity, String mensagem) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(severity, mensagem, null));
    }
    
    // Getters e Setters
    public LazyDataModel<NoticiaDTO> getNoticiasLazy() {
        return noticiasLazy;
    }
    
    public void setNoticiasLazy(LazyDataModel<NoticiaDTO> noticiasLazy) {
        this.noticiasLazy = noticiasLazy;
    }
    
    public List<CategoriaDTO> getCategorias() {
        return categorias;
    }
    
    public void setCategorias(List<CategoriaDTO> categorias) {
        this.categorias = categorias;
    }
    
    public String getFiltroTermo() {
        return filtroTermo;
    }
    
    public void setFiltroTermo(String filtroTermo) {
        this.filtroTermo = filtroTermo;
    }
    
    public Long getFiltroCategoria() {
        return filtroCategoria;
    }
    
    public void setFiltroCategoria(Long filtroCategoria) {
        this.filtroCategoria = filtroCategoria;
    }
    
    public String getFiltroOrdenacao() {
        return filtroOrdenacao;
    }
    
    public void setFiltroOrdenacao(String filtroOrdenacao) {
        this.filtroOrdenacao = filtroOrdenacao;
    }
    
    public String getModoVisualizacao() {
        return modoVisualizacao;
    }
    
    public void setModoVisualizacao(String modoVisualizacao) {
        this.modoVisualizacao = modoVisualizacao;
    }
    
    public NoticiaDTO getNoticiaAtual() {
        return noticiaAtual;
    }
    
    public void setNoticiaAtual(NoticiaDTO noticiaAtual) {
        this.noticiaAtual = noticiaAtual;
    }
    
    public List<NoticiaDTO> getNoticiasRelacionadas() {
        return noticiasRelacionadas;
    }
    
    public void setNoticiasRelacionadas(List<NoticiaDTO> noticiasRelacionadas) {
        this.noticiasRelacionadas = noticiasRelacionadas;
    }
    
    public List<ComentarioDTO> getComentarios() {
        return comentarios;
    }
    
    public void setComentarios(List<ComentarioDTO> comentarios) {
        this.comentarios = comentarios;
    }
    
    public ComentarioDTO getNovoComentario() {
        return novoComentario;
    }
    
    public void setNovoComentario(ComentarioDTO novoComentario) {
        this.novoComentario = novoComentario;
    }
    
    public boolean isComentarioEnviado() {
        return comentarioEnviado;
    }
    
    public void setComentarioEnviado(boolean comentarioEnviado) {
        this.comentarioEnviado = comentarioEnviado;
    }
    
    public String getMensagemComentario() {
        return mensagemComentario;
    }
    
    public void setMensagemComentario(String mensagemComentario) {
        this.mensagemComentario = mensagemComentario;
    }
}