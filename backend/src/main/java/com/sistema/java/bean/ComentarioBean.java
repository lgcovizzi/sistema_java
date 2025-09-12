package com.sistema.java.bean;

import com.sistema.java.model.dto.ComentarioDTO;
import com.sistema.java.model.dto.NoticiaDTO;
import com.sistema.java.model.entity.Comentario;
import com.sistema.java.model.entity.Noticia;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.service.ComentarioService;
import com.sistema.java.service.NoticiaService;
import com.sistema.java.service.AuthService;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.primefaces.model.SortMeta;
import org.primefaces.model.FilterMeta;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Bean para gerenciamento de comentários
 * Referência: Sistema de Temas Claros e Escuros - project_rules.md
 * Referência: Controle de Acesso - project_rules.md
 */
@Named
@SessionScoped
public class ComentarioBean implements Serializable {

    @Inject
    private ComentarioService comentarioService;

    @Inject
    private NoticiaService noticiaService;

    @Inject
    private AuthService authService;

    private LazyDataModel<ComentarioDTO> comentariosLazy;
    private ComentarioDTO novoComentario;
    private ComentarioDTO comentarioAtual;
    private Long noticiaId;
    private Map<String, Object> filtros = new HashMap<>();

    @PostConstruct
    public void init() {
        novoComentario = new ComentarioDTO();
        comentarioAtual = new ComentarioDTO();
        initLazyModel();
    }

    private void initLazyModel() {
        comentariosLazy = new LazyDataModel<ComentarioDTO>() {
            @Override
            public List<ComentarioDTO> load(int first, int pageSize, Map<String, SortMeta> sortBy, 
                                           Map<String, FilterMeta> filterBy) {
                // Implementação do lazy loading
                String sortField = null;
                boolean ascending = true;
                
                if (sortBy != null && !sortBy.isEmpty()) {
                    SortMeta sortMeta = sortBy.values().iterator().next();
                    sortField = sortMeta.getField();
                    ascending = sortMeta.getOrder() == SortOrder.ASCENDING;
                }
                
                Map<String, Object> filters = new HashMap<>();
                if (filterBy != null) {
                    for (Map.Entry<String, FilterMeta> entry : filterBy.entrySet()) {
                        filters.put(entry.getKey(), entry.getValue().getFilterValue());
                    }
                }
                
                String orderBy = sortField != null ? sortField : "dataCriacao";
                
                List<Comentario> comentarios = comentarioService.listarComFiltros(filters, first, pageSize, orderBy, ascending);
                long total = comentarioService.contarComFiltros(filters);
                
                this.setRowCount((int) total);
                
                return comentarios.stream()
                    .map(this::convertToDTO)
                    .toList();
            }
            
            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                Map<String, Object> filters = new HashMap<>();
                if (filterBy != null) {
                    for (Map.Entry<String, FilterMeta> entry : filterBy.entrySet()) {
                        filters.put(entry.getKey(), entry.getValue().getFilterValue());
                    }
                }
                return (int) comentarioService.contarComFiltros(filters);
            }
            
            private ComentarioDTO convertToDTO(Comentario comentario) {
                ComentarioDTO dto = new ComentarioDTO();
                dto.setId(comentario.getId());
                dto.setConteudo(comentario.getConteudo());
                dto.setAprovado(comentario.getAprovado());
                dto.setDataCriacao(comentario.getDataCriacao());
                if (comentario.getAutor() != null) {
                    dto.setAutorNome(comentario.getAutor().getNome() + " " + comentario.getAutor().getSobrenome());
                }
                if (comentario.getNoticia() != null) {
                    dto.setNoticiaId(comentario.getNoticia().getId());
                    dto.setNoticiaTitulo(comentario.getNoticia().getTitulo());
                }
                return dto;
            }
        };
    }

    public void criarComentario() {
        try {
            Usuario usuario = authService.getUsuarioLogado();
            if (usuario == null) {
                return;
            }
            
            Optional<NoticiaDTO> noticiaOpt = noticiaService.findById(comentarioAtual.getNoticiaId());
             if (noticiaOpt.isEmpty()) {
                 return;
             }
             NoticiaDTO noticiaDTO = noticiaOpt.get();
            
            comentarioService.criarComentario(comentarioAtual, usuario);
            comentarioAtual = new ComentarioDTO();
        } catch (Exception e) {
            // Log error
        }
    }

    public void aprovarComentario(Long id) {
        try {
            comentarioService.aprovarComentario(id);
        } catch (Exception e) {
            // Log error
        }
    }

    public void rejeitarComentario(Long id) {
        try {
            comentarioService.rejeitarComentario(id);
        } catch (Exception e) {
            // Log error
        }
    }

    public void deletarComentario(Long id) {
        try {
            comentarioService.deletarComentario(id);
        } catch (Exception e) {
            // Log error
        }
    }

    public void novoComentario() {
        comentarioAtual = new ComentarioDTO();
    }

    public void novoComentarioParaNoticia(Long noticiaId) {
        comentarioAtual = new ComentarioDTO();
        comentarioAtual.setNoticiaId(noticiaId);
    }
    
    public void limparFiltros() {
        // Método para limpar filtros - implementação futura
    }
    
    public void cancelarEdicao() {
        comentarioAtual = null;
    }

    // Getters e Setters
    public LazyDataModel<ComentarioDTO> getComentariosLazy() {
        return comentariosLazy;
    }

    public void setComentariosLazy(LazyDataModel<ComentarioDTO> comentariosLazy) {
        this.comentariosLazy = comentariosLazy;
    }

    public ComentarioDTO getNovoComentario() {
        return novoComentario;
    }

    public void setNovoComentario(ComentarioDTO novoComentario) {
        this.novoComentario = novoComentario;
    }

    public ComentarioDTO getComentarioAtual() {
        return comentarioAtual;
    }

    public void setComentarioAtual(ComentarioDTO comentarioAtual) {
        this.comentarioAtual = comentarioAtual;
    }

    public Long getNoticiaId() {
        return noticiaId;
    }

    public void setNoticiaId(Long noticiaId) {
        this.noticiaId = noticiaId;
    }

    public Map<String, Object> getFiltros() {
        return filtros;
    }

    public void setFiltros(Map<String, Object> filtros) {
        this.filtros = filtros;
    }
}