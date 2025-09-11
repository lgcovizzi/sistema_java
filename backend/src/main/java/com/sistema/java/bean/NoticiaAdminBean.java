package com.sistema.java.bean;

import com.sistema.java.model.dto.NoticiaDTO;
import com.sistema.java.model.dto.CategoriaDTO;
import com.sistema.java.model.dto.UsuarioDTO;
import com.sistema.java.service.NoticiaService;
import com.sistema.java.service.CategoriaService;
import com.sistema.java.service.UsuarioService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ViewScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Managed Bean para administração de notícias.
 * Responsável por gerenciar CRUD e operações administrativas de notícias.
 */
@Named("noticiaAdminBean")
@ViewScoped
public class NoticiaAdminBean implements Serializable {
    
    private static final Logger logger = LoggerFactory.getLogger(NoticiaAdminBean.class);
    
    @Inject
    private NoticiaService noticiaService;
    
    @Inject
    private CategoriaService categoriaService;
    
    @Inject
    private UsuarioService usuarioService;
    
    // Propriedades para listagem
    private LazyDataModel<NoticiaDTO> noticiasLazy;
    private List<NoticiaDTO> noticiasSelecionadas;
    
    // Propriedades para filtros
    private String filtroTitulo;
    private Long filtroAutor;
    private Long filtroCategoria;
    private Boolean filtroPublicada;
    private Date dataInicio;
    private Date dataFim;
    private String termoPesquisa;
    
    // Propriedades para CRUD
    private NoticiaDTO noticiaEdicao;
    private NoticiaDTO noticiaVisualizacao;
    private boolean modoEdicao;
    private UploadedFile imagemUpload;
    private List<Long> categoriasSelecionadas;
    
    // Listas auxiliares
    private List<CategoriaDTO> categoriasDisponiveis;
    private List<UsuarioDTO> autoresDisponiveis;
    
    // Estatísticas
    private long totalNoticias;
    private long noticiasPublicadas;
    private long noticiasRascunho;
    private long noticiasPublicadasHoje;
    
    // Auto-save
    private boolean autoSaveAtivo = true;
    private String ultimoAutoSave;
    
    @PostConstruct
    public void init() {
        try {
            inicializarDados();
            configurarLazyModel();
            carregarEstatisticas();
            carregarListasAuxiliares();
        } catch (Exception e) {
            logger.error("Erro ao inicializar NoticiaAdminBean", e);
        }
    }
    
    /**
     * Inicializa os dados necessários
     */
    private void inicializarDados() {
        this.noticiasSelecionadas = new ArrayList<>();
        this.categoriasSelecionadas = new ArrayList<>();
        inicializarNoticiaEdicao();
    }
    
