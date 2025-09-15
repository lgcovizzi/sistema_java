package com.sistema.controller;

import com.sistema.entity.User;
import com.sistema.service.AuthService;
import com.sistema.service.JwtService;
import com.sistema.service.TokenBlacklistService;
import com.sistema.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador REST para operações de autenticação e autorização.
 * Fornece endpoints para login, registro, refresh de tokens e gerenciamento de usuários.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Autowired
    public AuthController(AuthService authService, JwtService jwtService, TokenBlacklistService tokenBlacklistService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * Endpoint para autenticação de usuários.
     * 
     * @param loginRequest dados de login
     * @param httpRequest requisição HTTP
     * @return tokens JWT e informações do usuário
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest httpRequest) {
        try {
            Map<String, Object> authResponse = authService.authenticate(
                    loginRequest.getUsernameOrEmail(),
                    loginRequest.getPassword(),
                    httpRequest
            );
            
            logger.info("Login realizado com sucesso para: {}", loginRequest.getUsernameOrEmail());
            return ResponseEntity.ok(authResponse);
            
        } catch (Exception e) {
            logger.warn("Falha no login para: {}", loginRequest.getUsernameOrEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Credenciais inválidas", "INVALID_CREDENTIALS"));
        }
    }

    /**
     * Endpoint para registro de novos usuários.
     * 
     * @param registerRequest dados de registro
     * @param httpRequest requisição HTTP
     * @return tokens JWT e informações do usuário criado
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest, HttpServletRequest httpRequest) {
        try {
            Map<String, Object> authResponse = authService.registerAndAuthenticate(
                    registerRequest.getUsername(),
                    registerRequest.getEmail(),
                    registerRequest.getPassword(),
                    registerRequest.getFirstName(),
                    registerRequest.getLastName(),
                    httpRequest
            );
            
            logger.info("Usuário registrado com sucesso: {}", registerRequest.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Erro no registro: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage(), "REGISTRATION_ERROR"));
        } catch (Exception e) {
            logger.error("Erro interno no registro", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erro interno do servidor", "INTERNAL_ERROR"));
        }
    }

    /**
     * Endpoint para renovação de token de acesso.
     * 
     * @param refreshRequest dados do refresh token
     * @param httpRequest requisição HTTP
     * @return novo token de acesso
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshRequest, HttpServletRequest httpRequest) {
        try {
            Map<String, Object> refreshResponse = authService.refreshAccessToken(
                    refreshRequest.getRefreshToken(),
                    httpRequest
            );
            
            return ResponseEntity.ok(refreshResponse);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Refresh token inválido: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Refresh token inválido", "INVALID_REFRESH_TOKEN"));
        } catch (Exception e) {
            logger.error("Erro no refresh do token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erro interno do servidor", "INTERNAL_ERROR"));
        }
    }

    /**
     * Endpoint para logout (invalidação de token).
     * 
     * @param logoutRequest dados do logout
     * @param httpRequest requisição HTTP para extrair token atual
     * @return confirmação de logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) LogoutRequest logoutRequest, HttpServletRequest httpRequest) {
        try {
            String refreshToken = logoutRequest != null ? logoutRequest.getRefreshToken() : null;
            boolean revokeAll = logoutRequest != null ? logoutRequest.isRevokeAll() : false;
            
            // Extrai o token de acesso atual da requisição
            String currentAccessToken = extractTokenFromRequest(httpRequest);
            String username = null;
            
            if (currentAccessToken != null) {
                try {
                    username = jwtService.extractUsername(currentAccessToken);
                    // Revoga o token de acesso atual
                    tokenBlacklistService.revokeToken(currentAccessToken);
                    logger.info("Token de acesso atual revogado para usuário: {}", username);
                } catch (Exception e) {
                    logger.warn("Erro ao revogar token de acesso atual: {}", e.getMessage());
                }
            }
            
            // Revoga o refresh token se fornecido
            if (refreshToken != null && !refreshToken.trim().isEmpty()) {
                try {
                    if (jwtService.isValidRefreshToken(refreshToken)) {
                        tokenBlacklistService.revokeToken(refreshToken);
                        logger.info("Refresh token revogado");
                    }
                } catch (Exception e) {
                    logger.warn("Erro ao revogar refresh token: {}", e.getMessage());
                }
            }
            
            // Se solicitado, revoga todos os tokens do usuário
            if (revokeAll && username != null) {
                tokenBlacklistService.revokeAllUserTokens(username);
                logger.info("Todos os tokens do usuário {} foram revogados", username);
            }
            
            // Chama o logout do AuthService para limpeza adicional
            authService.logout(refreshToken, revokeAll);
            
            logger.info("Logout realizado com sucesso para usuário: {}", username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Logout realizado com sucesso");
            response.put("timestamp", System.currentTimeMillis());
            response.put("tokensRevoked", true);
            if (revokeAll) {
                response.put("allTokensRevoked", true);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro durante logout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erro interno do servidor", "INTERNAL_ERROR"));
        }
    }

    /**
     * Endpoint para obter informações do usuário autenticado.
     * 
     * @return informações do usuário
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("fullName", user.getFullName());
            userInfo.put("roles", user.getRoles());
            userInfo.put("lastLogin", user.getLastLogin());
            userInfo.put("createdAt", user.getCreatedAt());
            
            return ResponseEntity.ok(userInfo);
            
        } catch (Exception e) {
            logger.error("Erro ao obter informações do usuário", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erro interno do servidor", "INTERNAL_ERROR"));
        }
    }

    /**
     * Endpoint para alteração de senha.
     * 
     * @param changePasswordRequest dados para alteração de senha
     * @return confirmação da alteração
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            authService.changePassword(
                    username,
                    changePasswordRequest.getCurrentPassword(),
                    changePasswordRequest.getNewPassword()
            );
            
            logger.info("Senha alterada para usuário: {}", username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Senha alterada com sucesso");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage(), "INVALID_PASSWORD"));
        } catch (Exception e) {
            logger.error("Erro ao alterar senha", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erro interno do servidor", "INTERNAL_ERROR"));
        }
    }

    /**
     * Endpoint para validação de token.
     * 
     * @param validateTokenRequest dados do token
     * @return informações sobre a validade do token
     */
    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@Valid @RequestBody ValidateTokenRequest validateTokenRequest) {
        try {
            String token = validateTokenRequest.getToken();
            
            Map<String, Object> tokenInfo = jwtService.getTokenInfo(token);
            boolean isValid = jwtService.isValidAccessToken(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);
            response.put("tokenInfo", tokenInfo);
            
            if (isValid) {
                response.put("timeToExpiration", jwtService.getTimeToExpiration(token));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Endpoint administrativo para listar usuários.
     * 
     * @return lista de usuários
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUsers(@RequestParam(required = false) String search) {
        try {
            List<User> users;
            
            if (search != null && !search.trim().isEmpty()) {
                users = authService.searchUsers(search.trim());
            } else {
                users = authService.findActiveUsers();
            }
            
            return ResponseEntity.ok(users);
            
        } catch (Exception e) {
            logger.error("Erro ao listar usuários", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erro interno do servidor", "INTERNAL_ERROR"));
        }
    }

    /**
     * Endpoint administrativo para obter estatísticas de usuários.
     * 
     * @return estatísticas dos usuários
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserStatistics() {
        try {
            Map<String, Object> statistics = authService.getUserStatistics();
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Erro ao obter estatísticas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erro interno do servidor", "INTERNAL_ERROR"));
        }
    }

    /**
     * Endpoint administrativo para ativar/desativar usuário.
     * 
     * @param userId ID do usuário
     * @param enableUserRequest dados para ativação/desativação
     * @return confirmação da operação
     */
    @PutMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> setUserStatus(@PathVariable Long userId, 
                                          @Valid @RequestBody EnableUserRequest enableUserRequest) {
        try {
            Optional<User> userOpt = authService.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Usuário não encontrado", "USER_NOT_FOUND"));
            }
            
            authService.setUserEnabled(userId, enableUserRequest.isEnabled());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Status do usuário atualizado com sucesso");
            response.put("userId", userId);
            response.put("enabled", enableUserRequest.isEnabled());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao alterar status do usuário", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erro interno do servidor", "INTERNAL_ERROR"));
        }
    }

    /**
     * Extrai o token JWT do cabeçalho Authorization da requisição.
     * 
     * @param request requisição HTTP
     * @return token JWT ou null se não encontrado
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Cria uma resposta de erro padronizada.
     * 
     * @param message mensagem de erro
     * @param errorCode código do erro
     * @return mapa com dados do erro
     */
    private Map<String, Object> createErrorResponse(String message, String errorCode) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("errorCode", errorCode);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }

    // Classes de Request DTOs
    
    public static class LoginRequest {
        @NotBlank(message = "Username ou email é obrigatório")
        private String usernameOrEmail;
        
        @NotBlank(message = "Password é obrigatório")
        private String password;
        
        // Getters e Setters
        public String getUsernameOrEmail() { return usernameOrEmail; }
        public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    public static class RegisterRequest {
        @NotBlank(message = "Username é obrigatório")
        @Size(min = 3, max = 50, message = "Username deve ter entre 3 e 50 caracteres")
        private String username;
        
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ser válido")
        private String email;
        
        @NotBlank(message = "Password é obrigatório")
        @Size(min = 6, message = "Password deve ter pelo menos 6 caracteres")
        private String password;
        
        private String firstName;
        private String lastName;
        
        // Getters e Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
    }
    
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token é obrigatório")
        private String refreshToken;
        
        // Getters e Setters
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }
    
    public static class ChangePasswordRequest {
        @NotBlank(message = "Senha atual é obrigatória")
        private String currentPassword;
        
        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 6, message = "Nova senha deve ter pelo menos 6 caracteres")
        private String newPassword;
        
        // Getters e Setters
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
    
    public static class ValidateTokenRequest {
        @NotBlank(message = "Token é obrigatório")
        private String token;
        
        // Getters e Setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
    
    public static class LogoutRequest {
        private String refreshToken;
        private boolean revokeAll = false;
        
        // Getters e Setters
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public boolean isRevokeAll() { return revokeAll; }
        public void setRevokeAll(boolean revokeAll) { this.revokeAll = revokeAll; }
    }
    
    public static class EnableUserRequest {
        private boolean enabled;
        
        // Getters e Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}