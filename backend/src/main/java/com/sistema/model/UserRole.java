package com.sistema.model;

/**
 * Enum que define os diferentes tipos de usuários no sistema.
 * Cada role representa um nível de acesso e responsabilidade específico.
 */
public enum UserRole {
    /**
     * Usuário básico do sistema
     */
    USUARIO,
    
    /**
     * Usuário associado com privilégios adicionais
     */
    ASSOCIADO,
    
    /**
     * Colaborador da organização
     */
    COLABORADOR,
    
    /**
     * Diretor com acesso a funcionalidades de gestão
     */
    DIRETOR,
    
    /**
     * Fundador da organização com acesso completo
     */
    FUNDADOR,
    
    /**
     * Administrador do sistema com privilégios máximos
     */
    ADMINISTRADOR
}