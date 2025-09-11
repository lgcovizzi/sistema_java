package com.sistema.java.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para resposta de logout
 * Referência: Segurança - project_rules.md
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogoutResponseDTO {
    
    @NotNull
    private boolean sucesso;
    
    private String mensagem;
    
    private String erro;
    
    // Construtores
    public LogoutResponseDTO() {}
    
    public LogoutResponseDTO(boolean sucesso, String mensagem, String erro) {
        this.sucesso = sucesso;
        this.mensagem = mensagem;
        this.erro = erro;
    }
    
    // Métodos estáticos para criação
    public static LogoutResponseDTO sucesso() {
        return new LogoutResponseDTO(true, "Logout realizado com sucesso", null);
    }
    
    public static LogoutResponseDTO erro(String erro, String mensagem) {
        return new LogoutResponseDTO(false, mensagem, erro);
    }
    
    // Getters e Setters
    public boolean isSucesso() {
        return sucesso;
    }
    
    public void setSucesso(boolean sucesso) {
        this.sucesso = sucesso;
    }
    
    public String getMensagem() {
        return mensagem;
    }
    
    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
    
    public String getErro() {
        return erro;
    }
    
    public void setErro(String erro) {
        this.erro = erro;
    }
    
    @Override
    public String toString() {
        return "LogoutResponseDTO{" +
                "sucesso=" + sucesso +
                ", mensagem='" + mensagem + '\'' +
                ", erro='" + erro + '\'' +
                '}';
    }
}