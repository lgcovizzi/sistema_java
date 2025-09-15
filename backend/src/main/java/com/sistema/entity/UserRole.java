package com.sistema.entity;

/**
 * Enum que define os papéis/roles dos usuários no sistema.
 * Implementa Spring Security GrantedAuthority para integração com autenticação.
 */
public enum UserRole {
    USER("Usuário comum", "ROLE_USER"),
    ADMIN("Administrador", "ROLE_ADMIN");

    private final String description;
    private final String authority;

    UserRole(String description, String authority) {
        this.description = description;
        this.authority = authority;
    }

    /**
     * Retorna a descrição legível do papel.
     * @return descrição do papel
     */
    public String getDescription() {
        return description;
    }

    /**
     * Retorna a authority para Spring Security.
     * @return authority no formato ROLE_*
     */
    public String getAuthority() {
        return authority;
    }

    /**
     * Verifica se este papel tem permissões de administrador.
     * @return true se for ADMIN, false caso contrário
     */
    public boolean hasAdminPermission() {
        return this == ADMIN;
    }

    /**
     * Converte uma string para UserRole (case-insensitive).
     * @param roleString string representando o papel
     * @return UserRole correspondente
     * @throws IllegalArgumentException se a string for inválida
     */
    public static UserRole fromString(String roleString) {
        if (roleString == null || roleString.trim().isEmpty()) {
            throw new IllegalArgumentException("Role não pode ser nula ou vazia");
        }
        
        String normalizedRole = roleString.trim().toUpperCase();
        
        try {
            return UserRole.valueOf(normalizedRole);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Role inválida: " + roleString);
        }
    }

    @Override
    public String toString() {
        return name() + " (" + description + ")";
    }
}