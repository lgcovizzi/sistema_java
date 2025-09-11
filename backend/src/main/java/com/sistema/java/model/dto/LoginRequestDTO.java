package com.sistema.java.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para requisição de login
 * Referência: Login e Registro - project_rules.md
 * Referência: Padrões para Entidades JPA - project_rules.md
 */
public class LoginRequestDTO {

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    @Size(max = 150, message = "Email deve ter no máximo 150 caracteres")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    private String senha;

    private boolean lembrarMe = false;

    // Construtores
    public LoginRequestDTO() {}

    public LoginRequestDTO(String email, String senha) {
        this.email = email;
        this.senha = senha;
    }

    public LoginRequestDTO(String email, String senha, boolean lembrarMe) {
        this.email = email;
        this.senha = senha;
        this.lembrarMe = lembrarMe;
    }

    // Getters e Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : null;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public boolean isLembrarMe() {
        return lembrarMe;
    }

    public void setLembrarMe(boolean lembrarMe) {
        this.lembrarMe = lembrarMe;
    }

    /**
     * Valida se os dados de login estão completos
     * Referência: Login e Registro - project_rules.md
     * 
     * @return true se os dados são válidos
     */
    public boolean isValido() {
        return email != null && !email.trim().isEmpty() &&
               senha != null && !senha.trim().isEmpty() &&
               email.contains("@") && senha.length() >= 8;
    }

    /**
     * Limpa dados sensíveis do DTO
     * Referência: Segurança - project_rules.md
     */
    public void limparDadosSensiveis() {
        this.senha = null;
    }

    @Override
    public String toString() {
        return "LoginRequestDTO{" +
                "email='" + email + '\'' +
                ", lembrarMe=" + lembrarMe +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LoginRequestDTO that = (LoginRequestDTO) o;

        return email != null ? email.equals(that.email) : that.email == null;
    }

    @Override
    public int hashCode() {
        return email != null ? email.hashCode() : 0;
    }

    /**
     * Cria uma instância para testes
     * Referência: Testes e Qualidade de Código - project_rules.md
     * 
     * @param email Email do usuário
     * @param senha Senha do usuário
     * @return Instância de LoginRequestDTO
     */
    public static LoginRequestDTO criarParaTeste(String email, String senha) {
        return new LoginRequestDTO(email, senha, false);
    }

    /**
     * Cria uma instância com lembrar-me ativado
     * Referência: Login e Registro - project_rules.md
     * 
     * @param email Email do usuário
     * @param senha Senha do usuário
     * @return Instância de LoginRequestDTO com lembrar-me
     */
    public static LoginRequestDTO criarComLembrarMe(String email, String senha) {
        return new LoginRequestDTO(email, senha, true);
    }
}