package com.sistema.java.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO para comentários
 * Referência: Padrões para Entidades JPA - project_rules.md
 * Referência: Controle de Acesso - project_rules.md
 */
public class ComentarioDTO {
    
    private Long id;
    
    @NotBlank(message = "Conteúdo é obrigatório")
    @Size(max = 1000, message = "Conteúdo deve ter no máximo 1000 caracteres")
    private String conteudo;
    
    private Boolean aprovado;
    private LocalDateTime dataCriacao;
    
    private UsuarioDTO autor;
    private NoticiaDTO noticia;
    
    // Construtores
    public ComentarioDTO() {}
    
    public ComentarioDTO(Long id, String conteudo, Boolean aprovado, 
                        LocalDateTime dataCriacao) {
        this.id = id;
        this.conteudo = conteudo;
        this.aprovado = aprovado;
        this.dataCriacao = dataCriacao;
    }
    
    // Getters e Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getConteudo() {
        return conteudo;
    }
    
    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }
    
    public Boolean getAprovado() {
        return aprovado;
    }
    
    public void setAprovado(Boolean aprovado) {
        this.aprovado = aprovado;
    }
    
    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
    
    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
    
    public UsuarioDTO getAutor() {
        return autor;
    }
    
    public void setAutor(UsuarioDTO autor) {
        this.autor = autor;
    }
    
    public NoticiaDTO getNoticia() {
        return noticia;
    }
    
    public void setNoticia(NoticiaDTO noticia) {
        this.noticia = noticia;
    }
}