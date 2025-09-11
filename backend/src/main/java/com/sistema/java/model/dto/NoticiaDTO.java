package com.sistema.java.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class NoticiaDTO {
    
    private Long id;
    
    @NotBlank(message = "Título é obrigatório")
    @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
    private String titulo;
    
    @NotBlank(message = "Conteúdo é obrigatório")
    private String conteudo;
    
    @Size(max = 500, message = "Resumo deve ter no máximo 500 caracteres")
    private String resumo;
    
    private Boolean publicada;
    private LocalDateTime dataPublicacao;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
    
    private UsuarioDTO autor;
    private List<CategoriaDTO> categorias;
    private Integer totalComentarios;
    
    // Construtores
    public NoticiaDTO() {}
    
    public NoticiaDTO(Long id, String titulo, String conteudo, String resumo, 
                     Boolean publicada, LocalDateTime dataPublicacao,
                     LocalDateTime dataCriacao, LocalDateTime dataAtualizacao) {
        this.id = id;
        this.titulo = titulo;
        this.conteudo = conteudo;
        this.resumo = resumo;
        this.publicada = publicada;
        this.dataPublicacao = dataPublicacao;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
    }
    
    // Getters e Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitulo() {
        return titulo;
    }
    
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
    
    public String getConteudo() {
        return conteudo;
    }
    
    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }
    
    public String getResumo() {
        return resumo;
    }
    
    public void setResumo(String resumo) {
        this.resumo = resumo;
    }
    
    public Boolean getPublicada() {
        return publicada;
    }
    
    public void setPublicada(Boolean publicada) {
        this.publicada = publicada;
    }
    
    public LocalDateTime getDataPublicacao() {
        return dataPublicacao;
    }
    
    public void setDataPublicacao(LocalDateTime dataPublicacao) {
        this.dataPublicacao = dataPublicacao;
    }
    
    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
    
    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
    
    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }
    
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }
    
    public UsuarioDTO getAutor() {
        return autor;
    }
    
    public void setAutor(UsuarioDTO autor) {
        this.autor = autor;
    }
    
    public List<CategoriaDTO> getCategorias() {
        return categorias;
    }
    
    public void setCategorias(List<CategoriaDTO> categorias) {
        this.categorias = categorias;
    }
    
    public Integer getTotalComentarios() {
        return totalComentarios;
    }
    
    public void setTotalComentarios(Integer totalComentarios) {
        this.totalComentarios = totalComentarios;
    }
}