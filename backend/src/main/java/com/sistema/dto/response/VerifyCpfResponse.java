package com.sistema.dto.response;

/**
 * DTO para resposta de verificação de CPF no fluxo de recuperação de senha.
 * 
 * Este DTO é retornado quando um CPF é verificado com sucesso,
 * contendo o email mascarado para confirmação pelo usuário.
 */
public class VerifyCpfResponse {

    private boolean success;
    private String maskedEmail;
    private String message;

    public VerifyCpfResponse() {}

    public VerifyCpfResponse(boolean success, String maskedEmail, String message) {
        this.success = success;
        this.maskedEmail = maskedEmail;
        this.message = message;
    }

    /**
     * Cria uma resposta de sucesso com email mascarado
     */
    public static VerifyCpfResponse success(String maskedEmail) {
        return new VerifyCpfResponse(true, maskedEmail, "CPF encontrado. Confirme o email para prosseguir.");
    }

    /**
     * Cria uma resposta de erro
     */
    public static VerifyCpfResponse error(String message) {
        return new VerifyCpfResponse(false, null, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMaskedEmail() {
        return maskedEmail;
    }

    public void setMaskedEmail(String maskedEmail) {
        this.maskedEmail = maskedEmail;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "VerifyCpfResponse{" +
                "success=" + success +
                ", maskedEmail='" + maskedEmail + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}