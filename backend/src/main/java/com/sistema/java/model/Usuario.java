package com.sistema.java.model;

import com.sistema.java.model.enums.PapelUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "usuarios", indexes = {
    @Index(name = "idx_usuario_cpf", columnList = "cpf"),
    @Index(name = "idx_usuario_papel", columnList = "papel"),
    @Index(name = "idx_usuario_email", columnList = "email"),
    @Index(name = "idx_usuario_ativo", columnList = "ativo")
})
public class Usuario implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nome;
    
    @Size(min = 2, max = 100, message = "Sobrenome deve ter entre 2 e 100 caracteres")
    @Column(length = 100)
    private String sobrenome;
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", 
             message = "Email deve ter formato válido")
    @Column(nullable = false, unique = true, length = 150)
    private String email;
    
    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter pelo menos 8 caracteres")
    @Column(nullable = false)
    private String senha;
    
    @Pattern(regexp = "^\\d{11}$", message = "CPF deve ter exatamente 11 dígitos")
    @Column(unique = true, length = 11)
    private String cpf;
    
    @Pattern(regexp = "^\\(?\\d{2}\\)?[\\s-]?\\d{4,5}[\\s-]?\\d{4}$", 
             message = "Telefone deve ter formato válido")
    @Column(length = 20)
    private String telefone;
    
    @Past(message = "Data de nascimento deve ser no passado")
    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;
    
    @Column(length = 500)
    private String avatar;
    
    @NotNull(message = "Papel do usuário é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PapelUsuario papel = PapelUsuario.USUARIO;
    
    @Column(nullable = false)
    private Boolean ativo = true;
    
    @Column(name = "email_verificado", nullable = false)
    private Boolean emailVerificado = false;
    
    @Column(name = "token_verificacao")
    private String tokenVerificacao;
    
    @Column(name = "token_reset_senha")
    private String tokenResetSenha;
    
    @Column(name = "data_expiracao_token")
    private LocalDateTime dataExpiracaoToken;
    
    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;
    
    @UpdateTimestamp
    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;
    
    @Column(name = "ultimo_acesso")
    private LocalDateTime ultimoAcesso;
    
    // Relacionamentos
    @OneToMany(mappedBy = "autor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Noticia> noticias;
    
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comentario> comentarios;
    
    // Construtores
    public Usuario() {}
    
    public Usuario(String nome, String email, String senha) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.papel = PapelUsuario.USUARIO;
        this.ativo = true;
        this.emailVerificado = false;
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
    
    public String getCpf() {
        return cpf;
    }
    
    public void setCpf(String cpf) {
        this.cpf = cpf;
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
    
    public LocalDateTime getUltimoAcesso() {
        return ultimoAcesso;
    }
    
    public void setUltimoAcesso(LocalDateTime ultimoAcesso) {
        this.ultimoAcesso = ultimoAcesso;
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
    
    // Métodos utilitários
    public String getNomeCompleto() {
        if (sobrenome != null && !sobrenome.trim().isEmpty()) {
            return nome + " " + sobrenome;
        }
        return nome;
    }
    
    public boolean isAdmin() {
        return papel == PapelUsuario.ADMINISTRADOR || papel == PapelUsuario.FUNDADOR;
    }
    
    public boolean canAccessDashboard() {
        return papel.canAccessDashboard();
    }
    
    public boolean canManageUsers() {
        return papel.canManageUsers();
    }
    
    public boolean canManageContent() {
        return papel.canManageContent();
    }
    
    public boolean isTokenValid() {
        return dataExpiracaoToken != null && dataExpiracaoToken.isAfter(LocalDateTime.now());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario)) return false;
        Usuario usuario = (Usuario) o;
        return id != null && id.equals(usuario.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", email='" + email + '\'' +
                ", papel=" + papel +
                ", ativo=" + ativo +
                '}';
    }
}