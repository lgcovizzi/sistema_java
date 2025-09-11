package com.sistema.java.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Entidade Noticia
 * Referência: Sistema de Temas Claros e Escuros - project_rules.md
 * Esta entidade deve considerar formatação de conteúdo para diferentes temas
 */
@Entity
@Table(name = "noticias", indexes = {
    @Index(name = "idx_noticia_publicada", columnList = "publicada"),
    @Index(name = "idx_noticia_data_publicacao", columnList = "data_publicacao DESC"),
    @Index(name = "idx_noticia_autor", columnList = "autor_id")
})
public class Noticia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Título é obrigatório")
    @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;
    
    @NotBlank(message = "Conteúdo é obrigatório")
    @Column(name = "conteudo", nullable = false, columnDefinition = "TEXT")
    private String conteudo;
    
    @Size(max = 500, message = "Resumo deve ter no máximo 500 caracteres")
    @Column(name = "resumo", length = 500)
    private String resumo;
    
    @Column(name = "publicada", nullable = false)
    private Boolean publicada = false;
    
    @Column(name = "data_publicacao")
    private LocalDateTime dataPublicacao;
    
    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;
    
    @UpdateTimestamp
    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;
    
    // Relacionamentos
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;
    
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "noticia_categorias",
        joinColumns = @JoinColumn(name = "noticia_id"),
        inverseJoinColumns = @JoinColumn(name = "categoria_id")
    )
    private List<Categoria> categorias;
    
    @OneToMany(mappedBy = "noticia", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comentario> comentarios;
    
    // Construtores
    /**
     * Construtor padrão
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Inicializa notícia com configurações padrão de exibição
     */
    public Noticia() {}
    
    public Noticia(String titulo, String conteudo, String resumo, Usuario autor) {
        this.titulo = titulo;
        this.conteudo = conteudo;
        this.resumo = resumo;
        this.autor = autor;
        this.publicada = false;
    }
    
    // Métodos de negócio
    /**
     * Publica a notícia
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Notícia publicada deve ser exibida adequadamente em ambos os temas
     */
    public void publicar() {
        this.publicada = true;
        this.dataPublicacao = LocalDateTime.now();
    }
    
    public void despublicar() {
        this.publicada = false;
        this.dataPublicacao = null;
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
    
    public Usuario getAutor() {
        return autor;
    }
    
    public void setAutor(Usuario autor) {
        this.autor = autor;
    }
    
    public List<Categoria> getCategorias() {
        return categorias;
    }
    
    public void setCategorias(List<Categoria> categorias) {
        this.categorias = categorias;
    }
    
    public List<Comentario> getComentarios() {
        return comentarios;
    }
    
    public void setComentarios(List<Comentario> comentarios) {
        this.comentarios = comentarios;
    }
    
    // equals e hashCode baseados no ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Noticia noticia = (Noticia) o;
        return Objects.equals(id, noticia.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Noticia{" +
                "id=" + id +
                ", titulo='" + titulo + '\'' +
                ", publicada=" + publicada +
                ", dataPublicacao=" + dataPublicacao +
                ", dataCriacao=" + dataCriacao +
                ", dataAtualizacao=" + dataAtualizacao +
                '}';
    }
}