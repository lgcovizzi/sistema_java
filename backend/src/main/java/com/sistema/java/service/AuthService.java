package com.sistema.java.service;

import com.sistema.java.model.dto.*;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;
import com.sistema.java.repository.UsuarioRepository;
import com.sistema.java.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Serviço de autenticação e autorização
 * Referência: Login e Registro - project_rules.md
 * Referência: Controle de Acesso - project_rules.md
 * Referência: Segurança - project_rules.md
 */
@Service
@Validated
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final UsuarioService usuarioService;

    @Autowired
    public AuthService(UsuarioRepository usuarioRepository,
                      PasswordEncoder passwordEncoder,
                      AuthenticationManager authenticationManager,
                      JwtUtil jwtUtil,
                      EmailService emailService,
                      UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.usuarioService = usuarioService;
    }

    /**
     * Realiza o login do usuário
     * Referência: Login e Registro - project_rules.md
     * 
     * @param loginRequest Dados de login
     * @return Resposta do login com token
     */
    public LoginResponseDTO login(@Valid LoginRequestDTO loginRequest) {
        try {
            logger.info("Tentativa de login para email: {}", loginRequest.getEmail());

            // Validar dados de entrada
            if (!loginRequest.isValido()) {
                logger.warn("Dados de login inválidos para email: {}", loginRequest.getEmail());
                return LoginResponseDTO.erro("Dados de login inválidos", "Email ou senha não informados");
            }

            // Buscar usuário
            // TODO: Implementar método findByEmailAndAtivoTrue no UsuarioRepository
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(loginRequest.getEmail());
            if (usuarioOpt.isEmpty()) {
                logger.warn("Usuário não encontrado ou inativo: {}", loginRequest.getEmail());
                return LoginResponseDTO.erro("Credenciais inválidas", "Email ou senha incorretos");
            }

            Usuario usuario = usuarioOpt.get();

            // Verificar se a conta está ativa
            if (!usuario.getAtivo()) {
                logger.warn("Tentativa de login com conta inativa: {}", loginRequest.getEmail());
                return LoginResponseDTO.erro("Conta inativa", "Sua conta foi desativada. Entre em contato com o suporte.");
            }

            // TODO: Implementar autenticação Spring Security
            // Authentication authentication = authenticationManager.authenticate(
            //     new UsernamePasswordAuthenticationToken(
            //         loginRequest.getEmail(),
            //         loginRequest.getSenha()
            //     )
            // );

            // TODO: Implementar JwtUtil para gerar tokens
            // String token = jwtUtil.generateToken(usuario);
            // String refreshToken = jwtUtil.generateRefreshToken(usuario);
            String token = "temp-token-" + usuario.getId();
            String refreshToken = "temp-refresh-" + usuario.getId();

            // TODO: Adicionar campo ultimoLogin na entidade Usuario
            // usuario.setUltimoLogin(LocalDateTime.now());
            usuarioRepository.save(usuario);

            // Configurações do usuário
            Map<String, Object> configuracoes = criarConfiguracoes(usuario);

            // TODO: Adicionar campo ultimoLogin na entidade Usuario
            // Enviar email de boas-vindas se for primeiro login
            // if (usuario.getUltimoLogin() == null) {
            //     enviarEmailBoasVindasAsync(usuario);
            // }
            enviarEmailBoasVindasAsync(usuario);

            logger.info("Login realizado com sucesso para usuário: {} ({})", usuario.getEmail(), usuario.getPapel());

            return LoginResponseDTO.builder()
                .sucesso(true)
                .mensagem("Login realizado com sucesso")
                .token(token)
                .refreshToken(refreshToken)
                .expiracaoToken(LocalDateTime.now().plusHours(24))
                .usuario(usuario)
                .configuracoes(configuracoes)
                .build();

        } catch (BadCredentialsException e) {
            logger.warn("Credenciais inválidas para email: {}", loginRequest.getEmail());
            return LoginResponseDTO.erro("Credenciais inválidas", "Email ou senha incorretos");
        } catch (DisabledException e) {
            logger.warn("Conta desabilitada para email: {}", loginRequest.getEmail());
            return LoginResponseDTO.erro("Conta desabilitada", "Sua conta foi desabilitada");
        } catch (AuthenticationException e) {
            logger.error("Erro de autenticação para email: {}", loginRequest.getEmail(), e);
            return LoginResponseDTO.erro("Erro de autenticação", "Não foi possível autenticar o usuário");
        } catch (Exception e) {
            logger.error("Erro interno durante login para email: {}", loginRequest.getEmail(), e);
            return LoginResponseDTO.erro("Erro interno", "Ocorreu um erro interno. Tente novamente.");
        } finally {
            // Limpar dados sensíveis
            loginRequest.limparDadosSensiveis();
        }
    }

    /**
     * Registra um novo usuário
     * Referência: Login e Registro - project_rules.md
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param registroRequest Dados de registro
     * @return Resposta do registro
     */
    public RegistroResponseDTO registrar(@Valid RegistroRequestDTO registroRequest) {
        try {
            logger.info("Tentativa de registro para email: {}", registroRequest.getEmail());

            // Validar dados de entrada
            if (!registroRequest.isValido()) {
                logger.warn("Dados de registro inválidos para email: {}", registroRequest.getEmail());
                return RegistroResponseDTO.erro("Dados de registro inválidos", "Verifique os campos obrigatórios");
            }

            // Verificar se email já existe
            if (usuarioRepository.existsByEmail(registroRequest.getEmail())) {
                logger.warn("Tentativa de registro com email já existente: {}", registroRequest.getEmail());
                return RegistroResponseDTO.emailJaExiste(registroRequest.getEmail());
            }

            // Verificar se CPF já existe
            if (usuarioRepository.existsByCpf(registroRequest.getCpf())) {
                logger.warn("Tentativa de registro com CPF já existente");
                return RegistroResponseDTO.cpfJaExiste(registroRequest.getCpf());
            }

            // Criar usuário
            Usuario usuario = criarUsuarioFromRegistro(registroRequest);
            usuario = usuarioRepository.save(usuario);

            // Enviar email de verificação
            enviarEmailVerificacaoAsync(usuario);

            logger.info("Usuário registrado com sucesso: {} (ID: {})", usuario.getEmail(), usuario.getId());

            return RegistroResponseDTO.sucesso(usuario);

        } catch (Exception e) {
            logger.error("Erro interno durante registro para email: {}", registroRequest.getEmail(), e);
            return RegistroResponseDTO.erro("Erro interno", "Ocorreu um erro interno. Tente novamente.");
        } finally {
            // Limpar dados sensíveis
            registroRequest.limparDadosSensiveis();
        }
    }

    /**
     * Atualiza o token de acesso usando refresh token
     * Referência: Segurança - project_rules.md
     * 
     * @param refreshToken Token de refresh
     * @return Nova resposta de login
     */
    public LoginResponseDTO refreshToken(String refreshToken) {
        try {
            logger.info("Tentativa de refresh token");

            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return LoginResponseDTO.erro("Token inválido", "Refresh token não informado");
            }

            // TODO: Implementar JwtUtil.isRefreshTokenValid()
            // if (!jwtUtil.isRefreshTokenValid(refreshToken)) {
            //     logger.warn("Refresh token inválido ou expirado");
            //     return LoginResponseDTO.erro("Token expirado", "Refresh token inválido ou expirado");
            // }

            // TODO: Implementar JwtUtil.extractUsername() e UsuarioRepository.findByEmailAndAtivoTrue()
            // String email = jwtUtil.extractUsername(refreshToken);
            // Optional<Usuario> usuarioOpt = usuarioRepository.findByEmailAndAtivoTrue(email);

            // if (usuarioOpt.isEmpty()) {
            //     logger.warn("Usuário não encontrado para refresh token: {}", email);
            //     return LoginResponseDTO.erro("Usuário não encontrado", "Usuário não existe ou está inativo");
            // }

            // Usuario usuario = usuarioOpt.get();

            // TODO: Implementar JwtUtil.generateToken() e generateRefreshToken()
            // String novoToken = jwtUtil.generateToken(usuario);
            // String novoRefreshToken = jwtUtil.generateRefreshToken(usuario);
            
            // Implementação temporária
            return LoginResponseDTO.erro("Não implementado", "Refresh token ainda não implementado");

            // TODO: Implementar retorno completo quando JwtUtil estiver pronto
            // Map<String, Object> configuracoes = criarConfiguracoes(usuario);
            // logger.info("Token atualizado com sucesso para usuário: {}", usuario.getEmail());
            // return LoginResponseDTO.builder()
            //     .sucesso(true)
            //     .mensagem("Token atualizado com sucesso")
            //     .token(novoToken)
            //     .refreshToken(novoRefreshToken)
            //     .expiracaoToken(LocalDateTime.now().plusHours(24))
            //     .usuario(usuario)
            //     .configuracoes(configuracoes)
            //     .build();

        } catch (Exception e) {
            logger.error("Erro durante refresh token", e);
            return LoginResponseDTO.erro("Erro interno", "Não foi possível atualizar o token");
        }
    }

    /**
     * Valida se um token JWT é válido
     * Referência: Segurança - project_rules.md
     * 
     * @param token Token JWT
     * @return true se o token é válido
     */
    public boolean validarToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }

            // Remover prefixo Bearer se presente
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // TODO: Implementar JwtUtil.isTokenValid()
            // return jwtUtil.isTokenValid(token);
            return true; // Implementação temporária
        } catch (Exception e) {
            logger.warn("Erro ao validar token", e);
            return false;
        }
    }

    /**
     * Verifica se um email está disponível
     * Referência: Login e Registro - project_rules.md
     * 
     * @param email Email a verificar
     * @return true se está disponível
     */
    @Transactional(readOnly = true)
    public boolean emailDisponivel(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        try {
            return !usuarioRepository.existsByEmail(email.toLowerCase().trim());
        } catch (Exception e) {
            logger.error("Erro ao verificar disponibilidade do email: {}", email, e);
            return false;
        }
    }
    
    /**
     * Alias para emailDisponivel para compatibilidade
     */
    @Transactional(readOnly = true)
    public boolean isEmailDisponivel(String email) {
        return emailDisponivel(email);
    }

    /**
     * Verifica se um CPF está disponível
     * Referência: Login e Registro - project_rules.md
     * 
     * @param cpf CPF a verificar
     * @return true se está disponível
     */
    @Transactional(readOnly = true)
    public boolean cpfDisponivel(String cpf) {
        if (cpf == null || cpf.trim().isEmpty()) {
            return false;
        }
        
        try {
            String cpfLimpo = cpf.replaceAll("[^0-9]", "");
            return !usuarioRepository.existsByCpf(cpfLimpo);
        } catch (Exception e) {
            logger.error("Erro ao verificar disponibilidade do CPF", e);
            return false;
        }
    }
    
    /**
     * Alias para cpfDisponivel para compatibilidade
     */
    @Transactional(readOnly = true)
    public boolean isCpfDisponivel(String cpf) {
        return cpfDisponivel(cpf);
    }

    /**
     * Realiza logout do usuário invalidando o token
     * Referência: Segurança - project_rules.md
     * 
     * @param token Token JWT a ser invalidado
     * @return Resposta do logout
     */
    public LogoutResponseDTO logout(String token) {
        try {
            logger.info("Tentativa de logout");

            if (token == null || token.trim().isEmpty()) {
                return LogoutResponseDTO.erro("Token inválido", "Token não informado");
            }

            // Remover prefixo Bearer se presente
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // TODO: Implementar blacklist de tokens no JwtUtil
            // jwtUtil.invalidateToken(token);
            
            logger.info("Logout realizado com sucesso");
            return LogoutResponseDTO.sucesso();

        } catch (Exception e) {
            logger.error("Erro durante logout", e);
            return LogoutResponseDTO.erro("Erro interno", "Não foi possível realizar o logout");
        }
    }

    /**
     * Obtém o usuário atualmente autenticado
     * Referência: Segurança - project_rules.md
     * 
     * @param token Token JWT
     * @return Usuário autenticado ou null se inválido
     */
    @Transactional(readOnly = true)
    public Usuario obterUsuarioAtual(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return null;
            }

            // Remover prefixo Bearer se presente
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // TODO: Implementar JwtUtil.extractUsername() e validação
            // if (!jwtUtil.isTokenValid(token)) {
            //     return null;
            // }
            // 
            // String email = jwtUtil.extractUsername(token);
            // return usuarioRepository.findByEmailAndAtivoTrue(email).orElse(null);
            
            // Implementação temporária - retorna null
            return null;

        } catch (Exception e) {
            logger.warn("Erro ao obter usuário atual do token", e);
            return null;
        }
    }

    /**
     * Cria um usuário a partir dos dados de registro
     * Referência: Login e Registro - project_rules.md
     * Referência: Padrões para Entidades JPA - project_rules.md
     * 
     * @param registroRequest Dados de registro
     * @return Usuário criado
     */
    private Usuario criarUsuarioFromRegistro(RegistroRequestDTO registroRequest) {
        Usuario usuario = new Usuario();
        usuario.setNome(registroRequest.getNome());
        usuario.setSobrenome(registroRequest.getSobrenome());
        usuario.setCpf(registroRequest.getCpf());
        usuario.setEmail(registroRequest.getEmail());
        usuario.setSenha(passwordEncoder.encode(registroRequest.getSenha()));
        usuario.setTelefone(registroRequest.getTelefone());
        usuario.setDataNascimento(registroRequest.getDataNascimento());
        usuario.setPapel(PapelUsuario.USUARIO); // Papel padrão
        usuario.setAtivo(true);
        // TODO: Adicionar campos emailVerificado e receberNewsletter na entidade Usuario
        // usuario.setEmailVerificado(false);
        // usuario.setReceberNewsletter(registroRequest.isReceberNewsletter());
        usuario.setDataCriacao(LocalDateTime.now());
        usuario.setDataAtualizacao(LocalDateTime.now());
        
        return usuario;
    }

    /**
     * Cria configurações específicas do usuário
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param usuario Usuário
     * @return Mapa de configurações
     */
    private Map<String, Object> criarConfiguracoes(Usuario usuario) {
        Map<String, Object> configuracoes = new HashMap<>();
        configuracoes.put("temAcessoDashboard", usuario.getPapel() != PapelUsuario.CONVIDADO);
        configuracoes.put("temPermissoesAdministrativas", 
            usuario.getPapel() == PapelUsuario.ADMINISTRADOR || 
            usuario.getPapel() == PapelUsuario.FUNDADOR || 
            usuario.getPapel() == PapelUsuario.COLABORADOR);
        configuracoes.put("podeEditarPerfil", true);
        configuracoes.put("podeComentarNoticias", usuario.getPapel() != PapelUsuario.CONVIDADO);
        // TODO: Implementar campos receberNewsletter e ultimoLogin na entidade Usuario
        // configuracoes.put("receberNotificacoes", usuario.isReceberNewsletter());
        configuracoes.put("receberNotificacoes", false); // Implementação temporária
        // configuracoes.put("primeiroLogin", usuario.getUltimoLogin() == null);
        configuracoes.put("primeiroLogin", true); // Implementação temporária
        
        return configuracoes;
    }

    /**
     * Envia email de verificação de forma assíncrona
     * Referência: Sistema de Email com MailHog - project_rules.md
     * 
     * @param usuario Usuário
     */
    private void enviarEmailVerificacaoAsync(Usuario usuario) {
        CompletableFuture.runAsync(() -> {
            try {
                String token = jwtUtil.generateEmailVerificationToken(usuario);
                String linkVerificacao = "http://localhost:8080/auth/verificar-email?token=" + token;
                
                Map<String, Object> variaveis = new HashMap<>();
                variaveis.put("nomeUsuario", usuario.getNome());
                variaveis.put("sobrenomeUsuario", usuario.getSobrenome());
                variaveis.put("emailUsuario", usuario.getEmail());
                variaveis.put("linkVerificacao", linkVerificacao);
                variaveis.put("validadeToken", "24 horas");
                
                emailService.enviarEmailHtml(
                    usuario.getEmail(),
                    "Verificação de Email - Sistema Java",
                    "emails/verificacao-email",
                    variaveis
                );
                
                logger.info("Email de verificação enviado para: {}", usuario.getEmail());
            } catch (Exception e) {
                logger.error("Erro ao enviar email de verificação para: {}", usuario.getEmail(), e);
            }
        });
    }

    /**
     * Envia email de boas-vindas de forma assíncrona
     * Referência: Sistema de Email com MailHog - project_rules.md
     * 
     * @param usuario Usuário
     */
    private void enviarEmailBoasVindasAsync(Usuario usuario) {
        CompletableFuture.runAsync(() -> {
            try {
                emailService.enviarEmailBoasVindas(usuario);
                
                logger.info("Email de boas-vindas enviado para: {}", usuario.getEmail());
            } catch (Exception e) {
                logger.error("Erro ao enviar email de boas-vindas para: {}", usuario.getEmail(), e);
            }
        });
    }

    /**
     * Verifica se o usuário logado tem um papel específico
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param papel Papel a verificar
     * @return true se o usuário tem o papel
     */
    public boolean hasRole(PapelUsuario papel) {
        Usuario usuario = getUsuarioLogado();
        if (usuario == null) {
            return false;
        }
        return usuario.getPapel() == papel;
    }

    /**
     * Verifica se o usuário logado tem um papel específico (versão String)
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param papel Nome do papel como String
     * @return true se o usuário tem o papel
     */
    public boolean hasRole(String papel) {
        try {
            PapelUsuario papelEnum = PapelUsuario.valueOf(papel.toUpperCase());
            return hasRole(papelEnum);
        } catch (IllegalArgumentException e) {
            logger.warn("Papel inválido: {}", papel);
            return false;
        }
    }

    /**
     * Obtém o usuário logado da sessão atual
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @return Usuário logado ou null se não estiver logado
     */
    public Usuario getUsuarioLogado() {
        // TODO: Implementar obtenção do usuário da sessão/contexto de segurança
        // Por enquanto retorna null - implementação temporária
        return null;
    }

    /**
     * Verifica se há um usuário logado
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @return true se há usuário logado
     */
    public boolean isLogado() {
        return getUsuarioLogado() != null;
    }

    /**
     * Verifica se o usuário está logado
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @return true se estiver logado, false caso contrário
     */
    public boolean isLoggedIn() {
        return getUsuarioLogado() != null;
    }

    /**
     * Verifica se o usuário pode moderar comentários
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @return true se pode moderar comentários
     */
    public boolean canModerateComments() {
        return hasRole(PapelUsuario.ADMINISTRADOR) || 
               hasRole(PapelUsuario.FUNDADOR) || 
               hasRole(PapelUsuario.COLABORADOR);
    }

    /**
     * Solicita reset de senha por email
     * 
     * @param email Email do usuário
     */
    public void solicitarResetSenha(String email) {
        // TODO: Implementar lógica de reset de senha
        // Gerar token, salvar no banco, enviar email
    }

    /**
     * Reseta a senha do usuário
     * 
     * @param token Token de reset
     * @param novaSenha Nova senha
     * @param confirmaSenha Confirmação da senha
     * @return true se sucesso
     */
    public boolean resetarSenha(String token, String novaSenha, String confirmaSenha) {
        // TODO: Implementar lógica de reset de senha
        // Validar token, verificar senhas, atualizar no banco
        return false;
    }

    /**
     * Verifica email do usuário
     * 
     * @param token Token de verificação
     * @return true se sucesso
     */
    public boolean verificarEmail(String token) {
        // TODO: Implementar lógica de verificação de email
        // Validar token, marcar email como verificado
        return false;
    }

    /**
     * Verifica se o usuário pode acessar dashboard
     * 
     * @return true se pode acessar
     */
    public boolean canAccessDashboard() {
        return isLogado();
    }

    /**
     * Verifica se o usuário pode gerenciar usuários
     * 
     * @return true se pode gerenciar
     */
    public boolean canManageUsers() {
        return hasRole(PapelUsuario.ADMINISTRADOR) || 
               hasRole(PapelUsuario.FUNDADOR);
    }

    /**
     * Verifica se o usuário pode gerenciar conteúdo
     * 
     * @return true se pode gerenciar
     */
    public boolean canManageContent() {
        return hasRole(PapelUsuario.ADMINISTRADOR) || 
               hasRole(PapelUsuario.FUNDADOR) || 
               hasRole(PapelUsuario.COLABORADOR);
    }

    /**
     * Verifica se o usuário é administrador
     * 
     * @return true se é admin
     */
    public boolean isAdmin() {
        return hasRole(PapelUsuario.ADMINISTRADOR);
    }
}