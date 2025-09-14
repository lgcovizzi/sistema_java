package com.sistema.service;

import com.sistema.entity.RefreshToken;
import com.sistema.entity.Role;
import com.sistema.entity.User;
import com.sistema.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serviço de autenticação e gerenciamento de usuários.
 * Implementa UserDetailsService para integração com Spring Security.
 */
@Service
@Transactional
public class AuthService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private AuthenticationManager authenticationManager;

    @Autowired
    public AuthService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder, 
                      JwtService jwtService,
                      RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Autowired
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * Implementação do UserDetailsService para Spring Security.
     * 
     * @param username nome de usuário ou email
     * @return UserDetails do usuário
     * @throws UsernameNotFoundException se usuário não encontrado
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> {
                    logger.warn("Usuário não encontrado: {}", username);
                    return new UsernameNotFoundException("Usuário não encontrado: " + username);
                });
        
        logger.debug("Usuário carregado: {}", user.getUsername());
        return user;
    }

    /**
     * Autentica um usuário e gera tokens JWT.
     * 
     * @param usernameOrEmail nome de usuário ou email
     * @param password senha
     * @param request requisição HTTP para extrair informações do dispositivo
     * @return mapa com tokens e informações do usuário
     * @throws AuthenticationException se credenciais inválidas
     */
    public Map<String, Object> authenticate(String usernameOrEmail, String password, HttpServletRequest request) {
        try {
            // Autentica usando Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usernameOrEmail, password)
            );

            User user = (User) authentication.getPrincipal();
            
            // Atualiza último login
            updateLastLogin(user.getId());
            
            // Gera tokens
            String accessToken = jwtService.generateAccessToken(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, request);
            
            logger.info("Usuário autenticado com sucesso: {}", user.getUsername());
            
            return createAuthResponse(user, accessToken, refreshToken.getToken());
            
        } catch (AuthenticationException e) {
            logger.warn("Falha na autenticação para usuário: {}", usernameOrEmail);
            throw new BadCredentialsException("Credenciais inválidas");
        }
    }

    /**
     * Registra um novo usuário.
     * 
     * @param username nome de usuário
     * @param email email
     * @param password senha
     * @param firstName nome (opcional)
     * @param lastName sobrenome (opcional)
     * @return usuário criado
     * @throws IllegalArgumentException se dados inválidos
     */
    public User register(String username, String email, String password, 
                        String firstName, String lastName) {
        
        // Validações
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Nome de usuário já existe: " + username);
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email já está em uso: " + email);
        }
        
        // Cria novo usuário
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRoles(List.of(Role.USER));
        user.setEnabled(true);
        
        User savedUser = userRepository.save(user);
        logger.info("Novo usuário registrado: {}", savedUser.getUsername());
        
        return savedUser;
    }

    /**
     * Registra um novo usuário e retorna tokens de autenticação.
     * 
     * @param username nome de usuário
     * @param email email
     * @param password senha
     * @param firstName nome (opcional)
     * @param lastName sobrenome (opcional)
     * @param request requisição HTTP
     * @return mapa com tokens e informações do usuário
     */
    public Map<String, Object> registerAndAuthenticate(String username, String email, String password,
                                                       String firstName, String lastName, HttpServletRequest request) {
        User user = register(username, email, password, firstName, lastName);
        
        // Gera tokens para o usuário recém-criado
        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, request);
        
        return createAuthResponse(user, accessToken, refreshToken.getToken());
    }

    /**
     * Renova o token de acesso usando um refresh token válido.
     * 
     * @param refreshTokenValue token de refresh
     * @param request requisição HTTP
     * @return mapa com novo token de acesso
     * @throws IllegalArgumentException se refresh token inválido
     */
    public Map<String, Object> refreshAccessToken(String refreshTokenValue, HttpServletRequest request) {
        logger.debug("Tentativa de renovação de token");
        
        RefreshToken refreshToken = refreshTokenService.findValidRefreshToken(refreshTokenValue)
                .orElseThrow(() -> {
                    logger.warn("Refresh token inválido ou expirado");
                    return new IllegalArgumentException("Refresh token inválido ou expirado");
                });
        
        User user = refreshToken.getUser();
        
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Usuário desabilitado");
        }
        
        // Gera novo access token
        String newAccessToken = jwtService.generateAccessToken(user);
        
        // Opcionalmente, gera novo refresh token (rotação de tokens)
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user, request);
        
        // Revoga o refresh token antigo
        refreshTokenService.revokeRefreshToken(refreshTokenValue);
        
        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", newAccessToken);
        response.put("refreshToken", newRefreshToken.getToken());
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 3600); // 1 hora em segundos
        response.put("refreshExpiresIn", newRefreshToken.getDaysUntilExpiration() * 24 * 60 * 60); // em segundos
        
        logger.info("Token renovado com sucesso para usuário: {}", user.getUsername());
        
        return response;
    }

    /**
     * Altera a senha do usuário.
     * 
     * @param username nome do usuário
     * @param currentPassword senha atual
     * @param newPassword nova senha
     * @throws IllegalArgumentException se senha atual incorreta
     */
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Senha atual incorreta");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        logger.info("Senha alterada para usuário: {}", username);
    }

    /**
     * Ativa ou desativa um usuário.
     * 
     * @param userId ID do usuário
     * @param enabled true para ativar, false para desativar
     */
    public void setUserEnabled(Long userId, boolean enabled) {
        userRepository.updateUserStatus(userId, enabled);
        logger.info("Status do usuário {} alterado para: {}", userId, enabled ? "ativo" : "inativo");
    }

    /**
     * Busca usuário por ID.
     * 
     * @param userId ID do usuário
     * @return usuário encontrado
     */
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * Busca usuário por username.
     * 
     * @param username nome do usuário
     * @return usuário encontrado
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Lista todos os usuários ativos.
     * 
     * @return lista de usuários ativos
     */
    public List<User> findActiveUsers() {
        return userRepository.findByEnabledTrue();
    }

    /**
     * Busca usuários por termo de pesquisa.
     * 
     * @param searchTerm termo de busca
     * @return lista de usuários encontrados
     */
    public List<User> searchUsers(String searchTerm) {
        return userRepository.searchUsers(searchTerm);
    }

    /**
     * Atualiza o último login do usuário.
     * 
     * @param userId ID do usuário
     */
    private void updateLastLogin(Long userId) {
        userRepository.updateLastLogin(userId, LocalDateTime.now());
    }

    /**
     * Cria a resposta de autenticação com tokens e informações do usuário.
     * 
     * @param user usuário autenticado
     * @param accessToken token de acesso
     * @param refreshToken token de refresh
     * @return mapa com dados da resposta
     */
    private Map<String, Object> createAuthResponse(User user, String accessToken, String refreshToken) {
        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 3600); // 1 hora em segundos
        response.put("refreshExpiresIn", 15552000); // 6 meses em segundos
        
        // Informações do usuário
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("fullName", user.getFullName());
        userInfo.put("roles", user.getRoles());
        userInfo.put("lastLogin", user.getLastLogin());
        
        response.put("user", userInfo);
        
        return response;
    }

    /**
     * Valida se um token JWT é válido para um usuário específico.
     * 
     * @param token token JWT
     * @param username nome do usuário
     * @return true se válido
     */
    public boolean isTokenValidForUser(String token, String username) {
        try {
            User user = userRepository.findByUsername(username)
                    .orElse(null);
            
            if (user == null || !user.isEnabled()) {
                return false;
            }
            
            return jwtService.isTokenValid(token, user);
        } catch (Exception e) {
            logger.warn("Erro ao validar token para usuário {}: {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Realiza logout do usuário.
     *
     * @param refreshTokenValue Token de refresh para revogar
     * @param revokeAll Se deve revogar todos os tokens do usuário
     */
    public void logout(String refreshTokenValue, boolean revokeAll) {
        logger.debug("Realizando logout");
        
        if (refreshTokenValue != null) {
            Optional<RefreshToken> refreshToken = refreshTokenService.findValidRefreshToken(refreshTokenValue);
            
            if (refreshToken.isPresent()) {
                User user = refreshToken.get().getUser();
                
                if (revokeAll) {
                    // Revoga todos os tokens do usuário
                    int revokedCount = refreshTokenService.revokeAllUserTokens(user);
                    logger.info("Logout completo: {} tokens revogados para usuário: {}", revokedCount, user.getUsername());
                } else {
                    // Revoga apenas o token atual
                    refreshTokenService.revokeRefreshToken(refreshTokenValue);
                    logger.info("Logout realizado para usuário: {}", user.getUsername());
                }
            }
        }
    }

    /**
     * Obtém estatísticas dos usuários.
     * 
     * @return mapa com estatísticas
     */
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("activeUsers", userRepository.countByEnabledTrue());
        stats.put("adminUsers", userRepository.countByRole(Role.ADMIN));
        stats.put("regularUsers", userRepository.countByRole(Role.USER));
        
        return stats;
    }
}