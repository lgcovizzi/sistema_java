package com.sistema.java.bean;

import com.sistema.java.model.dto.UsuarioDTO;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;
import com.sistema.java.service.AuthService;
import com.sistema.java.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;

/**
 * Managed Bean para autenticação e registro de usuários
 * Referência: Login e Registro - project_rules.md
 * Referência: Controle de Acesso - project_rules.md
 */
@Named("authBean")
@SessionScoped
public class AuthBean implements Serializable {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthBean.class);
    
    @Inject
    private AuthService authService;
    
    @Inject
    private UsuarioService usuarioService;
    
    @Inject
    private FacesContext facesContext;
    
    @Inject
    private BCryptPasswordEncoder passwordEncoder;
    
    // Campos de login
    private String emailLogin;
    private String senhaLogin;
    
    // Campos de registro
    private Usuario novoUsuario = new Usuario();
    
    // Campos de reset de senha
    private String emailReset;
    
    // Estado da sessão
    private Usuario usuarioLogado;
    
    /**
     * Realiza o login do usuário
     * Referência: Login e Registro - project_rules.md
     */
    public String login() {
        try {
            logger.info("Tentativa de login para email: {}", emailLogin);
            
            // Validar dados de entrada
            if (emailLogin == null || emailLogin.trim().isEmpty() || 
                senhaLogin == null || senhaLogin.trim().isEmpty()) {
                addErrorMessage("Email e senha são obrigatórios");
                return null;
            }
            
            // Autenticar usuário
            Usuario usuario = authService.autenticar(emailLogin.trim(), senhaLogin);
            
            if (usuario != null) {
                usuarioLogado = usuario;
                logger.info("Login realizado com sucesso para usuário: {} ({})", 
                           usuario.getEmail(), usuario.getPapel());
                
                // Limpar campos de login
                limparCamposLogin();
                
                // Redirecionar baseado no papel do usuário
                if (usuario.getPapel() == PapelUsuario.ADMINISTRADOR || 
                    usuario.getPapel() == PapelUsuario.FUNDADOR) {
                    return "/admin/dashboard?faces-redirect=true";
                } else {
                    return "/dashboard?faces-redirect=true";
                }
            } else {
                logger.warn("Credenciais inválidas para email: {}", emailLogin);
                addErrorMessage("Email ou senha incorretos");
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Erro durante login para email: {}", emailLogin, e);
            addErrorMessage("Erro interno. Tente novamente.");
            return null;
        }
    }
    
    /**
     * Realiza o registro de novo usuário
     * Referência: Login e Registro - project_rules.md
     */
    public String registrar() {
        try {
            logger.info("Tentativa de registro para email: {}", novoUsuario.getEmail());
            
            // Validar dados básicos
            if (!validarDadosRegistro()) {
                return null;
            }
            
            // Verificar se email já existe
            if (usuarioService.existsByEmail(novoUsuario.getEmail())) {
                addErrorMessage("Este email já está cadastrado");
                return null;
            }
            
            // Verificar se CPF já existe
            if (usuarioService.existsByCpf(novoUsuario.getCpf())) {
                addErrorMessage("Este CPF já está cadastrado");
                return null;
            }
            
            // Validar idade mínima (16 anos)
            if (novoUsuario.getDataNascimento() != null) {
                int idade = Period.between(novoUsuario.getDataNascimento(), LocalDate.now()).getYears();
                if (idade < 16) {
                    addErrorMessage("Idade mínima para cadastro é 16 anos");
                    return null;
                }
            }
            
            // Criar DTO para o serviço
            UsuarioDTO usuarioDTO = new UsuarioDTO();
            usuarioDTO.setNome(novoUsuario.getNome());
            usuarioDTO.setSobrenome(novoUsuario.getSobrenome());
            usuarioDTO.setCpf(novoUsuario.getCpf());
            usuarioDTO.setEmail(novoUsuario.getEmail());
            usuarioDTO.setSenha(novoUsuario.getSenha());
            usuarioDTO.setTelefone(novoUsuario.getTelefone());
            usuarioDTO.setDataNascimento(novoUsuario.getDataNascimento());
            usuarioDTO.setPapel(PapelUsuario.USUARIO); // Papel padrão
            
            // Criar usuário
            UsuarioDTO usuarioCriado = usuarioService.create(usuarioDTO);
            
            if (usuarioCriado != null) {
                logger.info("Usuário registrado com sucesso: {}", usuarioCriado.getEmail());
                addSuccessMessage("Cadastro realizado com sucesso! Faça login para continuar.");
                
                // Limpar formulário
                novoUsuario = new Usuario();
                
                return "/login?faces-redirect=true";
            } else {
                addErrorMessage("Erro ao criar usuário. Tente novamente.");
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Erro durante registro para email: {}", novoUsuario.getEmail(), e);
            addErrorMessage("Erro interno. Tente novamente.");
            return null;
        }
    }
    
    /**
     * Realiza o logout do usuário
     */
    public String logout() {
        try {
            logger.info("Logout do usuário: {}", usuarioLogado != null ? usuarioLogado.getEmail() : "desconhecido");
            
            // Limpar sessão
            usuarioLogado = null;
            limparCamposLogin();
            
            // Invalidar sessão JSF
            ExternalContext externalContext = facesContext.getExternalContext();
            externalContext.invalidateSession();
            
            return "/index?faces-redirect=true";
            
        } catch (Exception e) {
            logger.error("Erro durante logout", e);
            return "/index?faces-redirect=true";
        }
    }
    
    /**
     * Solicita reset de senha
     */
    public void solicitarResetSenha() {
        try {
            if (emailReset == null || emailReset.trim().isEmpty()) {
                addErrorMessage("Email é obrigatório");
                return;
            }
            
            // Verificar se email existe
            if (usuarioService.findByEmail(emailReset.trim()).isPresent()) {
                // TODO: Implementar envio de email de reset
                addSuccessMessage("Se o email estiver cadastrado, você receberá instruções para redefinir sua senha.");
                emailReset = "";
            } else {
                // Por segurança, sempre mostrar a mesma mensagem
                addSuccessMessage("Se o email estiver cadastrado, você receberá instruções para redefinir sua senha.");
                emailReset = "";
            }
            
        } catch (Exception e) {
            logger.error("Erro ao solicitar reset de senha para email: {}", emailReset, e);
            addErrorMessage("Erro interno. Tente novamente.");
        }
    }
    
    /**
     * Redireciona para dashboard se usuário já está autenticado
     */
    public void redirectToDashboard() {
        if (isAuthenticated()) {
            try {
                ExternalContext externalContext = facesContext.getExternalContext();
                if (usuarioLogado.getPapel() == PapelUsuario.ADMINISTRADOR || 
                    usuarioLogado.getPapel() == PapelUsuario.FUNDADOR) {
                    externalContext.redirect(externalContext.getRequestContextPath() + "/admin/dashboard.xhtml");
                } else {
                    externalContext.redirect(externalContext.getRequestContextPath() + "/dashboard.xhtml");
                }
            } catch (Exception e) {
                logger.error("Erro ao redirecionar usuário autenticado", e);
            }
        }
    }
    
    /**
     * Verifica se usuário está autenticado
     */
    public boolean isAuthenticated() {
        return usuarioLogado != null;
    }
    
    // Métodos de validação
    private boolean validarDadosRegistro() {
        if (novoUsuario.getNome() == null || novoUsuario.getNome().trim().isEmpty()) {
            addErrorMessage("Nome é obrigatório");
            return false;
        }
        
        if (novoUsuario.getSobrenome() == null || novoUsuario.getSobrenome().trim().isEmpty()) {
            addErrorMessage("Sobrenome é obrigatório");
            return false;
        }
        
        if (novoUsuario.getCpf() == null || novoUsuario.getCpf().trim().isEmpty()) {
            addErrorMessage("CPF é obrigatório");
            return false;
        }
        
        if (novoUsuario.getEmail() == null || novoUsuario.getEmail().trim().isEmpty()) {
            addErrorMessage("Email é obrigatório");
            return false;
        }
        
        if (novoUsuario.getSenha() == null || novoUsuario.getSenha().length() < 8) {
            addErrorMessage("Senha deve ter pelo menos 8 caracteres");
            return false;
        }
        
        return true;
    }
    
    // Métodos utilitários
    private void limparCamposLogin() {
        emailLogin = "";
        senhaLogin = "";
    }
    
    private void addErrorMessage(String message) {
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", message));
    }
    
    private void addSuccessMessage(String message) {
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", message));
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
    
    public String getEmailReset() {
        return emailReset;
    }
    
    public void setEmailReset(String emailReset) {
        this.emailReset = emailReset;
    }
    
    public Usuario getUsuarioLogado() {
        return usuarioLogado;
    }
    
    public void setUsuarioLogado(Usuario usuarioLogado) {
        this.usuarioLogado = usuarioLogado;
    }
}