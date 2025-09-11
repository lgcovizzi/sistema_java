package com.sistema.java.model.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

/**
 * DTO para requisição de registro de usuário
 * Referência: Login e Registro - project_rules.md
 * Referência: Padrões para Entidades JPA - project_rules.md
 */
public class RegistroRequestDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "Nome deve conter apenas letras e espaços")
    private String nome;

    @NotBlank(message = "Sobrenome é obrigatório")
    @Size(min = 2, max = 100, message = "Sobrenome deve ter entre 2 e 100 caracteres")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "Sobrenome deve conter apenas letras e espaços")
    private String sobrenome;

    @NotBlank(message = "CPF é obrigatório")
    @CPF(message = "CPF deve ser válido")
    private String cpf;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter formato válido")
    @Size(max = 150, message = "Email deve ter no máximo 150 caracteres")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", 
             message = "Senha deve conter ao menos: 1 letra minúscula, 1 maiúscula, 1 número e 1 caractere especial")
    private String senha;

    @NotBlank(message = "Confirmação de senha é obrigatória")
    private String confirmacaoSenha;

    @Pattern(regexp = "^[0-9\\-()+ ]{8,20}$", message = "Telefone deve ter formato válido")
    private String telefone;

    @Past(message = "Data de nascimento deve ser no passado")
    private LocalDate dataNascimento;

    @AssertTrue(message = "É necessário aceitar os termos de uso")
    private boolean aceitarTermos;

    private boolean receberNewsletter = false;

    // Construtores
    public RegistroRequestDTO() {}

    public RegistroRequestDTO(String nome, String sobrenome, String cpf, String email, String senha) {
        this.nome = nome;
        this.sobrenome = sobrenome;
        this.cpf = cpf;
        this.email = email;
        this.senha = senha;
        this.confirmacaoSenha = senha;
        this.aceitarTermos = true;
    }

    // Getters e Setters
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome != null ? nome.trim() : null;
    }

    public String getSobrenome() {
        return sobrenome;
    }

    public void setSobrenome(String sobrenome) {
        this.sobrenome = sobrenome != null ? sobrenome.trim() : null;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf != null ? cpf.replaceAll("[^0-9]", "") : null;
    }

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

    public String getConfirmacaoSenha() {
        return confirmacaoSenha;
    }

    public void setConfirmacaoSenha(String confirmacaoSenha) {
        this.confirmacaoSenha = confirmacaoSenha;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone != null ? telefone.trim() : null;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public boolean isAceitarTermos() {
        return aceitarTermos;
    }

    public void setAceitarTermos(boolean aceitarTermos) {
        this.aceitarTermos = aceitarTermos;
    }

    public boolean isReceberNewsletter() {
        return receberNewsletter;
    }

    public void setReceberNewsletter(boolean receberNewsletter) {
        this.receberNewsletter = receberNewsletter;
    }

    /**
     * Valida se as senhas coincidem
     * Referência: Login e Registro - project_rules.md
     * 
     * @return true se as senhas são iguais
     */
    @AssertTrue(message = "Senha e confirmação devem ser iguais")
    public boolean isSenhasIguais() {
        if (senha == null || confirmacaoSenha == null) {
            return false;
        }
        return senha.equals(confirmacaoSenha);
    }

    /**
     * Valida se a idade é adequada (mínimo 16 anos)
     * Referência: Regras de Validação de Perfil - project_rules.md
     * 
     * @return true se a idade é válida
     */
    @AssertTrue(message = "Idade mínima de 16 anos")
    public boolean isIdadeValida() {
        if (dataNascimento == null) {
            return true; // Campo opcional
        }
        return dataNascimento.isBefore(LocalDate.now().minusYears(16));
    }

    /**
     * Valida se todos os campos obrigatórios estão preenchidos
     * Referência: Login e Registro - project_rules.md
     * 
     * @return true se os dados são válidos
     */
    public boolean isValido() {
        return nome != null && !nome.trim().isEmpty() &&
               sobrenome != null && !sobrenome.trim().isEmpty() &&
               cpf != null && cpf.length() == 11 &&
               email != null && email.contains("@") &&
               senha != null && senha.length() >= 8 &&
               isSenhasIguais() &&
               aceitarTermos &&
               isIdadeValida();
    }

    /**
     * Limpa dados sensíveis do DTO
     * Referência: Segurança - project_rules.md
     */
    public void limparDadosSensiveis() {
        this.senha = null;
        this.confirmacaoSenha = null;
    }

    /**
     * Obtém o nome completo
     * Referência: Padrões para Entidades JPA - project_rules.md
     * 
     * @return Nome completo
     */
    public String getNomeCompleto() {
        if (nome == null || sobrenome == null) {
            return "";
        }
        return nome.trim() + " " + sobrenome.trim();
    }

    /**
     * Formata o CPF para exibição
     * Referência: Padrões de Código - project_rules.md
     * 
     * @return CPF formatado (XXX.XXX.XXX-XX)
     */
    public String getCpfFormatado() {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + 
               cpf.substring(3, 6) + "." + 
               cpf.substring(6, 9) + "-" + 
               cpf.substring(9, 11);
    }

    @Override
    public String toString() {
        return "RegistroRequestDTO{" +
                "nome='" + nome + '\'' +
                ", sobrenome='" + sobrenome + '\'' +
                ", cpf='" + getCpfFormatado() + '\'' +
                ", email='" + email + '\'' +
                ", telefone='" + telefone + '\'' +
                ", dataNascimento=" + dataNascimento +
                ", aceitarTermos=" + aceitarTermos +
                ", receberNewsletter=" + receberNewsletter +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegistroRequestDTO that = (RegistroRequestDTO) o;

        if (email != null ? !email.equals(that.email) : that.email != null) return false;
        return cpf != null ? cpf.equals(that.cpf) : that.cpf == null;
    }

    @Override
    public int hashCode() {
        int result = email != null ? email.hashCode() : 0;
        result = 31 * result + (cpf != null ? cpf.hashCode() : 0);
        return result;
    }

    /**
     * Cria uma instância para testes
     * Referência: Testes e Qualidade de Código - project_rules.md
     * 
     * @param nome Nome do usuário
     * @param sobrenome Sobrenome do usuário
     * @param cpf CPF do usuário
     * @param email Email do usuário
     * @param senha Senha do usuário
     * @return Instância de RegistroRequestDTO
     */
    public static RegistroRequestDTO criarParaTeste(String nome, String sobrenome, String cpf, String email, String senha) {
        RegistroRequestDTO dto = new RegistroRequestDTO(nome, sobrenome, cpf, email, senha);
        dto.setDataNascimento(LocalDate.now().minusYears(25));
        return dto;
    }

    /**
     * Cria uma instância com dados mínimos válidos
     * Referência: Login e Registro - project_rules.md
     * 
     * @return Instância válida para testes
     */
    public static RegistroRequestDTO criarValido() {
        return criarParaTeste(
            "João",
            "Silva",
            "12345678901",
            "joao.silva@email.com",
            "MinhaSenh@123"
        );
    }
}