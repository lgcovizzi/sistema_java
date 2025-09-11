package com.sistema.java.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "comentarios", indexes = {
    @Index(name = "idx_comentario_noticia", columnList = "noticia_id"),
    @Index(name = "idx_comentario_aprovado", columnList = "aprovado")
})
public class Comentario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Conteúdo é obrigatório")
    @Column(name = "conteudo", nullable = false, columnDefinition = "TEXT")
    private String conteudo;
    
    @Column(name = "aprovado", nullable = false)
    private Boolean aprovado = false;
    
    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;
    
    // Relacionamentos
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "noticia_id", nullable = false)
    private Noticia noticia;
    
    // Construtores
    // Referência: Sistema de Temas Claros e Escuros - project_rules.md
    // Entidade deve suportar metadados de tema para renderização adequada
    public Comentario() {}
    
    public Comentario(String conteudo, Usuario autor, Noticia noticia) {
        this.conteudo = conteudo;
        this.autor = autor;
        this.noticia = noticia;
        this.aprovado = false;
    }
    
    // Métodos de negócio
    // Referência: Sistema de Temas Claros e Escuros - project_rules.md
    // Status de aprovação deve ser visualmente distinto em ambos os temas
    public void aprovar() {
        this.aprovado = true;
    }
    
    public void reprovar() {
        this.aprovado = false;
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
    
    public Usuario getAutor() {
        return autor;
    }
    
    public void setAutor(Usuario autor) {
        this.autor = autor;
    }
    
    public Noticia getNoticia() {
        return noticia;
    }
    
    public void setNoticia(Noticia noticia) {
        this.noticia = noticia;
    }
    
    // equals e hashCode baseados no ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comentario comentario = (Comentario) o;
        return Objects.equals(id, comentario.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Comentario{" +
                "id=" + id +
                ", aprovado=" + aprovado +
                ", dataCriacao=" + dataCriacao +
                '}';
    }
}