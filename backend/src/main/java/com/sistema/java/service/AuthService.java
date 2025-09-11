package com.sistema.java.service;

import com.sistema.java.model.Usuario;
import com.sistema.java.model.enums.PapelUsuario;
import com.sistema.java.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class AuthService {
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private EmailService emailService;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    private static final String SESSION_USER_KEY = "usuarioLogado";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    private static final Pattern CPF_PATTERN = Pattern.compile("^\\d{11}$");
    private static final Pattern TELEFONE_PATTERN = Pattern.compile(
        "^\\(?\\d{2}\\)?[\\s-]?\\d{4,5}[\\s-]?\\d{4}$"
    );
    
    /**
     * Realiza login do usuário
     */
    public boolean login(String email, String senha) {
        try {
            if (email == null || senha == null || email.trim().isEmpty() || senha.trim().isEmpty()) {
                addErrorMessage("Email e senha são obrigatórios");
                return false;
            }
            
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmailAndAtivoTrue(email.trim().toLowerCase());
            
            if (usuarioOpt.isEmpty()) {
                addErrorMessage("Credenciais inválidas");
                return false;
            }
            
            Usuario usuario = usuarioOpt.get();
            
            if (!passwordEncoder.matches(senha, usuario.getSenha())) {
                addErrorMessage("Credenciais inválidas");
                return false;
            }
            
            if (!usuario.getEmailVerificado()) {
                addErrorMessage("Email não verificado. Verifique sua caixa de entrada.");
                return false;
            }
            
            // Atualizar último acesso
            usuario.setUltimoAcesso(LocalDateTime.now());
            usuarioRepository.save(usuario);
            
            // Criar sessão
            createUserSession(usuario);
            
            addInfoMessage("Login realizado com sucesso!");
            return true;
            
        } catch (Exception e) {
            addErrorMessage("Erro interno. Tente novamente.");
            return false;
        }
    }
    
    /**
     * Realiza logout do usuário
     */
    public void logout() {
        HttpSession session = getSession();
        if (session != null) {
            session.invalidate();
        }
        addInfoMessage("Logout realizado com sucesso!");
    }
    
    /**
     * Registra novo usuário
     */
    public boolean register(Usuario usuario, String confirmaSenha) {
        try {
            // Validações básicas
            if (!validateUserData(usuario, confirmaSenha)) {
                return false;
            }
            
            // Verificar se email já existe
            if (usuarioRepository.existsByEmail(usuario.getEmail().toLowerCase())) {
                addErrorMessage("Email já cadastrado no sistema");
                return false;
            }
            
            // Verificar se CPF já existe (se informado)
            if (usuario.getCpf() != null && !usuario.getCpf().trim().isEmpty()) {
                if (usuarioRepository.existsByCpf(usuario.getCpf())) {
                    addErrorMessage("CPF já cadastrado no sistema");
                    return false;
                }
            }
            
            // Preparar usuário
            usuario.setEmail(usuario.getEmail().toLowerCase());
            usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
            usuario.setPapel(PapelUsuario.USUARIO);
            usuario.setAtivo(true);
            usuario.setEmailVerificado(false);
            usuario.setTokenVerificacao(UUID.randomUUID().toString());
            usuario.setDataExpiracaoToken(LocalDateTime.now().plusHours(24));
            
            // Salvar usuário
            Usuario usuarioSalvo = usuarioRepository.save(usuario);
            
            // Enviar email de verificação
            emailService.enviarEmailVerificacao(usuarioSalvo);
            
            addInfoMessage("Cadastro realizado! Verifique seu email para ativar a conta.");
            return true;
            
        } catch (Exception e) {
            addErrorMessage("Erro ao realizar cadastro. Tente novamente.");
            return false;
        }
    }
    
    /**
     * Verifica email do usuário
     */
    public boolean verificarEmail(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                addErrorMessage("Token inválido");
                return false;
            }
            
            Optional<Usuario> usuarioOpt = usuarioRepository.findByTokenVerificacao(token);
            
            if (usuarioOpt.isEmpty()) {
                addErrorMessage("Token inválido ou expirado");
                return false;
            }
            
            Usuario usuario = usuarioOpt.get();
            
            if (!usuario.isTokenValid()) {
                addErrorMessage("Token expirado. Solicite um novo email de verificação.");
                return false;
            }
            
            usuario.setEmailVerificado(true);
            usuario.setTokenVerificacao(null);
            usuario.setDataExpiracaoToken(null);
            usuarioRepository.save(usuario);
            
            addInfoMessage("Email verificado com sucesso! Agora você pode fazer login.");
            return true;
            
        } catch (Exception e) {
            addErrorMessage("Erro ao verificar email. Tente novamente.");
            return false;
        }
    }
    
    /**
     * Solicita reset de senha
     */
    public boolean solicitarResetSenha(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                addErrorMessage("Email é obrigatório");
                return false;
            }
            
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmailAndAtivoTrue(email.toLowerCase());
            
            if (usuarioOpt.isEmpty()) {
                // Por segurança, sempre retorna sucesso
                addInfoMessage("Se o email existir, você receberá instruções para reset.");
                return true;
            }
            
            Usuario usuario = usuarioOpt.get();
            usuario.setTokenResetSenha(UUID.randomUUID().toString());
            usuario.setDataExpiracaoToken(LocalDateTime.now().plusHours(2));
            usuarioRepository.save(usuario);
            
            emailService.enviarEmailResetSenha(usuario);
            
            addInfoMessage("Se o email existir, você receberá instruções para reset.");
            return true;
            
        } catch (Exception e) {
            addErrorMessage("Erro ao solicitar reset. Tente novamente.");
            return false;
        }
    }
    
    /**
     * Reseta senha do usuário
     */
    public boolean resetarSenha(String token, String novaSenha, String confirmaSenha) {
        try {
            if (token == null || novaSenha == null || confirmaSenha == null) {
                addErrorMessage("Todos os campos são obrigatórios");
                return false;
            }
            
            if (!novaSenha.equals(confirmaSenha)) {
                addErrorMessage("Senhas não conferem");
                return false;
            }
            
            if (novaSenha.length() < 8) {
                addErrorMessage("Senha deve ter pelo menos 8 caracteres");
                return false;
            }
            
            Optional<Usuario> usuarioOpt = usuarioRepository.findByTokenResetSenha(token);
            
            if (usuarioOpt.isEmpty()) {
                addErrorMessage("Token inválido ou expirado");
                return false;
            }
            
            Usuario usuario = usuarioOpt.get();
            
            if (!usuario.isTokenValid()) {
                addErrorMessage("Token expirado. Solicite um novo reset de senha.");
                return false;
            }
            
            usuario.setSenha(passwordEncoder.encode(novaSenha));
            usuario.setTokenResetSenha(null);
            usuario.setDataExpiracaoToken(null);
            usuarioRepository.save(usuario);
            
            addInfoMessage("Senha alterada com sucesso! Faça login com a nova senha.");
            return true;
            
        } catch (Exception e) {
            addErrorMessage("Erro ao resetar senha. Tente novamente.");
            return false;
        }
    }
    
    /**
     * Obtém usuário logado
     */
    public Usuario getUsuarioLogado() {
        HttpSession session = getSession();
        if (session != null) {
            return (Usuario) session.getAttribute(SESSION_USER_KEY);
        }
        return null;
    }
    
    /**
     * Verifica se usuário está logado
     */
    public boolean isLogado() {
        return getUsuarioLogado() != null;
    }
    
    /**
     * Verifica se usuário tem acesso ao dashboard
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Dashboard deve carregar com tema preferido do usuário
     */
    public boolean canAccessDashboard() {
        Usuario usuario = getUsuarioLogado();
        return usuario != null && usuario.canAccessDashboard();
    }
    
    /**
     * Verifica se usuário pode gerenciar usuários
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Interface administrativa deve manter consistência de tema
     */
    public boolean canManageUsers() {
        Usuario usuario = getUsuarioLogado();
        return usuario != null && usuario.canManageUsers();
    }
    
    /**
     * Verifica se usuário pode gerenciar conteúdo
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Editores de conteúdo devem adaptar-se ao tema selecionado
     */
    public boolean canManageContent() {
        Usuario usuario = getUsuarioLogado();
        return usuario != null && usuario.canManageContent();
    }
    
    /**
     * Verifica se usuário é admin
     */
    public boolean isAdmin() {
        Usuario usuario = getUsuarioLogado();
        return usuario != null && usuario.isAdmin();
    }
    
    // Métodos privados
    
    private boolean validateUserData(Usuario usuario, String confirmaSenha) {
        if (usuario.getNome() == null || usuario.getNome().trim().length() < 2) {
            addErrorMessage("Nome deve ter pelo menos 2 caracteres");
            return false;
        }
        
        if (usuario.getEmail() == null || !EMAIL_PATTERN.matcher(usuario.getEmail()).matches()) {
            addErrorMessage("Email deve ter formato válido");
            return false;
        }
        
        if (usuario.getSenha() == null || usuario.getSenha().length() < 8) {
            addErrorMessage("Senha deve ter pelo menos 8 caracteres");
            return false;
        }
        
        if (!usuario.getSenha().equals(confirmaSenha)) {
            addErrorMessage("Senhas não conferem");
            return false;
        }
        
        if (usuario.getCpf() != null && !usuario.getCpf().trim().isEmpty()) {
            if (!CPF_PATTERN.matcher(usuario.getCpf()).matches()) {
                addErrorMessage("CPF deve ter exatamente 11 dígitos");
                return false;
            }
        }
        
        if (usuario.getTelefone() != null && !usuario.getTelefone().trim().isEmpty()) {
            if (!TELEFONE_PATTERN.matcher(usuario.getTelefone()).matches()) {
                addErrorMessage("Telefone deve ter formato válido");
                return false;
            }
        }
        
        return true;
    }
    
    private void createUserSession(Usuario usuario) {
        HttpSession session = getSession();
        if (session != null) {
            session.setAttribute(SESSION_USER_KEY, usuario);
            session.setMaxInactiveInterval(3600); // 1 hora
        }
    }
    
    private HttpSession getSession() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null && facesContext.getExternalContext() != null) {
            return (HttpSession) facesContext.getExternalContext().getSession(true);
        }
        return null;
    }
    
    private void addErrorMessage(String message) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", message));
        }
    }
    
    private void addInfoMessage(String message) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", message));
        }
    }
}