package com.sistema.entity;

/**
 * Enum para definir os papéis (roles) dos usuários no sistema.
 */
public enum Role {
    /**
     * Usuário comum com permissões básicas
     */
    USER,
    
    /**
     * Administrador com permissões elevadas
     */
    ADMIN,
    
    /**
     * Moderador com permissões intermediárias
     */
    MODERATOR;
    
    /**
     * Retorna o nome do role com prefixo ROLE_
     * @return String com o nome formatado para Spring Security
     */
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
    
    /**
     * Verifica se o role tem permissões administrativas
     * @return true se for ADMIN ou MODERATOR
     */
    public boolean isPrivileged() {
        return this == ADMIN || this == MODERATOR;
    }
    
    /**
     * Verifica se o role é de administrador
     * @return true se for ADMIN
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }
}