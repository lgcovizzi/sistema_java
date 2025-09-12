package com.sistema.java.model.enums;

/**
 * Enum que define os status possíveis para comentários no sistema.
 * Referência: Sistema de Comentários - project_rules.md
 */
public enum StatusComentario {
    
    /**
     * Comentário aguardando aprovação
     */
    PENDENTE("Pendente", "Aguardando aprovação"),
    
    /**
     * Comentário aprovado e visível
     */
    APROVADO("Aprovado", "Comentário aprovado e visível"),
    
    /**
     * Comentário rejeitado por moderação
     */
    REJEITADO("Rejeitado", "Comentário rejeitado por moderação"),
    
    /**
     * Comentário removido por violação de regras
     */
    REMOVIDO("Removido", "Comentário removido por violação");
    
    private final String nome;
    private final String descricao;
    
    StatusComentario(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }
    
    public String getNome() {
        return nome;
    }
    
    public String getDescricao() {
        return descricao;
    }
    
    /**
     * Verifica se o status permite visualização pública
     */
    public boolean isVisivel() {
        return this == APROVADO;
    }
    
    /**
     * Verifica se o status permite edição
     */
    public boolean isEditavel() {
        return this == PENDENTE || this == REJEITADO;
    }
    
    /**
     * Retorna o status padrão para novos comentários
     */
    public static StatusComentario getPadrao() {
        return PENDENTE;
    }
}