    /**
     * Carrega listas auxiliares
     */
    private void carregarListasAuxiliares() {
        try {
            this.categoriasDisponiveis = categoriaService.buscarAtivas();
            this.autoresDisponiveis = usuarioService.buscarAtivos();
        } catch (Exception e) {
            logger.error("Erro ao carregar listas auxiliares", e);
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
                    return (int) noticiaService.contarComFiltros(
                        filtroTitulo, filtroAutor, filtroCategoria, filtroPublicada, 
                        dataInicio, dataFim, termoPesquisa
                    );
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
                        filtroTitulo, filtroAutor, filtroCategoria, filtroPublicada,
                        dataInicio, dataFim, termoPesquisa,
                        ordenacao, pagina, pageSize
                    );
                } catch (Exception e) {
                    logger.error("Erro ao carregar notícias", e);
                    return new ArrayList<>();
                }
            }
        };
        
        this.noticiasLazy.setRowCount(20);
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
        return "dataCriacao DESC";
    }
    
    /**
     * Carrega as estatísticas das notícias
     */
    private void carregarEstatisticas() {
        try {
            this.totalNoticias = noticiaService.contar();
            this.noticiasPublicadas = noticiaService.contarPublicadas();
            this.noticiasRascunho = noticiaService.contarRascunhos();
            this.noticiasPublicadasHoje = noticiaService.contarPublicadasHoje();
            
            logger.debug("Estatísticas carregadas - Total: {}, Publicadas: {}, Rascunho: {}, Hoje: {}", 
                        totalNoticias, noticiasPublicadas, noticiasRascunho, noticiasPublicadasHoje);
        } catch (Exception e) {
            logger.error("Erro ao carregar estatísticas", e);
        }
    }
    
    /**
     * Aplica os filtros de pesquisa
     */
    public void aplicarFiltros() {
        try {
            configurarLazyModel();
            logger.debug("Filtros aplicados - Título: {}, Autor: {}, Categoria: {}, Publicada: {}", 
                        filtroTitulo, filtroAutor, filtroCategoria, filtroPublicada);
        } catch (Exception e) {
            logger.error("Erro ao aplicar filtros", e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao aplicar filtros");
        }
    }
    
    /**
     * Limpa todos os filtros
     */
    public void limparFiltros() {
        this.filtroTitulo = null;
        this.filtroAutor = null;
        this.filtroCategoria = null;
        this.filtroPublicada = null;
        this.dataInicio = null;
        this.dataFim = null;
        this.termoPesquisa = null;
        aplicarFiltros();
    }
    
    /**
     * Pesquisa notícias por termo
     */
    public void pesquisar() {
        aplicarFiltros();
    }
    
    /**
     * Inicializa uma nova notícia para edição
     */
    private void inicializarNoticiaEdicao() {
        this.noticiaEdicao = new NoticiaDTO();
        this.noticiaEdicao.setPublicada(false);
        this.noticiaEdicao.setDataCriacao(new Date());
        this.modoEdicao = false;
        this.categoriasSelecionadas = new ArrayList<>();
        this.imagemUpload = null;
    }
    
    /**
     * Prepara para criar uma nova notícia
     */
    public void novaNoticia() {
        inicializarNoticiaEdicao();
        this.modoEdicao = false;
    }
    
    /**
     * Prepara para editar uma notícia existente
     */
    public void editarNoticia(NoticiaDTO noticia) {
        try {
            this.noticiaEdicao = noticiaService.buscarPorId(noticia.getId());
            this.modoEdicao = true;
            this.imagemUpload = null;
            
            // Carrega categorias selecionadas
            this.categoriasSelecionadas = noticiaEdicao.getCategorias() != null ?
                noticiaEdicao.getCategorias().stream().map(CategoriaDTO::getId).toList() :
                new ArrayList<>();
                
        } catch (Exception e) {
            logger.error("Erro ao carregar notícia para edição: " + noticia.getId(), e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao carregar notícia");
        }
    }
    
    /**
     * Visualiza detalhes de uma notícia
     */
    public void visualizarNoticia(NoticiaDTO noticia) {
        try {
            this.noticiaVisualizacao = noticiaService.buscarPorId(noticia.getId());
        } catch (Exception e) {
            logger.error("Erro ao carregar notícia para visualização: " + noticia.getId(), e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao carregar notícia");
        }
    }
    
    /**
     * Salva a notícia (criar ou atualizar)
     */
    public void salvarNoticia() {
        try {
            // Validações
            if (!validarNoticia()) {
                return;
            }
            
            // Processa upload de imagem
            if (imagemUpload != null && imagemUpload.getSize() > 0) {
                String caminhoImagem = processarUploadImagem();
                if (caminhoImagem != null) {
                    noticiaEdicao.setImagemUrl(caminhoImagem);
                }
            }
            
            // Define categorias
            if (!categoriasSelecionadas.isEmpty()) {
                List<CategoriaDTO> categorias = categoriasSelecionadas.stream()
                    .map(id -> categoriasDisponiveis.stream()
                        .filter(c -> c.getId().equals(id))
                        .findFirst().orElse(null))
                    .filter(c -> c != null)
                    .toList();
                noticiaEdicao.setCategorias(categorias);
            }
            
            if (modoEdicao) {
                // Atualizar notícia existente
                noticiaEdicao.setDataAtualizacao(new Date());
                noticiaService.atualizar(noticiaEdicao.getId(), noticiaEdicao);
                adicionarMensagem(FacesMessage.SEVERITY_INFO, "Notícia atualizada com sucesso!");
                
                logger.info("Notícia atualizada: {}", noticiaEdicao.getTitulo());
            } else {
                // Criar nova notícia
                noticiaEdicao.setDataCriacao(new Date());
                noticiaService.criar(noticiaEdicao);
                adicionarMensagem(FacesMessage.SEVERITY_INFO, "Notícia criada com sucesso!");
                
                logger.info("Nova notícia criada: {}", noticiaEdicao.getTitulo());
            }
            
            // Atualiza a listagem e estatísticas
            configurarLazyModel();
            carregarEstatisticas();
            
        } catch (Exception e) {
            logger.error("Erro ao salvar notícia", e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao salvar notícia: " + e.getMessage());
        }
    }
    
    /**
     * Processa o upload da imagem
     */
    private String processarUploadImagem() {
        try {
            // Validações da imagem
            if (imagemUpload.getSize() > 5 * 1024 * 1024) { // 5MB
                adicionarMensagem(FacesMessage.SEVERITY_WARN, "Imagem muito grande. Máximo 5MB.");
                return null;
            }
            
            String contentType = imagemUpload.getContentType();
            if (!contentType.startsWith("image/")) {
                adicionarMensagem(FacesMessage.SEVERITY_WARN, "Arquivo deve ser uma imagem.");
                return null;
            }
            
            // Aqui você implementaria o upload real para um diretório ou serviço de storage
            // Por enquanto, retornamos um caminho simulado
            String nomeArquivo = "noticia_" + System.currentTimeMillis() + "_" + imagemUpload.getFileName();
            String caminhoImagem = "/uploads/noticias/" + nomeArquivo;
            
            logger.info("Imagem processada: {}", caminhoImagem);
            return caminhoImagem;
            
        } catch (Exception e) {
            logger.error("Erro ao processar upload de imagem", e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao processar imagem");
            return null;
        }
    }
    
    /**
     * Valida os dados da notícia
     */
    private boolean validarNoticia() {
        if (noticiaEdicao.getTitulo() == null || noticiaEdicao.getTitulo().trim().isEmpty()) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN, "Título é obrigatório");
            return false;
        }
        
        if (noticiaEdicao.getConteudo() == null || noticiaEdicao.getConteudo().trim().isEmpty()) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN, "Conteúdo é obrigatório");
            return false;
        }
        
        if (noticiaEdicao.getAutorId() == null) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN, "Autor é obrigatório");
            return false;
        }
        
        if (categoriasSelecionadas.isEmpty()) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN, "Selecione pelo menos uma categoria");
            return false;
        }
        
        return true;
    }
    
    /**
     * Exclui uma notícia
     */
    public void excluirNoticia(NoticiaDTO noticia) {
        try {
            noticiaService.excluir(noticia.getId());
            adicionarMensagem(FacesMessage.SEVERITY_INFO, "Notícia excluída com sucesso!");
            
            configurarLazyModel();
            carregarEstatisticas();
            
            logger.info("Notícia excluída: {}", noticia.getTitulo());
            
        } catch (Exception e) {
            logger.error("Erro ao excluir notícia: " + noticia.getId(), e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao excluir notícia: " + e.getMessage());
        }
    }
    
    /**
     * Publica/despublica uma notícia
     */
    public void alterarStatusPublicacao(NoticiaDTO noticia) {
        try {
            boolean novoStatus = !noticia.getPublicada();
            
            if (novoStatus) {
                noticiaService.publicar(noticia.getId());
            } else {
                noticiaService.despublicar(noticia.getId());
            }
            
            noticia.setPublicada(novoStatus);
            
            String status = novoStatus ? "publicada" : "despublicada";
            adicionarMensagem(FacesMessage.SEVERITY_INFO, "Notícia " + status + " com sucesso!");
            
            carregarEstatisticas();
            
            logger.info("Status de publicação alterado: {} - {}", noticia.getTitulo(), status);
            
        } catch (Exception e) {
            logger.error("Erro ao alterar status de publicação: " + noticia.getId(), e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao alterar status de publicação");
        }
    }
    
    /**
     * Publica notícias selecionadas
     */
    public void publicarSelecionadas() {
        try {
            if (noticiasSelecionadas.isEmpty()) {
                adicionarMensagem(FacesMessage.SEVERITY_WARN, "Selecione pelo menos uma notícia");
                return;
            }
            
            List<Long> ids = noticiasSelecionadas.stream()
                .map(NoticiaDTO::getId)
                .toList();
            
            noticiaService.publicarEmLote(ids);
            
            adicionarMensagem(FacesMessage.SEVERITY_INFO, 
                noticiasSelecionadas.size() + " notícia(s) publicada(s) com sucesso!");
            
            configurarLazyModel();
            carregarEstatisticas();
            noticiasSelecionadas.clear();
            
            logger.info("Notícias publicadas em lote: {}", ids.size());
            
        } catch (Exception e) {
            logger.error("Erro ao publicar notícias selecionadas", e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao publicar notícias selecionadas");
        }
    }
    
    /**
     * Despublica notícias selecionadas
     */
    public void despublicarSelecionadas() {
        try {
            if (noticiasSelecionadas.isEmpty()) {
                adicionarMensagem(FacesMessage.SEVERITY_WARN, "Selecione pelo menos uma notícia");
                return;
            }
            
            List<Long> ids = noticiasSelecionadas.stream()
                .map(NoticiaDTO::getId)
                .toList();
            
            noticiaService.despublicarEmLote(ids);
            
            adicionarMensagem(FacesMessage.SEVERITY_INFO, 
                noticiasSelecionadas.size() + " notícia(s) despublicada(s) com sucesso!");
            
            configurarLazyModel();
            carregarEstatisticas();
            noticiasSelecionadas.clear();
            
            logger.info("Notícias despublicadas em lote: {}", ids.size());
            
        } catch (Exception e) {
            logger.error("Erro ao despublicar notícias selecionadas", e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao despublicar notícias selecionadas");
        }
    }
    
    /**
     * Exclui notícias selecionadas
     */
    public void excluirSelecionadas() {
        try {
            if (noticiasSelecionadas.isEmpty()) {
                adicionarMensagem(FacesMessage.SEVERITY_WARN, "Selecione pelo menos uma notícia");
                return;
            }
            
            List<Long> ids = noticiasSelecionadas.stream()
                .map(NoticiaDTO::getId)
                .toList();
            
            noticiaService.excluirEmLote(ids);
            
            adicionarMensagem(FacesMessage.SEVERITY_INFO, 
                noticiasSelecionadas.size() + " notícia(s) excluída(s) com sucesso!");
            
            configurarLazyModel();
            carregarEstatisticas();
            noticiasSelecionadas.clear();
            
            logger.info("Notícias excluídas em lote: {}", ids.size());
            
        } catch (Exception e) {
            logger.error("Erro ao excluir notícias selecionadas", e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao excluir notícias selecionadas");
        }
    }
    
    /**
     * Auto-save da notícia em edição
     */
    public void autoSave() {
        if (!autoSaveAtivo || noticiaEdicao == null) {
            return;
        }
        
        try {
            if (modoEdicao && noticiaEdicao.getId() != null) {
                // Salva apenas se estiver editando uma notícia existente
                noticiaEdicao.setDataAtualizacao(new Date());
                noticiaService.salvarRascunho(noticiaEdicao.getId(), noticiaEdicao);
                
                this.ultimoAutoSave = "Salvo automaticamente às " + 
                    java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                
                logger.debug("Auto-save realizado para notícia: {}", noticiaEdicao.getId());
            }
        } catch (Exception e) {
            logger.error("Erro no auto-save", e);
        }
    }
    
    /**
     * Conta caracteres do conteúdo
     */
    public int contarCaracteres() {
        return noticiaEdicao != null && noticiaEdicao.getConteudo() != null ? 
            noticiaEdicao.getConteudo().length() : 0;
    }
    
    /**
     * Conta palavras do conteúdo
     */
    public int contarPalavras() {
        if (noticiaEdicao == null || noticiaEdicao.getConteudo() == null || 
            noticiaEdicao.getConteudo().trim().isEmpty()) {
            return 0;
        }
        
        return noticiaEdicao.getConteudo().trim().split("\\s+").length;
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
    
    public List<NoticiaDTO> getNoticiasSelecionadas() {
        return noticiasSelecionadas;
    }
    
    public void setNoticiasSelecionadas(List<NoticiaDTO> noticiasSelecionadas) {
        this.noticiasSelecionadas = noticiasSelecionadas;
    }
    
    public String getFiltroTitulo() {
        return filtroTitulo;
    }
    
    public void setFiltroTitulo(String filtroTitulo) {
        this.filtroTitulo = filtroTitulo;
    }
    
    public Long getFiltroAutor() {
        return filtroAutor;
    }
    
    public void setFiltroAutor(Long filtroAutor) {
        this.filtroAutor = filtroAutor;
    }
    
    public Long getFiltroCategoria() {
        return filtroCategoria;
    }
    
    public void setFiltroCategoria(Long filtroCategoria) {
        this.filtroCategoria = filtroCategoria;
    }
    
    public Boolean getFiltroPublicada() {
        return filtroPublicada;
    }
    
    public void setFiltroPublicada(Boolean filtroPublicada) {
        this.filtroPublicada = filtroPublicada;
    }
    
    public Date getDataInicio() {
        return dataInicio;
    }
    
    public void setDataInicio(Date dataInicio) {
        this.dataInicio = dataInicio;
    }
    
    public Date getDataFim() {
        return dataFim;
    }
    
    public void setDataFim(Date dataFim) {
        this.dataFim = dataFim;
    }
    
    public String getTermoPesquisa() {
        return termoPesquisa;
    }
    
    public void setTermoPesquisa(String termoPesquisa) {
        this.termoPesquisa = termoPesquisa;
    }
    
    public NoticiaDTO getNoticiaEdicao() {
        return noticiaEdicao;
    }
    
    public void setNoticiaEdicao(NoticiaDTO noticiaEdicao) {
        this.noticiaEdicao = noticiaEdicao;
    }
    
    public NoticiaDTO getNoticiaVisualizacao() {
        return noticiaVisualizacao;
    }
    
    public void setNoticiaVisualizacao(NoticiaDTO noticiaVisualizacao) {
        this.noticiaVisualizacao = noticiaVisualizacao;
    }
    
    public boolean isModoEdicao() {
        return modoEdicao;
    }
    
    public void setModoEdicao(boolean modoEdicao) {
        this.modoEdicao = modoEdicao;
    }
    
    public UploadedFile getImagemUpload() {
        return imagemUpload;
    }
    
    public void setImagemUpload(UploadedFile imagemUpload) {
        this.imagemUpload = imagemUpload;
    }
    
    public List<Long> getCategoriasSelecionadas() {
        return categoriasSelecionadas;
    }
    
    public void setCategoriasSelecionadas(List<Long> categoriasSelecionadas) {
        this.categoriasSelecionadas = categoriasSelecionadas;
    }
    
    public List<CategoriaDTO> getCategoriasDisponiveis() {
        return categoriasDisponiveis;
    }
    
    public void setCategoriasDisponiveis(List<CategoriaDTO> categoriasDisponiveis) {
        this.categoriasDisponiveis = categoriasDisponiveis;
    }
    
    public List<UsuarioDTO> getAutoresDisponiveis() {
        return autoresDisponiveis;
    }
    
    public void setAutoresDisponiveis(List<UsuarioDTO> autoresDisponiveis) {
        this.autoresDisponiveis = autoresDisponiveis;
    }
    
    public long getTotalNoticias() {
        return totalNoticias;
    }
    
    public void setTotalNoticias(long totalNoticias) {
        this.totalNoticias = totalNoticias;
    }
    
    public long getNoticiasPublicadas() {
        return noticiasPublicadas;
    }
    
    public void setNoticiasPublicadas(long noticiasPublicadas) {
        this.noticiasPublicadas = noticiasPublicadas;
    }
    
    public long getNoticiasRascunho() {
        return noticiasRascunho;
    }
    
    public void setNoticiasRascunho(long noticiasRascunho) {
        this.noticiasRascunho = noticiasRascunho;
    }
    
    public long getNoticiasPublicadasHoje() {
        return noticiasPublicadasHoje;
    }
    
    public void setNoticiasPublicadasHoje(long noticiasPublicadasHoje) {
        this.noticiasPublicadasHoje = noticiasPublicadasHoje;
    }
    
    public boolean isAutoSaveAtivo() {
        return autoSaveAtivo;
    }
    
    public void setAutoSaveAtivo(boolean autoSaveAtivo) {
        this.autoSaveAtivo = autoSaveAtivo;
    }
    
    public String getUltimoAutoSave() {
        return ultimoAutoSave;
    }
    
    public void setUltimoAutoSave(String ultimoAutoSave) {
        this.ultimoAutoSave = ultimoAutoSave;
    }
}