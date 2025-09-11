package com.sistema.java.model.dto;

import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO para resposta de registro de usuário
 * Referência: Login e Registro - project_rules.md
 * Referência: Controle de Acesso - project_rules.md
 */
public class RegistroResponseDTO {

    private boolean sucesso;
    private String mensagem;
    private String erro;
    private List<String> erros;
    private UsuarioRegistroDTO usuario;
    private String proximoPasso;
    private Map<String, Object> configuracoes;
    private LocalDateTime timestamp;
    private boolean requerVerificacaoEmail;
    private String emailVerificacao;

    // Construtores
    public RegistroResponseDTO() {
        this.timestamp = LocalDateTime.now();
        this.requerVerificacaoEmail = true;
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

    public List<String> getErros() {
        return erros;
    }

    public void setErros(List<String> erros) {
        this.erros = erros;
    }

    public UsuarioRegistroDTO getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioRegistroDTO usuario) {
        this.usuario = usuario;
    }

    public String getProximoPasso() {
        return proximoPasso;
    }

    public void setProximoPasso(String proximoPasso) {
        this.proximoPasso = proximoPasso;
    }

    public Map<String, Object> getConfiguracoes() {
        return configuracoes;
    }

    public void setConfiguracoes(Map<String, Object> configuracoes) {
        this.configuracoes = configuracoes;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRequerVerificacaoEmail() {
        return requerVerificacaoEmail;
    }

    public void setRequerVerificacaoEmail(boolean requerVerificacaoEmail) {
        this.requerVerificacaoEmail = requerVerificacaoEmail;
    }

    public String getEmailVerificacao() {
        return emailVerificacao;
    }

    public void setEmailVerificacao(String emailVerificacao) {
        this.emailVerificacao = emailVerificacao;
    }

    /**
     * DTO interno para informações do usuário no registro
     * Referência: Controle de Acesso - project_rules.md
     */
    public static class UsuarioRegistroDTO {
        private Long id;
        private String nome;
        private String sobrenome;
        private String email;
        private String cpf;
        private PapelUsuario papel;
        private boolean ativo;
        private LocalDateTime dataCriacao;
        private boolean emailVerificado;
        private String telefone;

        // Construtores
        public UsuarioRegistroDTO() {}

        public UsuarioRegistroDTO(Usuario usuario) {
            this.id = usuario.getId();
            this.nome = usuario.getNome();
            this.sobrenome = usuario.getSobrenome();
            this.email = usuario.getEmail();
            this.cpf = formatarCpf(usuario.getCpf());
            this.papel = usuario.getPapel();
            this.ativo = usuario.isAtivo();
            this.dataCriacao = usuario.getDataCriacao();
            this.emailVerificado = usuario.isEmailVerificado();
            this.telefone = usuario.getTelefone();
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

        public String getCpf() {
            return cpf;
        }

        public void setCpf(String cpf) {
            this.cpf = cpf;
        }

        public PapelUsuario getPapel() {
            return papel;
        }

        public void setPapel(PapelUsuario papel) {
            this.papel = papel;
        }

        public boolean isAtivo() {
            return ativo;
        }

        public void setAtivo(boolean ativo) {
            this.ativo = ativo;
        }

        public LocalDateTime getDataCriacao() {
            return dataCriacao;
        }

        public void setDataCriacao(LocalDateTime dataCriacao) {
            this.dataCriacao = dataCriacao;
        }

        public boolean isEmailVerificado() {
            return emailVerificado;
        }

        public void setEmailVerificado(boolean emailVerificado) {
            this.emailVerificado = emailVerificado;
        }

        public String getTelefone() {
            return telefone;
        }

        public void setTelefone(String telefone) {
            this.telefone = telefone;
        }

        /**
         * Obtém o nome completo do usuário
         * Referência: Padrões para Entidades JPA - project_rules.md
         * 
         * @return Nome completo
         */
        public String getNomeCompleto() {
            return nome + " " + sobrenome;
        }

        /**
         * Formata o CPF para exibição
         * Referência: Padrões de Código - project_rules.md
         * 
         * @param cpf CPF sem formatação
         * @return CPF formatado
         */
        private String formatarCpf(String cpf) {
            if (cpf == null || cpf.length() != 11) {
                return cpf;
            }
            return cpf.substring(0, 3) + "." + 
                   cpf.substring(3, 6) + "." + 
                   cpf.substring(6, 9) + "-" + 
                   cpf.substring(9, 11);
        }

        /**
         * Verifica se o usuário tem permissões básicas
         * Referência: Controle de Acesso - project_rules.md
         * 
         * @return true se tem acesso ao dashboard
         */
        public boolean temAcessoDashboard() {
            return papel != PapelUsuario.CONVIDADO && ativo;
        }
    }

    /**
     * Builder para facilitar criação de respostas
     * Referência: Padrões de Código - project_rules.md
     */
    public static class Builder {
        private final RegistroResponseDTO response;

        public Builder() {
            this.response = new RegistroResponseDTO();
        }

        public Builder sucesso(boolean sucesso) {
            response.setSucesso(sucesso);
            return this;
        }

        public Builder mensagem(String mensagem) {
            response.setMensagem(mensagem);
            return this;
        }

        public Builder erro(String erro) {
            response.setErro(erro);
            return this;
        }

        public Builder erros(List<String> erros) {
            response.setErros(erros);
            return this;
        }

        public Builder usuario(Usuario usuario) {
            response.setUsuario(new UsuarioRegistroDTO(usuario));
            return this;
        }

        public Builder proximoPasso(String proximoPasso) {
            response.setProximoPasso(proximoPasso);
            return this;
        }

        public Builder requerVerificacaoEmail(boolean requer) {
            response.setRequerVerificacaoEmail(requer);
            return this;
        }

        public Builder emailVerificacao(String email) {
            response.setEmailVerificacao(email);
            return this;
        }

        public Builder configuracoes(Map<String, Object> configuracoes) {
            response.setConfiguracoes(configuracoes);
            return this;
        }

        public RegistroResponseDTO build() {
            return response;
        }
    }

    /**
     * Cria um builder para construir a resposta
     * Referência: Padrões de Código - project_rules.md
     * 
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Cria resposta de sucesso
     * Referência: Login e Registro - project_rules.md
     * 
     * @param usuario Usuário registrado
     * @return Resposta de sucesso
     */
    public static RegistroResponseDTO sucesso(Usuario usuario) {
        return builder()
            .sucesso(true)
            .mensagem("Usuário registrado com sucesso")
            .usuario(usuario)
            .proximoPasso("Verifique seu email para ativar a conta")
            .emailVerificacao(usuario.getEmail())
            .build();
    }

    /**
     * Cria resposta de erro
     * Referência: Login e Registro - project_rules.md
     * 
     * @param mensagem Mensagem de erro
     * @param erro Detalhes do erro
     * @return Resposta de erro
     */
    public static RegistroResponseDTO erro(String mensagem, String erro) {
        return builder()
            .sucesso(false)
            .mensagem(mensagem)
            .erro(erro)
            .build();
    }

    /**
     * Cria resposta de erro com múltiplos erros
     * Referência: Login e Registro - project_rules.md
     * 
     * @param mensagem Mensagem principal
     * @param erros Lista de erros
     * @return Resposta de erro
     */
    public static RegistroResponseDTO erroMultiplo(String mensagem, List<String> erros) {
        return builder()
            .sucesso(false)
            .mensagem(mensagem)
            .erros(erros)
            .build();
    }

    /**
     * Cria resposta para email já existente
     * Referência: Login e Registro - project_rules.md
     * 
     * @param email Email que já existe
     * @return Resposta de erro específica
     */
    public static RegistroResponseDTO emailJaExiste(String email) {
        return builder()
            .sucesso(false)
            .mensagem("Email já está em uso")
            .erro("O email " + email + " já está registrado no sistema")
            .proximoPasso("Tente fazer login ou use outro email")
            .build();
    }

    /**
     * Cria resposta para CPF já existente
     * Referência: Login e Registro - project_rules.md
     * 
     * @param cpf CPF que já existe
     * @return Resposta de erro específica
     */
    public static RegistroResponseDTO cpfJaExiste(String cpf) {
        return builder()
            .sucesso(false)
            .mensagem("CPF já está em uso")
            .erro("O CPF informado já está registrado no sistema")
            .proximoPasso("Verifique os dados ou entre em contato com o suporte")
            .build();
    }
}