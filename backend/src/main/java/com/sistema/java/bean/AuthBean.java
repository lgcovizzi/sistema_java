package com.sistema.java.bean;

import com.sistema.java.model.entity.Usuario;
import com.sistema.java.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;
import java.time.LocalDate;

@Component
@Scope("session")
public class AuthBean implements Serializable {
    
    @Autowired
    private AuthService authService;
    
    // Campos para login
    private String emailLogin;
    private String senhaLogin;
    
    // Campos para registro
    private Usuario novoUsuario;
    private String confirmaSenha;
    
    // Campos para reset de senha
    private String emailReset;
    private String tokenReset;
    private String novaSenha;
    private String confirmaNovaSenha;
    
    // Campos para verificação de email
    private String tokenVerificacao;
    
    // Estado da interface
    private boolean mostrarFormularioRegistro = false;
    private boolean mostrarFormularioReset = false;
    
    @PostConstruct
    public void init() {
        limparCampos();
    }
    
    /**
     * Realiza login do usuário
     */
    public String login() {
        try {
            // TODO: Implementar login com DTO
            // if (authService.login(emailLogin, senhaLogin)) {
            if (false) {
                limparCampos();
                
                // Redirecionar baseado no papel do usuário
                Usuario usuario = authService.getUsuarioLogado();
                // TODO: Implementar verificação de acesso ao dashboard
                if (usuario != null && true) { // usuario.canAccessDashboard()
                    return "/admin/dashboard?faces-redirect=true";
                } else {
                    return "/index?faces-redirect=true";
                }
            }
        } catch (Exception e) {
            addErrorMessage("Erro interno. Tente novamente.");
        }
        return null;
    }
    
    /**
     * Realiza logout do usuário
     */
    public String logout() {
        try {
            // TODO: Implementar logout com token
            // authService.logout();
            limparCampos();
            return "/index?faces-redirect=true";
        } catch (Exception e) {
            addErrorMessage("Erro ao fazer logout.");
            return null;
        }
    }
    
    /**
     * Registra novo usuário
     */
    public String registrar() {
        try {
            // TODO: Implementar registro com confirmação de senha
            // if (authService.register(novoUsuario, confirmaSenha)) {
            if (false) {
                limparCampos();
                mostrarFormularioRegistro = false;
                addInfoMessage("Cadastro realizado! Verifique seu email.");
                return "/login?faces-redirect=true";
            }
        } catch (Exception e) {
            addErrorMessage("Erro ao realizar cadastro.");
        }
        return null;
    }
    
    /**
     * Solicita reset de senha
     */
    public void solicitarResetSenha() {
        try {
            authService.solicitarResetSenha(emailReset);
            emailReset = "";
            mostrarFormularioReset = false;
        } catch (Exception e) {
            addErrorMessage("Erro ao solicitar reset de senha.");
        }
    }
    
    /**
     * Reseta senha do usuário
     */
    public String resetarSenha() {
        try {
            if (authService.resetarSenha(tokenReset, novaSenha, confirmaNovaSenha)) {
                limparCampos();
                return "/login?faces-redirect=true";
            }
        } catch (Exception e) {
            addErrorMessage("Erro ao resetar senha.");
        }
        return null;
    }
    
    /**
     * Verifica email do usuário
     */
    public String verificarEmail() {
        try {
            if (authService.verificarEmail(tokenVerificacao)) {
                limparCampos();
                return "/login?faces-redirect=true";
            }
        } catch (Exception e) {
            addErrorMessage("Erro ao verificar email.");
        }
        return null;
    }
    
    /**
     * Alterna exibição do formulário de registro
     */
    public void alternarFormularioRegistro() {
        mostrarFormularioRegistro = !mostrarFormularioRegistro;
        if (mostrarFormularioRegistro) {
            novoUsuario = new Usuario();
            confirmaSenha = "";
        }
    }
    
    /**
     * Alterna exibição do formulário de reset
     */
    public void alternarFormularioReset() {
        mostrarFormularioReset = !mostrarFormularioReset;
        if (mostrarFormularioReset) {
            emailReset = "";
        }
    }
    
    /**
     * Limpa todos os campos
     */
    public void limparCampos() {
        // Login
        emailLogin = "";
        senhaLogin = "";
        
        // Registro
        novoUsuario = new Usuario();
        confirmaSenha = "";
        
        // Reset
        emailReset = "";
        tokenReset = "";
        novaSenha = "";
        confirmaNovaSenha = "";
        
        // Verificação
        tokenVerificacao = "";
        
        // Estado
        mostrarFormularioRegistro = false;
        mostrarFormularioReset = false;
    }
    
    /**
     * Verifica se usuário está logado
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Estado de login deve considerar preferências de tema do usuário
     */
    public boolean isLogado() {
        return authService.isLogado();
    }
    
