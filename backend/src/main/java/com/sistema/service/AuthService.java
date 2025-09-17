package com.sistema.service;

import com.sistema.entity.RefreshToken;
import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.service.base.BaseUserService;
import com.sistema.util.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
 * Estende BaseUserService para reutilizar lógica comum.
 */
@Service
@Transactional
public class AuthService extends BaseUserService implements UserDetailsService {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;
    private AuthenticationManager authenticationManager;

    @Autowired
    public AuthService(JwtService jwtService, RefreshTokenService refreshTokenService, 
                      EmailVerificationService emailVerificationService) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.emailVerificationService = emailVerificationService;
    }

    @Autowired
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * Implementação do UserDetailsService para Spring Security.
     * 
     * @param email email do usuário
     * @return UserDetails do usuário
     * @throws UsernameNotFoundException se usuário não encontrado
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            Optional<User> userOpt = findUserByEmail(email);
            if (userOpt.isEmpty()) {
                throw new UsernameNotFoundException("Usuário não encontrado: " + email);
            }
            
            User user = userOpt.get();
            logger.debug("Usuário carregado: {}", user.getEmail());
            return user;
        } catch (Exception e) {
            logger.warn("Erro ao carregar usuário: {}", email, e);
            throw new UsernameNotFoundException("Usuário não encontrado: " + email);
        }
    }

    /**
     * Busca usuário por email.
     * 
     * @param email email do usuário
     * @return usuário encontrado ou Optional.empty()
     */
    protected Optional<User> findUserByEmail(String email) {
        validateNotEmpty(email, "email");
        return userRepository.findByEmail(email);
    }

    /**
     * Autentica um usuário com email e senha.
     * 
     * @param email email do usuário
     * @param password senha do usuário
     * @return mapa com tokens JWT
     * @throws AuthenticationException se credenciais inválidas
     */
    public Map<String, Object> authenticate(String email, String password) throws AuthenticationException {
        try {
            logger.info("Tentativa de autenticação para email: {}", email);
            
            // Validar entrada
            ValidationUtils.validateNotBlank(email, "Email é obrigatório");
            ValidationUtils.validateNotBlank(password, "Senha é obrigatória");
            ValidationUtils.validateEmail(email);
            
            // Buscar usuário por email
            User user = findUserByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
            
            // Verificar se usuário está habilitado
            if (!user.isEnabled()) {
                logger.warn("Tentativa de login com usuário desabilitado: {}", email);
                throw new BadCredentialsException("Usuário desabilitado");
            }
            
            // Verificar se email foi verificado
            if (!user.isEmailVerified()) {
                logger.warn("Tentativa de login com email não verificado: {}", email);
                throw new BadCredentialsException("Email não verificado. Verifique sua caixa de entrada e clique no link de verificação.");
            }
            
            // Autenticar com Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            
            // Atualizar último login usando método da classe base
            updateLastLogin(user.getId());
            
            // Gerar tokens
            String accessToken = jwtService.generateAccessToken(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken.getToken());
            response.put("tokenType", "Bearer");
            response.put("expiresIn", jwtService.getAccessTokenExpiration());
            response.put("user", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "role", user.getRole().name()
            ));
            
            logger.info("Autenticação bem-sucedida para usuário: {}", email);
            return response;
            
        } catch (AuthenticationException e) {
            logger.warn("Falha na autenticação para email: {} - {}", email, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Erro inesperado durante autenticação para email: {}", email, e);
            throw new BadCredentialsException("Erro interno do servidor");
        }
    }

    /**
     * Autentica um usuário e gera tokens JWT.
     * 
     * @param email email do usuário
     * @param password senha
     * @param request requisição HTTP para extrair informações do dispositivo
     * @return mapa com tokens e informações do usuário
     * @throws AuthenticationException se credenciais inválidas
     */
    public Map<String, Object> authenticate(String email, String password, HttpServletRequest request) {
        try {
            // Autentica usando Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            User user = (User) authentication.getPrincipal();
            
            // Atualiza último login
            updateLastLogin(user.getId());
            
            // Gera tokens
            String accessToken = jwtService.generateAccessToken(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, request);
            
            logger.info("Usuário autenticado com sucesso: {}", user.getEmail());
            
            return createAuthResponse(user, accessToken, refreshToken.getToken());
            
        } catch (AuthenticationException e) {
            logger.warn("Falha na autenticação para usuário: {}", email);
            throw new BadCredentialsException("Credenciais inválidas");
        }
    }

    /**
     * Registra um novo usuário no sistema.
     * 
     * @param email email do usuário
     * @param password senha do usuário
     * @param firstName primeiro nome
     * @param lastName último nome
     * @param cpf CPF do usuário
     * @return usuário criado
     * @throws RuntimeException se email ou CPF já existirem
     */
    public User register(String email, String password, String firstName, String lastName, String cpf) {
        // Validar entrada usando utilitários
        ValidationUtils.validateNotBlank(email, "Email é obrigatório");
        ValidationUtils.validateNotBlank(password, "Senha é obrigatória");
        ValidationUtils.validateNotBlank(firstName, "Primeiro nome é obrigatório");
        ValidationUtils.validateNotBlank(lastName, "Último nome é obrigatório");
        ValidationUtils.validateNotBlank(cpf, "CPF é obrigatório");
        
        ValidationUtils.validateEmail(email);
        ValidationUtils.validatePassword(password);
        
        // Verificar se dados já existem usando métodos da classe base
        validateEmailNotInUse(email, null);
        
        // Verificar se CPF já existe
        if (userRepository.existsByCpf(cpf)) {
            throw new RuntimeException("CPF já está em uso");
        }
        
        // Criar novo usuário
        User user = new User();
        user.setEmail(email);
        user.setPassword(encodePassword(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCpf(cpf);
        
        // Verificar se é o email especial para auto-promoção a admin
        if ("lgcovizzi@gmail.com".equalsIgnoreCase(email)) {
            user.setRole(UserRole.ADMIN);
            user.setEmailVerified(true); // Admin é verificado automaticamente
            logger.info("Usuário {} promovido automaticamente para ADMIN devido ao email especial", email);
        } else {
            user.setRole(UserRole.USER);
            user.setEmailVerified(false); // Usuários normais precisam verificar email
        }
        
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        
        // Gerar token de verificação apenas para usuários não verificados
        if (!savedUser.isEmailVerified()) {
            try {
                String verificationToken = emailVerificationService.generateVerificationToken(savedUser);
                logger.info("Token de verificação gerado para: {}", email);
            } catch (Exception e) {
                logger.error("Erro ao gerar token de verificação para: {}", email, e);
                // Não falha o registro se o token não puder ser gerado
            }
        }
        
        logger.info("Usuário registrado com sucesso: {}", email);
        
        return savedUser;
    }
    
    /**
     * Registra um novo usuário e o autentica automaticamente.
     * 
     * @param email email do usuário
     * @param password senha do usuário
     * @param firstName primeiro nome
     * @param lastName último nome
     * @param cpf CPF do usuário
     * @param request requisição HTTP
     * @return tokens de autenticação
     * @throws RuntimeException se email ou CPF já existirem
     */
    public Map<String, Object> registerAndAuthenticate(String email, String password, 
                                                       String firstName, String lastName, String cpf, HttpServletRequest request) {
        // Registrar o usuário
        User user = register(email, password, firstName, lastName, cpf);
        
        // Autenticar automaticamente
        return authenticate(email, password, request);
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
        
        logger.info("Token renovado com sucesso para usuário: {}", user.getEmail());
        
        return response;
    }

    /**
     * Altera a senha do usuário.
     * 
     * @param email email do usuário
     * @param currentPassword senha atual
     * @param newPassword nova senha
     * @throws IllegalArgumentException se senha atual incorreta
     */
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Senha atual incorreta");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        logger.info("Senha alterada para usuário: {}", email);
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
        userInfo.put("email", user.getEmail());
        userInfo.put("fullName", user.getFullName());
        userInfo.put("role", user.getRole());
        userInfo.put("lastLogin", user.getLastLogin());
        
        response.put("user", userInfo);
        
        return response;
    }

    /**
     * Valida se um token JWT é válido para um usuário específico.
     * 
     * @param token token JWT
     * @param email email do usuário
     * @return true se válido
     */
    public boolean isTokenValidForUser(String token, String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElse(null);
            
            if (user == null || !user.isEnabled()) {
                return false;
            }
            
            return jwtService.isTokenValid(token, user);
        } catch (Exception e) {
            logger.warn("Erro ao validar token para usuário {}: {}", email, e.getMessage());
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
                    logger.info("Logout completo: {} tokens revogados para usuário: {}", revokedCount, user.getEmail());
                } else {
                    // Revoga apenas o token atual
                    refreshTokenService.revokeRefreshToken(refreshTokenValue);
                    logger.info("Logout realizado para usuário: {}", user.getEmail());
                }
            }
        }
    }

    /**
     * Atualiza o perfil do usuário.
     * 
     * @param userId ID do usuário
     * @param firstName novo nome
     * @param lastName novo sobrenome
     * @param phone novo telefone (opcional)
     * @return usuário atualizado
     */
    public User updateProfile(Long userId, String firstName, String lastName, String phone) {
        logInfo("Atualizando perfil do usuário");
        
        // Validações
        validateNotNull(userId, "ID do usuário");
        validateNotEmpty(firstName, "Nome");
        validateNotEmpty(lastName, "Sobrenome");
        
        // Validação de telefone se fornecido
        if (phone != null && !phone.trim().isEmpty()) {
            if (!ValidationUtils.isValidPhone(phone)) {
                throw new IllegalArgumentException("Formato de telefone inválido");
            }
        }
        
        User user = findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        
        // Atualiza os campos
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setPhone(phone != null && !phone.trim().isEmpty() ? phone.trim() : null);
        user.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        
        logInfo("Perfil do usuário atualizado com sucesso");
        
        return savedUser;
    }

    /**
     * Atualiza role do usuário.
     * 
     * @param userId ID do usuário
     * @param newRole nova role
     * @return usuário atualizado
     */
    public User updateUserRole(Long userId, UserRole newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        
        UserRole oldRole = user.getRole();
        user.setRole(newRole);
        user.setUpdatedAt(LocalDateTime.now());
        
        User updatedUser = userRepository.save(user);
        
        logger.info("Role do usuário {} (ID: {}) alterado de {} para {}", 
                   user.getEmail(), userId, oldRole, newRole);
        
        return updatedUser;
    }



    /**
     * Obtém estatísticas dos usuários.
     * 
     * @return objeto UserStatistics com estatísticas
     */
    @Override
    public UserStatistics getUserStatistics() {
        UserStatistics stats = super.getUserStatistics();
        
        logInfo("Estatísticas de usuários consultadas");
        
        return stats;
    }
    
    /**
     * Retorna estatísticas de usuários como Map para uso em controllers.
     * 
     * @return mapa com estatísticas dos usuários
     */
    public Map<String, Object> getUserStatisticsAsMap() {
        UserStatistics stats = getUserStatistics();
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", stats.getTotalUsers());
        result.put("activeUsers", stats.getActiveUsers());
        result.put("adminUsers", stats.getAdminUsers());
        result.put("regularUsers", stats.getRegularUsers());
        result.put("activeUserPercentage", stats.getActiveUserPercentage());
        
        return result;
    }

    /**
     * Busca usuário por CPF.
     * 
     * @param cpf CPF do usuário
     * @return Optional com o usuário encontrado
     */
    public Optional<User> findByCpf(String cpf) {
        try {
            ValidationUtils.validateNotBlank(cpf, "CPF é obrigatório");
            return userRepository.findByCpf(cpf);
        } catch (Exception e) {
            logError("Erro ao buscar usuário por CPF: " + cpf, e);
            return Optional.empty();
        }
    }

    /**
     * Busca usuários por termo de pesquisa.
     * 
     * @param searchTerm termo de pesquisa
     * @return lista de usuários encontrados
     */
    public List<User> searchUsers(String searchTerm) {
        try {
            ValidationUtils.validateNotBlank(searchTerm, "Termo de pesquisa é obrigatório");
            return userRepository.findByEmailContainingIgnoreCase(searchTerm);
        } catch (Exception e) {
            logError("Erro ao pesquisar usuários com termo: " + searchTerm, e);
            return List.of();
        }
    }

    /**
     * Busca todos os usuários ativos.
     * 
     * @return lista de usuários ativos
     */
    public List<User> findActiveUsers() {
        try {
            return userRepository.findByEnabledTrue();
        } catch (Exception e) {
            logError("Erro ao buscar usuários ativos", e);
            return List.of();
        }
    }

    /**
     * Busca usuário por ID.
     * 
     * @param id ID do usuário
     * @return Optional com o usuário encontrado
     */
    public Optional<User> findById(Long id) {
        try {
            ValidationUtils.validateNotNull(id, "ID do usuário é obrigatório");
            return userRepository.findById(id);
        } catch (Exception e) {
            logError("Erro ao buscar usuário por ID: " + id, e);
            return Optional.empty();
        }
    }

}