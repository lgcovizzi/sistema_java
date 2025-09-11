package com.sistema.java.model.dto;

import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO para resposta de login
 * Referência: Login e Registro - project_rules.md
 * Referência: Controle de Acesso - project_rules.md
 */
public class LoginResponseDTO {

    private boolean sucesso;
    private String mensagem;
    private String erro;
    private String token;
    private String refreshToken;
    private String tipoToken;
    private LocalDateTime expiracaoToken;
    private UsuarioLoginDTO usuario;
    private Map<String, Object> configuracoes;
    private LocalDateTime timestamp;

    // Construtores
    public LoginResponseDTO() {
        this.timestamp = LocalDateTime.now();
        this.tipoToken = "Bearer";
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTipoToken() {
        return tipoToken;
    }

    public void setTipoToken(String tipoToken) {
        this.tipoToken = tipoToken;
    }

    public LocalDateTime getExpiracaoToken() {
        return expiracaoToken;
    }

    public void setExpiracaoToken(LocalDateTime expiracaoToken) {
        this.expiracaoToken = expiracaoToken;
    }

    public UsuarioLoginDTO getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioLoginDTO usuario) {
        this.usuario = usuario;
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

    /**
     * DTO interno para informações do usuário no login
     * Referência: Controle de Acesso - project_rules.md
     */
    public static class UsuarioLoginDTO {
        private Long id;
        private String nome;
        private String sobrenome;
        private String email;
        private PapelUsuario papel;
        private String avatar;
        private boolean ativo;
        private LocalDateTime ultimoLogin;
        private boolean primeiroLogin;

        // Construtores
        public UsuarioLoginDTO() {}

        public UsuarioLoginDTO(Usuario usuario) {
            this.id = usuario.getId();
            this.nome = usuario.getNome();
            this.sobrenome = usuario.getSobrenome();
            this.email = usuario.getEmail();
            this.papel = usuario.getPapel();
            this.avatar = usuario.getAvatar();
            this.ativo = usuario.isAtivo();
            this.ultimoLogin = usuario.getUltimoLogin();
            this.primeiroLogin = usuario.getUltimoLogin() == null;
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

        public PapelUsuario getPapel() {
            return papel;
        }

        public void setPapel(PapelUsuario papel) {
            this.papel = papel;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }

        public boolean isAtivo() {
            return ativo;
        }

        public void setAtivo(boolean ativo) {
            this.ativo = ativo;
        }

        public LocalDateTime getUltimoLogin() {
            return ultimoLogin;
        }

        public void setUltimoLogin(LocalDateTime ultimoLogin) {
            this.ultimoLogin = ultimoLogin;
        }

        public boolean isPrimeiroLogin() {
            return primeiroLogin;
        }

        public void setPrimeiroLogin(boolean primeiroLogin) {
            this.primeiroLogin = primeiroLogin;
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
         * Verifica se o usuário tem permissões administrativas
         * Referência: Controle de Acesso - project_rules.md
         * 
         * @return true se é admin, fundador ou colaborador
         */
        public boolean temPermissoesAdministrativas() {
            return papel == PapelUsuario.ADMINISTRADOR || 
                   papel == PapelUsuario.FUNDADOR || 
                   papel == PapelUsuario.COLABORADOR;
        }
    }

    /**
     * Builder para facilitar criação de respostas
     * Referência: Padrões de Código - project_rules.md
     */
    public static class Builder {
        private final LoginResponseDTO response;

        public Builder() {
            this.response = new LoginResponseDTO();
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

        public Builder token(String token) {
            response.setToken(token);
            return this;
        }

        public Builder refreshToken(String refreshToken) {
            response.setRefreshToken(refreshToken);
            return this;
        }

        public Builder expiracaoToken(LocalDateTime expiracao) {
            response.setExpiracaoToken(expiracao);
            return this;
        }

        public Builder usuario(Usuario usuario) {
            response.setUsuario(new UsuarioLoginDTO(usuario));
            return this;
        }

        public Builder configuracoes(Map<String, Object> configuracoes) {
            response.setConfiguracoes(configuracoes);
            return this;
        }

        public LoginResponseDTO build() {
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
     * @param usuario Usuário autenticado
     * @param token Token JWT
     * @param refreshToken Refresh token
     * @return Resposta de sucesso
     */
    public static LoginResponseDTO sucesso(Usuario usuario, String token, String refreshToken) {
        return builder()
            .sucesso(true)
            .mensagem("Login realizado com sucesso")
            .token(token)
            .refreshToken(refreshToken)
            .usuario(usuario)
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
    public static LoginResponseDTO erro(String mensagem, String erro) {
        return builder()
            .sucesso(false)
            .mensagem(mensagem)
            .erro(erro)
            .build();
    }
}