    /**
     * Obtém usuário logado
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Dados do usuário incluem preferências de tema para interface
     */
    public Usuario getUsuarioLogado() {
        return authService.getUsuarioLogado();
    }
    
    /**
     * Verifica se usuário pode acessar dashboard
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Dashboard deve carregar com tema preferido do usuário autenticado
     */
    public boolean canAccessDashboard() {
        return authService.canAccessDashboard();
    }
    
    /**
     * Verifica se usuário pode gerenciar usuários
     */
    public boolean canManageUsers() {
        return authService.canManageUsers();
    }
    
    /**
     * Verifica se usuário pode gerenciar conteúdo
     */
    public boolean canManageContent() {
        return authService.canManageContent();
    }
    
    /**
     * Verifica se usuário é admin
     */
    public boolean isAdmin() {
        return authService.isAdmin();
    }
    
    /**
     * Valida idade mínima (18 anos)
     */
    public void validarIdadeMinima() {
        if (novoUsuario.getDataNascimento() != null) {
            LocalDate hoje = LocalDate.now();
            LocalDate dataNascimento = novoUsuario.getDataNascimento();
            
            if (dataNascimento.plusYears(18).isAfter(hoje)) {
                addErrorMessage("Você deve ter pelo menos 18 anos para se cadastrar.");
                novoUsuario.setDataNascimento(null);
            }
        }
    }
    
    /**
     * Formata CPF durante digitação
     */
    public void formatarCpf() {
        if (novoUsuario.getCpf() != null) {
            String cpf = novoUsuario.getCpf().replaceAll("\\D", "");
            if (cpf.length() <= 11) {
                novoUsuario.setCpf(cpf);
            } else {
                novoUsuario.setCpf(cpf.substring(0, 11));
            }
        }
    }
    
    /**
     * Formata telefone durante digitação
     */
    public void formatarTelefone() {
        if (novoUsuario.getTelefone() != null) {
            String telefone = novoUsuario.getTelefone().replaceAll("\\D", "");
            if (telefone.length() <= 11) {
                novoUsuario.setTelefone(telefone);
            } else {
                novoUsuario.setTelefone(telefone.substring(0, 11));
            }
        }
    }
    
    // Getters e Setters
    
    public String getEmailLogin() {
        return emailLogin;
    }
    
    public void setEmailLogin(String emailLogin) {
        this.emailLogin = emailLogin;
    }
    
    public String getSenhaLogin() {
        return senhaLogin;
    }
    
    public void setSenhaLogin(String senhaLogin) {
        this.senhaLogin = senhaLogin;
    }
    
    public Usuario getNovoUsuario() {
        return novoUsuario;
    }
    
    public void setNovoUsuario(Usuario novoUsuario) {
        this.novoUsuario = novoUsuario;
    }
    
    public String getConfirmaSenha() {
        return confirmaSenha;
    }
    
    public void setConfirmaSenha(String confirmaSenha) {
        this.confirmaSenha = confirmaSenha;
    }
    
    public String getEmailReset() {
        return emailReset;
    }
    
    public void setEmailReset(String emailReset) {
        this.emailReset = emailReset;
    }
    
    public String getTokenReset() {
        return tokenReset;
    }
    
    public void setTokenReset(String tokenReset) {
        this.tokenReset = tokenReset;
    }
    
    public String getNovaSenha() {
        return novaSenha;
    }
    
    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }
    
    public String getConfirmaNovaSenha() {
        return confirmaNovaSenha;
    }
    
    public void setConfirmaNovaSenha(String confirmaNovaSenha) {
        this.confirmaNovaSenha = confirmaNovaSenha;
    }
    
    public String getTokenVerificacao() {
        return tokenVerificacao;
    }
    
    public void setTokenVerificacao(String tokenVerificacao) {
        this.tokenVerificacao = tokenVerificacao;
    }
    
    public boolean isMostrarFormularioRegistro() {
        return mostrarFormularioRegistro;
    }
    
    public void setMostrarFormularioRegistro(boolean mostrarFormularioRegistro) {
        this.mostrarFormularioRegistro = mostrarFormularioRegistro;
    }
    
    public boolean isMostrarFormularioReset() {
        return mostrarFormularioReset;
    }
    
    public void setMostrarFormularioReset(boolean mostrarFormularioReset) {
        this.mostrarFormularioReset = mostrarFormularioReset;
    }
    
    // Métodos utilitários
    
    private void addErrorMessage(String message) {
        // TODO: Implementar sistema de mensagens JSF adequado
        // Por enquanto, apenas log das mensagens
        System.out.println("ERROR: " + message);
    }
    
    private void addInfoMessage(String message) {
        // TODO: Implementar sistema de mensagens JSF adequado
        // Por enquanto, apenas log das mensagens
        System.out.println("INFO: " + message);
    }
}