package com.sistema.java.model.entity;

import com.sistema.java.model.enums.PapelUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Entidade Usuario
 * Referência: Padrões para Entidades JPA - project_rules.md
 * Referência: Controle de Acesso - project_rules.md
 * Referência: Login e Registro - project_rules.md
 */
@Entity
@Table(name = "usuarios", indexes = {
    @Index(name = "idx_usuario_email", columnList = "email", unique = true),
    @Index(name = "idx_usuario_cpf", columnList = "cpf", unique = true),
    @Index(name = "idx_usuario_papel", columnList = "papel")
})
public class Usuario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;
    
    @NotBlank(message = "Sobrenome é obrigatório")
    @Size(max = 100, message = "Sobrenome deve ter no máximo 100 caracteres")
    @Column(name = "sobrenome", nullable = false, length = 100)
    private String sobrenome;
    
    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "^\\d{11}$", message = "CPF deve ter 11 dígitos")
    @Column(name = "cpf", nullable = false, unique = true, length = 11)
    private String cpf;
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    @Size(max = 150, message = "Email deve ter no máximo 150 caracteres")
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;
    
    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter mínimo de 8 caracteres")
    @Column(name = "senha", nullable = false)
    private String senha;
    
    @Pattern(regexp = "^[0-9\\-()+ ]{8,20}$", message = "Telefone deve ter formato válido")
    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    @Column(name = "telefone", length = 20)
    private String telefone;
    
    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;
    
    @Size(max = 500, message = "Avatar deve ter no máximo 500 caracteres")
    @Column(name = "avatar", length = 500)
    private String avatar;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false, length = 20)
    private PapelUsuario papel = PapelUsuario.USUARIO;
    
    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;
    
    @Column(name = "email_verificado", nullable = false)
    private Boolean emailVerificado = false;
    
    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;
    
    @UpdateTimestamp
    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;
    
    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;
    
    @Size(max = 255, message = "Token de verificação deve ter no máximo 255 caracteres")
    @Column(name = "token_verificacao", length = 255)
    private String tokenVerificacao;
    
    @Size(max = 255, message = "Token de reset de senha deve ter no máximo 255 caracteres")
    @Column(name = "token_reset_senha", length = 255)
    private String tokenResetSenha;
    
    @Column(name = "data_expiracao_token")
    private LocalDateTime dataExpiracaoToken;
    
    // Relacionamentos
    @OneToMany(mappedBy = "autor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Noticia> noticias;
    
    @OneToMany(mappedBy = "autor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comentario> comentarios;
    
    // Construtores
    /**
     * Construtor padrão
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Inicializa usuário com configurações padrão de tema
     */
    public Usuario() {}
    
    /**
     * Construtor com parâmetros obrigatórios
     * Referência: Login e Registro - project_rules.md
     * Cria usuário com papel padrão USUARIO
     */
    public Usuario(String nome, String sobrenome, String cpf, String email, String senha) {
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.cpf = cpf;
        this.email = email;
        this.senha = senha;
        this.papel = PapelUsuario.USUARIO;
        this.ativo = true;
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
    
    public String getSobrenome() {
        return sobrenome;
    }
    
    public void setSobrenome(String sobrenome) {
        this.sobrenome = sobrenome;
    }
    
    public String getCpf() {
        return cpf;
    }
    
    public void setCpf(String cpf) {
        this.cpf = cpf;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getSenha() {
        return senha;
    }
    
    public void setSenha(String senha) {
        this.senha = senha;
    }
    
    public String getTelefone() {
        return telefone;
    }
    
    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }
    
    public LocalDate getDataNascimento() {
        return dataNascimento;
    }
    
    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }
    
    public String getAvatar() {
        return avatar;
    }
    
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
    
    public PapelUsuario getPapel() {
        return papel;
    }
    
    public void setPapel(PapelUsuario papel) {
        this.papel = papel;
    }
    
    public Boolean getAtivo() {
        return ativo;
    }
    
    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
    
    public Boolean getEmailVerificado() {
        return emailVerificado;
    }
    
    public void setEmailVerificado(Boolean emailVerificado) {
        this.emailVerificado = emailVerificado;
    }
    
    /**
     * Método de conveniência para verificar se o usuário está ativo
     * Referência: Controle de Acesso - project_rules.md
     */
    public boolean isAtivo() {
        return ativo != null && ativo;
    }
    
    /**
     * Método de conveniência para verificar se o email foi verificado
     * Referência: Login e Registro - project_rules.md
     */
    public boolean isEmailVerificado() {
        return emailVerificado != null && emailVerificado;
    }
    
    /**
     * Método para verificar se o usuário pode gerenciar outros usuários
     * Referência: Controle de Acesso - project_rules.md
     */
    public boolean canManageUsers() {
        // TODO: Implementar verificação de permissão para gerenciar usuários
        return papel == PapelUsuario.ADMINISTRADOR || papel == PapelUsuario.FUNDADOR;
    }
    
    /**
     * Método para verificar se o usuário pode gerenciar conteúdo
     * Referência: Controle de Acesso - project_rules.md
     */
    public boolean canManageContent() {
        // TODO: Implementar verificação de permissão para gerenciar conteúdo
        return papel == PapelUsuario.ADMINISTRADOR || papel == PapelUsuario.FUNDADOR || papel == PapelUsuario.COLABORADOR;
    }
    
    /**
     * Método para verificar se o usuário pode acessar o dashboard
     * Referência: Controle de Acesso - project_rules.md
     */
    public boolean canAccessDashboard() {
        // TODO: Implementar verificação de acesso ao dashboard
        return papel != PapelUsuario.CONVIDADO;
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
    
    public LocalDateTime getUltimoLogin() {
        return ultimoLogin;
    }
    
    public void setUltimoLogin(LocalDateTime ultimoLogin) {
        this.ultimoLogin = ultimoLogin;
    }
    
    public List<Noticia> getNoticias() {
        return noticias;
    }
    
    public void setNoticias(List<Noticia> noticias) {
        this.noticias = noticias;
    }
    
    public List<Comentario> getComentarios() {
        return comentarios;
    }
    
    public void setComentarios(List<Comentario> comentarios) {
        this.comentarios = comentarios;
    }

    public String getTokenVerificacao() {
        return tokenVerificacao;
    }

    public void setTokenVerificacao(String tokenVerificacao) {
        this.tokenVerificacao = tokenVerificacao;
    }

    public String getTokenResetSenha() {
        return tokenResetSenha;
    }

    public void setTokenResetSenha(String tokenResetSenha) {
        this.tokenResetSenha = tokenResetSenha;
    }

    public LocalDateTime getDataExpiracaoToken() {
        return dataExpiracaoToken;
    }

    public void setDataExpiracaoToken(LocalDateTime dataExpiracaoToken) {
        this.dataExpiracaoToken = dataExpiracaoToken;
    }

    // Métodos auxiliares equals e hashCode baseados no ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return Objects.equals(id, usuario.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", sobrenome='" + sobrenome + '\'' +
                ", cpf='" + cpf + '\'' +
                ", email='" + email + '\'' +
                ", telefone='" + telefone + '\'' +
                ", dataNascimento=" + dataNascimento +
                ", papel=" + papel +
                ", ativo=" + ativo +
                ", dataCriacao=" + dataCriacao +
                ", dataAtualizacao=" + dataAtualizacao +
                '}';
    }
}