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
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmailAndAtivoTrue(loginRequest.getEmail());
            if (usuarioOpt.isEmpty()) {
                logger.warn("Usuário não encontrado ou inativo: {}", loginRequest.getEmail());
                return LoginResponseDTO.erro("Credenciais inválidas", "Email ou senha incorretos");
            }

            Usuario usuario = usuarioOpt.get();

            // Verificar se a conta está ativa
            if (!usuario.isAtivo()) {
                logger.warn("Tentativa de login com conta inativa: {}", loginRequest.getEmail());
                return LoginResponseDTO.erro("Conta inativa", "Sua conta foi desativada. Entre em contato com o suporte.");
            }

            // Autenticar
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getSenha()
                )
            );

            // Gerar tokens
            String token = jwtUtil.generateToken(usuario);
            String refreshToken = jwtUtil.generateRefreshToken(usuario);

            // Atualizar último login
            usuario.setUltimoLogin(LocalDateTime.now());
            usuarioRepository.save(usuario);

            // Configurações do usuário
            Map<String, Object> configuracoes = criarConfiguracoes(usuario);

            // Enviar email de boas-vindas se for primeiro login
            if (usuario.getUltimoLogin() == null) {
                enviarEmailBoasVindasAsync(usuario);
            }

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

            // Validar refresh token
            if (!jwtUtil.isRefreshTokenValid(refreshToken)) {
                logger.warn("Refresh token inválido ou expirado");
                return LoginResponseDTO.erro("Token expirado", "Refresh token inválido ou expirado");
            }

            // Extrair usuário do token
            String email = jwtUtil.extractUsername(refreshToken);
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmailAndAtivoTrue(email);

            if (usuarioOpt.isEmpty()) {
                logger.warn("Usuário não encontrado para refresh token: {}", email);
                return LoginResponseDTO.erro("Usuário não encontrado", "Usuário não existe ou está inativo");
            }

            Usuario usuario = usuarioOpt.get();

            // Gerar novos tokens
            String novoToken = jwtUtil.generateToken(usuario);
            String novoRefreshToken = jwtUtil.generateRefreshToken(usuario);

            Map<String, Object> configuracoes = criarConfiguracoes(usuario);

            logger.info("Token atualizado com sucesso para usuário: {}", usuario.getEmail());

            return LoginResponseDTO.builder()
                .sucesso(true)
                .mensagem("Token atualizado com sucesso")
                .token(novoToken)
                .refreshToken(novoRefreshToken)
                .expiracaoToken(LocalDateTime.now().plusHours(24))
                .usuario(usuario)
                .configuracoes(configuracoes)
                .build();

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

            return jwtUtil.isTokenValid(token);
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
    public boolean emailDisponivel(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return !usuarioRepository.existsByEmail(email.trim().toLowerCase());
    }

    /**
     * Verifica se um CPF está disponível
     * Referência: Login e Registro - project_rules.md
     * 
     * @param cpf CPF a verificar
     * @return true se está disponível
     */
    public boolean cpfDisponivel(String cpf) {
        if (cpf == null || cpf.trim().isEmpty()) {
            return false;
        }
        String cpfLimpo = cpf.replaceAll("[^0-9]", "");
        return !usuarioRepository.existsByCpf(cpfLimpo);
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
        usuario.setEmailVerificado(false);
        usuario.setReceberNewsletter(registroRequest.isReceberNewsletter());
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
        configuracoes.put("receberNotificacoes", usuario.isReceberNewsletter());
        configuracoes.put("primeiroLogin", usuario.getUltimoLogin() == null);
        
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
                emailService.enviarEmailBoasVindas(
                    usuario.getEmail(),
                    usuario.getNome(),
                    usuario.getSobrenome()
                );
                
                logger.info("Email de boas-vindas enviado para: {}", usuario.getEmail());
            } catch (Exception e) {
                logger.error("Erro ao enviar email de boas-vindas para: {}", usuario.getEmail(), e);
            }
        });
    }
}