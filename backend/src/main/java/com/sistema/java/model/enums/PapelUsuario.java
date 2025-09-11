package com.sistema.java.model.enums;

/**
 * Enum que define os papéis/roles dos usuários no sistema.
 * Hierarquia de permissões: ADMINISTRADOR > FUNDADOR > COLABORADOR > ASSOCIADO > USUARIO > CONVIDADO
 */
public enum PapelUsuario {
    
    /**
     * Nível mais alto - acesso total ao sistema
     */
    ADMINISTRADOR("Administrador", 6),
    
    /**
     * Fundador da organização - acesso quase total
     */
    FUNDADOR("Fundador", 5),
    
    /**
     * Colaborador ativo - acesso a funcionalidades de gestão
     */
    COLABORADOR("Colaborador", 4),
    
    /**
     * Associado da organização - acesso a funcionalidades específicas
     */
    ASSOCIADO("Associado", 3),
    
    /**
     * Usuário padrão - acesso básico ao sistema
     */
    USUARIO("Usuário", 2),
    
    /**
     * Convidado - acesso apenas a registro/login
     */
    CONVIDADO("Convidado", 1);
    
    private final String descricao;
    private final int nivel;
    
    PapelUsuario(String descricao, int nivel) {
        this.descricao = descricao;
        this.nivel = nivel;
    }
    
    public String getDescricao() {
        return descricao;
    }
    
    public int getNivel() {
        return nivel;
    }
    
    /**
     * Verifica se este papel tem nível maior ou igual ao papel fornecido
     */
    public boolean temNivelMaiorOuIgual(PapelUsuario outroPapel) {
        return this.nivel >= outroPapel.nivel;
    }
    
    /**
     * Verifica se este papel pode acessar dashboard
     */
    public boolean podeAcessarDashboard() {
        return this != CONVIDADO;
    }
    
    /**
     * Verifica se este papel pode administrar outros usuários
     */
    public boolean podeAdministrarUsuarios() {
        return this == ADMINISTRADOR;
    }
    
    /**
     * Verifica se este papel pode gerenciar conteúdo
     */
    public boolean podeGerenciarConteudo() {
        return this.nivel >= COLABORADOR.nivel;
    }
    
    /**
     * Verifica se este papel pode moderar comentários
     */
    public boolean podeModerarComentarios() {
        return this.nivel >= ASSOCIADO.nivel;
    }
    
    /**
     * Retorna o papel padrão para novos usuários
     */
    public static PapelUsuario getPadraoNovoUsuario() {
        return USUARIO;
    }
}