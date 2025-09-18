package com.sistema.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para requisição de verificação de CPF no fluxo de recuperação de senha.
 * 
 * Este DTO é usado no primeiro passo do fluxo de recuperação de senha,
 * onde o usuário informa seu CPF para verificar se existe uma conta
 * associada e receber o email mascarado para confirmação.
 */
public class VerifyCpfRequest {

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}", 
             message = "CPF deve ter formato válido (11 dígitos ou xxx.xxx.xxx-xx)")
    private String cpf;

    public VerifyCpfRequest() {}

    public VerifyCpfRequest(String cpf) {
        this.cpf = cpf;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    @Override
    public String toString() {
        return "VerifyCpfRequest{" +
                "cpf='[PROTECTED]'" +
                '}';
    }
}