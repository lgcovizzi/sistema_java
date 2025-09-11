package com.sistema.java.bean;

import com.sistema.java.model.dto.CategoriaDTO;
import com.sistema.java.service.CategoriaService;
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
import java.util.List;
import java.util.Map;

/**
 * Managed Bean para gerenciamento de categorias.
 * Referência: Implementar beans JSF - project_rules.md
 * Referência: Padrões para Entidades JPA - project_rules.md
 */
@Named("categoriaBean")
@ViewScoped
public class CategoriaBean implements Serializable {
    
    private static final Logger logger = LoggerFactory.getLogger(CategoriaBean.class);
    
    @Inject
    private CategoriaService categoriaService;
    
    // Propriedades para listagem
    private LazyDataModel<CategoriaDTO> categoriasLazy;
    private List<CategoriaDTO> categorias;
    private String filtroTermo;
    private String filtroStatus = "todas"; // todas, ativas, inativas
    
    // Propriedades para CRUD
    private CategoriaDTO categoriaSelecionada;
    private CategoriaDTO novaCategoria;
    private boolean modoEdicao = false;
    private boolean dialogoVisivel = false;
    
    // Configurações
    private static final int ITENS_POR_PAGINA = 20;
    
    @PostConstruct
    public void init() {
        try {
            inicializarDados();
            configurarLazyModel();
        } catch (Exception e) {
            logger.error("Erro ao inicializar CategoriaBean", e);
            addErrorMessage("Erro ao carregar categorias.");
        }
    }
    
    /**
     * Inicializa os dados necessários
     * Referência: Inicialização de beans JSF - project_rules.md
     */
    private void inicializarDados() {
        carregarCategorias();
        novaCategoria = new CategoriaDTO();
    }
    
    /**
     * Configura o modelo lazy para paginação
     * Referência: Componentes PrimeFaces - project_rules.md
     */
    private void configurarLazyModel() {
        categoriasLazy = new LazyDataModel<CategoriaDTO>() {
            @Override
            public List<CategoriaDTO> load(int first, int pageSize, String sortField, 
                                         SortOrder sortOrder, Map<String, Object> filters) {
                try {
                    // Aplicar filtros
                    String termo = filtroTermo;
                    Boolean ativa = null;
                    
                    if ("ativas".equals(filtroStatus)) {
                        ativa = true;
                    } else if ("inativas".equals(filtroStatus)) {
                        ativa = false;
                    }
                    
                    // Buscar categorias com paginação
                    List<CategoriaDTO> resultado = categoriaService.buscarComPaginacao(
                        first, pageSize, sortField, sortOrder, termo, ativa);
                    
                    // Definir total de registros
                    long total = categoriaService.contarComFiltros(termo, ativa);
                    this.setRowCount((int) total);
                    
                    return resultado;
                } catch (Exception e) {
                    logger.error("Erro ao carregar categorias lazy", e);
                    addErrorMessage("Erro ao carregar categorias.");
                    return List.of();
                }
            }
        };
        
        categoriasLazy.setPageSize(ITENS_POR_PAGINA);
    }
    
    /**
     * Carrega todas as categorias ativas
     */
    public void carregarCategorias() {
        try {
            categorias = categoriaService.listarAtivas();
        } catch (Exception e) {
            logger.error("Erro ao carregar categorias", e);
            addErrorMessage("Erro ao carregar categorias.");
        }
    }
    
    /**
     * Aplica filtros na listagem
     */
    public void aplicarFiltros() {
        // O lazy model será recarregado automaticamente
    }
    
    /**
     * Limpa todos os filtros
     */
    public void limparFiltros() {
        filtroTermo = null;
        filtroStatus = "todas";
        aplicarFiltros();
    }
    
    /**
     * Prepara para criar nova categoria
     */
    public void prepararNova() {
        novaCategoria = new CategoriaDTO();
        modoEdicao = false;
        dialogoVisivel = true;
    }
    
    /**
     * Prepara para editar categoria existente
     */
    public void prepararEdicao(CategoriaDTO categoria) {
        if (categoria != null) {
            novaCategoria = new CategoriaDTO();
            novaCategoria.setId(categoria.getId());
            novaCategoria.setNome(categoria.getNome());
            novaCategoria.setDescricao(categoria.getDescricao());
            novaCategoria.setAtiva(categoria.getAtiva());
            
            modoEdicao = true;
            dialogoVisivel = true;
        }
    }
    
    /**
     * Salva categoria (nova ou editada)
     * Referência: Validação de entrada no backend - project_rules.md
     */
    public void salvar() {
        try {
            if (modoEdicao) {
                categoriaService.atualizar(novaCategoria);
                addInfoMessage("Categoria atualizada com sucesso!");
            } else {
                categoriaService.criar(novaCategoria);
                addInfoMessage("Categoria criada com sucesso!");
            }
            
            dialogoVisivel = false;
            carregarCategorias();
            
        } catch (Exception e) {
            logger.error("Erro ao salvar categoria", e);
            addErrorMessage("Erro ao salvar categoria: " + e.getMessage());
        }
    }
    
    /**
     * Ativa/desativa categoria
     */
    public void alterarStatus(CategoriaDTO categoria) {
        try {
            categoria.setAtiva(!categoria.getAtiva());
            categoriaService.atualizar(categoria);
            
            String status = categoria.getAtiva() ? "ativada" : "desativada";
            addInfoMessage("Categoria " + status + " com sucesso!");
            
            carregarCategorias();
            
        } catch (Exception e) {
            logger.error("Erro ao alterar status da categoria", e);
            addErrorMessage("Erro ao alterar status da categoria.");
        }
    }
    
    /**
     * Exclui categoria
     */
    public void excluir(CategoriaDTO categoria) {
        try {
            categoriaService.excluir(categoria.getId());
            addInfoMessage("Categoria excluída com sucesso!");
            carregarCategorias();
            
        } catch (Exception e) {
            logger.error("Erro ao excluir categoria", e);
            addErrorMessage("Erro ao excluir categoria: " + e.getMessage());
        }
    }
    
    /**
     * Cancela operação e fecha diálogo
     */
    public void cancelar() {
        dialogoVisivel = false;
        novaCategoria = new CategoriaDTO();
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
    public LazyDataModel<CategoriaDTO> getCategoriasLazy() {
        return categoriasLazy;
    }
    
    public List<CategoriaDTO> getCategorias() {
        return categorias;
    }
    
    public String getFiltroTermo() {
        return filtroTermo;
    }
    
    public void setFiltroTermo(String filtroTermo) {
        this.filtroTermo = filtroTermo;
    }
    
    public String getFiltroStatus() {
        return filtroStatus;
    }
    
    public void setFiltroStatus(String filtroStatus) {
        this.filtroStatus = filtroStatus;
    }
    
    public CategoriaDTO getCategoriaSelecionada() {
        return categoriaSelecionada;
    }
    
    public void setCategoriaSelecionada(CategoriaDTO categoriaSelecionada) {
        this.categoriaSelecionada = categoriaSelecionada;
    }
    
    public CategoriaDTO getNovaCategoria() {
        return novaCategoria;
    }
    
    public void setNovaCategoria(CategoriaDTO novaCategoria) {
        this.novaCategoria = novaCategoria;
    }
    
    public boolean isModoEdicao() {
        return modoEdicao;
    }
    
    public boolean isDialogoVisivel() {
        return dialogoVisivel;
    }
    
    public void setDialogoVisivel(boolean dialogoVisivel) {
        this.dialogoVisivel = dialogoVisivel;
    }
}