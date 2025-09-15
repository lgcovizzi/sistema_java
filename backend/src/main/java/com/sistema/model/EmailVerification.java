package com.sistema.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "email_verifications")
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Usuário não deve ser nulo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    @NotBlank(message = "Token não deve estar em branco")
    @Column(nullable = false, unique = true, length = 36)
    private String token;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_expiracao", nullable = false)
    private LocalDateTime dataExpiracao;

    @Column(name = "usado", nullable = false)
    private boolean usado = false;

    @Column(name = "data_uso")
    private LocalDateTime dataUso;

    // Construtores
    public EmailVerification() {
    }

    public EmailVerification(UserModel user, String token) {
        this.user = user;
        this.token = token;
    }

    // Métodos de callback JPA
    @PrePersist
    protected void prePersist() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
        if (dataExpiracao == null) {
            dataExpiracao = dataCriacao.plusHours(24);
        }
    }

    // Métodos de negócio
    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(dataExpiracao);
    }

    public boolean isValido() {
        return !isExpirado() && !usado;
    }

    public void marcarComoUsado() {
        this.usado = true;
        this.dataUso = LocalDateTime.now();
    }

    // Método estático para criar verificação com token único
    public static EmailVerification criarParaUsuario(UserModel user) {
        String token = UUID.randomUUID().toString();
        EmailVerification verification = new EmailVerification(user, token);
        verification.prePersist();
        return verification;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataExpiracao() {
        return dataExpiracao;
    }

    public void setDataExpiracao(LocalDateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }

    public boolean isUsado() {
        return usado;
    }

    public void setUsado(boolean usado) {
        this.usado = usado;
    }

    public LocalDateTime getDataUso() {
        return dataUso;
    }

    public void setDataUso(LocalDateTime dataUso) {
        this.dataUso = dataUso;
    }

    // equals e hashCode baseados no token (identificador único de negócio)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmailVerification that = (EmailVerification) o;
        return Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token);
    }

    @Override
    public String toString() {
        return "EmailVerification{" +
                "id=" + id +
                ", token='" + token + '\'' +
                ", dataCriacao=" + dataCriacao +
                ", dataExpiracao=" + dataExpiracao +
                ", usado=" + usado +
                ", dataUso=" + dataUso +
                '}';
    }
}