package com.sistema.java.bean;

import com.sistema.java.model.entity.Categoria;
import com.sistema.java.model.dto.CategoriaDTO;
import com.sistema.java.service.CategoriaService;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.primefaces.model.SortMeta;
import org.primefaces.model.FilterMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Managed Bean para gerenciamento de categorias
 * Referência: Padrões de Desenvolvimento - project_rules.md
 * Referência: Controle de Acesso - project_rules.md
 */
@Component("categoriaBean")
@Scope("view")
public class CategoriaBean implements Serializable {
    
    private static final Logger logger = LoggerFactory.getLogger(CategoriaBean.class);
    
    @Autowired
    private CategoriaService categoriaService;
    
    // Dados
    private Categoria categoriaAtual;
    private List<Categoria> categorias;
    private LazyDataModel<Categoria> categoriasLazy;
    private Map<String, Object> filtros;
    
    // Estado da interface
    private boolean modoEdicao = false;
    private boolean dialogoAberto = false;
    
    @PostConstruct
    public void init() {
        try {
            logger.info("Inicializando CategoriaBean");
            inicializarDados();
            criarLazyDataModel();
        } catch (Exception e) {
            logger.error("Erro ao inicializar CategoriaBean", e);
            adicionarMensagemErro("Erro ao carregar categorias");
        }
    }
    
    /**
     * Inicializa os dados do bean
     */
    private void inicializarDados() {
        categoriaAtual = new Categoria();
        filtros = new HashMap<>();
        modoEdicao = false;
        dialogoAberto = false;
    }
    
    /**
     * Cria o modelo lazy para carregamento paginado
     * Referência: Padrões JSF - project_rules.md
     */
    private void criarLazyDataModel() {
        categoriasLazy = new LazyDataModel<Categoria>() {
            @Override
            public List<Categoria> load(int first, int pageSize, Map<String, SortMeta> sortBy, 
                                       Map<String, FilterMeta> filterBy) {
                try {
                    // Extrair termo de busca dos filtros
                    String termo = "";
                    Boolean ativa = null;
                    
                    if (filterBy != null) {
                         if (filterBy.containsKey("nome")) {
                             termo = (String) filterBy.get("nome").getFilterValue();
                         }
                         if (filterBy.containsKey("ativa")) {
                             ativa = (Boolean) filterBy.get("ativa").getFilterValue();
                         }
                     }
                    
                    // Determinar campo e direção da ordenação
                     String campo = "id";
                     boolean crescente = true;
                     
                     if (sortBy != null && !sortBy.isEmpty()) {
                         SortMeta sort = sortBy.values().iterator().next();
                         campo = sort.getField();
                         crescente = sort.getOrder() == SortOrder.ASCENDING;
                     }
                    
                    // Buscar dados com paginação
                    List<CategoriaDTO> categoriasDTO = categoriaService.buscarComPaginacao(
                        first, pageSize, campo, crescente, termo, ativa);
                    
                    // Converter DTOs para entidades
                     List<Categoria> resultado = categoriasDTO.stream()
                         .map(this::convertToEntity)
                         .collect(Collectors.toList());
                    
                    // Definir total de registros
                    long total = categoriaService.contarComFiltros(termo, ativa);
                    this.setRowCount((int) total);
                    
                    logger.debug("Carregadas {} categorias de {} total", resultado.size(), total);
                    
                    return resultado;
                    
                } catch (Exception e) {
                    logger.error("Erro ao carregar categorias lazy", e);
                    adicionarMensagemErro("Erro ao carregar categorias");
                    return List.of();
                }
            }
            
            @Override
             public int count(Map<String, FilterMeta> filterBy) {
                 // Extrair termo de busca dos filtros
                 String termo = "";
                 Boolean ativa = null;
                 
                 if (filterBy != null) {
                     if (filterBy.containsKey("nome")) {
                         termo = (String) filterBy.get("nome").getFilterValue();
                     }
                     if (filterBy.containsKey("ativa")) {
                         ativa = (Boolean) filterBy.get("ativa").getFilterValue();
                     }
                 }
                 
                 return (int) categoriaService.contarComFiltros(termo, ativa);
             }
             
             private Categoria convertToEntity(CategoriaDTO dto) {
                 Categoria categoria = new Categoria();
                 categoria.setId(dto.getId());
                 categoria.setNome(dto.getNome());
                 categoria.setDescricao(dto.getDescricao());
                 categoria.setAtiva(dto.getAtiva());
                 categoria.setDataCriacao(dto.getDataCriacao());
                 return categoria;
             }
         };
    }
    
    /**
     * Prepara nova categoria
     */
    public void novaCategoria() {
        logger.info("Preparando nova categoria");
        categoriaAtual = new Categoria();
        categoriaAtual.setAtiva(true); // Padrão ativo
        modoEdicao = false;
        dialogoAberto = true;
    }
    
    /**
     * Prepara edição de categoria
     */
    public void editarCategoria(Categoria categoria) {
        if (categoria == null) {
            logger.warn("Tentativa de editar categoria nula");
            adicionarMensagemErro("Categoria não encontrada");
            return;
        }
        
        logger.info("Preparando edição da categoria: {}", categoria.getId());
        categoriaAtual = categoria;
        modoEdicao = true;
        dialogoAberto = true;
    }
    
