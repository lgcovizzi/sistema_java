package com.sistema.java.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Entidade Categoria
 * Referência: Sistema de Temas Claros e Escuros - project_rules.md
 * Esta entidade deve considerar cores e estilos adequados para ambos os temas
 */
@Entity
@Table(name = "categorias", indexes = {
    @Index(name = "idx_categoria_nome", columnList = "nome", unique = true)
})
public class Categoria {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    @Column(name = "nome", nullable = false, unique = true, length = 100)
    private String nome;
    
    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;
    
    @Column(name = "ativa", nullable = false)
    private Boolean ativa = true;
    
    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;
    
    // Relacionamentos
    @ManyToMany(mappedBy = "categorias", fetch = FetchType.LAZY)
    private List<Noticia> noticias;
    
    // Construtores
    /**
     * Construtor padrão
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Inicializa categoria com configurações padrão de exibição
     */
    public Categoria() {}
    
    /**
     * Construtor com parâmetros
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Cria categoria com cores compatíveis com sistema de temas
     */
    public Categoria(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
        this.ativa = true;
    }
    
    // Getters e Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNome() {
        return nome;
    }
    
    public void setNome(String nome) {
        this.nome = nome;
    }
    
    public String getDescricao() {
        return descricao;
    }
    
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
    
    public Boolean getAtiva() {
        return ativa;
    }
    
    public void setAtiva(Boolean ativa) {
        this.ativa = ativa;
    }
    
    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
    
    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
    
    public List<Noticia> getNoticias() {
        return noticias;
    }
    
    public void setNoticias(List<Noticia> noticias) {
        this.noticias = noticias;
    }
    
    // equals e hashCode baseados no ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Categoria categoria = (Categoria) o;
        return Objects.equals(id, categoria.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Categoria{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", descricao='" + descricao + '\'' +
                ", ativa=" + ativa +
                ", dataCriacao=" + dataCriacao +
                '}';
    }
}