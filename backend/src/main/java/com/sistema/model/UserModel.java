package com.sistema.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_models")
public class UserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome não deve estar em branco")
    @Size(max = 50, message = "Nome deve ter no máximo 50 caracteres")
    @Column(nullable = false, length = 50)
    private String nome;

    @NotBlank(message = "Sobrenome não deve estar em branco")
    @Size(max = 50, message = "Sobrenome deve ter no máximo 50 caracteres")
    @Column(nullable = false, length = 50)
    private String sobrenome;

    @NotBlank(message = "Email não deve estar em branco")
    @Email(message = "Email deve ser um endereço de e-mail bem formado")
    @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Senha não deve estar em branco")
    @Size(min = 6, max = 100, message = "Senha deve ter tamanho deve ser entre 6 e 100")
    @Column(nullable = false, length = 100)
    private String senha;

    @NotNull(message = "Role não deve ser nulo")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado = false;

    @Column(name = "data_verificacao_email")
    private LocalDateTime dataVerificacaoEmail;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    // Construtores
    public UserModel() {
    }

    public UserModel(String nome, String sobrenome, String email, String senha, UserRole role) {
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.email = email;
        this.senha = senha;
        this.role = role;
    }

    // Métodos de callback JPA
    @PrePersist
    protected void prePersist() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void preUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }

    // Métodos de negócio
    public void verificarEmail() {
        this.emailVerificado = true;
        this.dataVerificacaoEmail = LocalDateTime.now();
    }

    public String getNomeCompleto() {
        return nome + " " + sobrenome;
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

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isEmailVerificado() {
        return emailVerificado;
    }

    public void setEmailVerificado(boolean emailVerificado) {
        this.emailVerificado = emailVerificado;
    }

    public LocalDateTime getDataVerificacaoEmail() {
        return dataVerificacaoEmail;
    }

    public void setDataVerificacaoEmail(LocalDateTime dataVerificacaoEmail) {
        this.dataVerificacaoEmail = dataVerificacaoEmail;
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

    // equals e hashCode baseados no email (identificador único de negócio)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserModel user = (UserModel) o;
        return Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", sobrenome='" + sobrenome + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", emailVerificado=" + emailVerificado +
                ", dataCriacao=" + dataCriacao +
                '}';
    }
}