    /**
     * Salva categoria (nova ou editada)
     */
    public void salvarCategoria() {
        try {
            if (categoriaAtual == null) {
                adicionarMensagemErro("Dados da categoria inválidos");
                return;
            }
            
            // Validações básicas
            if (categoriaAtual.getNome() == null || categoriaAtual.getNome().trim().isEmpty()) {
                adicionarMensagemErro("Nome da categoria é obrigatório");
                return;
            }
            
            if (modoEdicao) {
                logger.info("Atualizando categoria: {}", categoriaAtual.getId());
                categoriaService.update(categoriaAtual.getId(), convertToDTO(categoriaAtual));
                adicionarMensagemSucesso("Categoria atualizada com sucesso");
            } else {
                logger.info("Criando nova categoria: {}", categoriaAtual.getNome());
                categoriaService.create(convertToDTO(categoriaAtual));
                adicionarMensagemSucesso("Categoria criada com sucesso");
            }
            
            fecharDialogo();
            
        } catch (Exception e) {
            logger.error("Erro ao salvar categoria", e);
            adicionarMensagemErro("Erro ao salvar categoria: " + e.getMessage());
        }
    }
    
    /**
     * Deleta categoria
     */
    public void deletarCategoria(Long id) {
        try {
            if (id == null) {
                adicionarMensagemErro("ID da categoria inválido");
                return;
            }
            
            Optional<CategoriaDTO> categoriaOpt = categoriaService.findById(id);
            if (!categoriaOpt.isPresent()) {
                adicionarMensagemErro("Categoria não encontrada");
                return;
            }
            
            CategoriaDTO categoriaDTO = categoriaOpt.get();
            
            // Verificar se pode ser deletada
            if (!categoriaService.podeSerRemovida(id)) {
                adicionarMensagemErro("Categoria não pode ser deletada pois possui notícias associadas");
                return;
            }
            
            logger.info("Deletando categoria: {} ({})", categoriaDTO.getNome(), id);
            categoriaService.delete(id);
            adicionarMensagemSucesso("Categoria deletada com sucesso");
            
        } catch (Exception e) {
            logger.error("Erro ao deletar categoria", e);
            adicionarMensagemErro("Erro ao deletar categoria: " + e.getMessage());
        }
    }
    
    /**
     * Ativa/desativa categoria
     */
    public void alternarStatusCategoria(Categoria categoria) {
        try {
            if (categoria == null) {
                adicionarMensagemErro("Categoria não selecionada");
                return;
            }
            
            // Alternar status
            if (categoria.getAtiva()) {
                categoriaService.desativar(categoria.getId());
                adicionarMensagemSucesso("Categoria desativada com sucesso");
            } else {
                categoriaService.ativar(categoria.getId());
                adicionarMensagemSucesso("Categoria ativada com sucesso");
            }
            
            logger.info("Status da categoria alterado: {} ({})", categoria.getNome(), categoria.getId());
            
        } catch (Exception e) {
            logger.error("Erro ao alterar status da categoria", e);
            adicionarMensagemErro("Erro ao alterar status da categoria");
        }
    }
    
    /**
     * Aplica filtros de pesquisa
     */
    public void aplicarFiltros() {
        logger.info("Aplicando filtros: {}", filtros);
        // O LazyDataModel será recarregado automaticamente
    }
    
    /**
     * Limpa todos os filtros
     */
    public void limparFiltros() {
        logger.info("Limpando filtros");
        filtros.clear();
    }
    
    /**
     * Fecha o diálogo de edição
     */
    public void fecharDialogo() {
        dialogoAberto = false;
        categoriaAtual = new Categoria();
        modoEdicao = false;
    }
    
    /**
     * Cancela a operação atual
     */
    public void cancelar() {
        logger.info("Cancelando operação");
        fecharDialogo();
    }
    
    /**
     * Converte entidade para DTO
     * 
     * @param categoria Entidade Categoria
     * @return CategoriaDTO
     */
    private CategoriaDTO convertToDTO(Categoria categoria) {
        if (categoria == null) {
            return null;
        }
        
        CategoriaDTO dto = new CategoriaDTO();
        dto.setId(categoria.getId());
        dto.setNome(categoria.getNome());
        dto.setDescricao(categoria.getDescricao());
        dto.setAtiva(categoria.getAtiva());
        dto.setDataCriacao(categoria.getDataCriacao());
        return dto;
    }
    
    // Métodos utilitários para mensagens
    private void adicionarMensagemSucesso(String mensagem) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", mensagem));
    }
    
    private void adicionarMensagemErro(String mensagem) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", mensagem));
    }
    
    private void adicionarMensagemAviso(String mensagem) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso", mensagem));
    }
    
    // Getters e Setters
    public Categoria getCategoriaAtual() {
        return categoriaAtual;
    }
    
    public void setCategoriaAtual(Categoria categoriaAtual) {
        this.categoriaAtual = categoriaAtual;
    }
    
    public List<Categoria> getCategorias() {
        return categorias;
    }
    
    public void setCategorias(List<Categoria> categorias) {
        this.categorias = categorias;
    }
    
    public LazyDataModel<Categoria> getCategoriasLazy() {
        return categoriasLazy;
    }
    
    public void setCategoriasLazy(LazyDataModel<Categoria> categoriasLazy) {
        this.categoriasLazy = categoriasLazy;
    }
    
    public Map<String, Object> getFiltros() {
        return filtros;
    }
    
    public void setFiltros(Map<String, Object> filtros) {
        this.filtros = filtros;
    }
    
    public boolean isModoEdicao() {
        return modoEdicao;
    }
    
    public void setModoEdicao(boolean modoEdicao) {
        this.modoEdicao = modoEdicao;
    }
    
    public boolean isDialogoAberto() {
        return dialogoAberto;
    }
    
    public void setDialogoAberto(boolean dialogoAberto) {
        this.dialogoAberto = dialogoAberto;
    }
}