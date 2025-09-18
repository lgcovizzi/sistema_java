package com.sistema.controller;

import com.sistema.dto.UpdateProfileRequest;
import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.service.AuthService;
import com.sistema.service.JwtService;
import com.sistema.service.TokenBlacklistService;
import com.sistema.service.AttemptService;
import com.sistema.service.CaptchaService;
import com.sistema.service.EmailVerificationService;
import com.sistema.validation.ValidCpf;
import com.sistema.util.EmailMaskUtil;
import com.sistema.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
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
    private final AttemptService attemptService;
    private final CaptchaService captchaService;
    private final EmailVerificationService emailVerificationService;

    @Autowired
    public AuthController(AuthService authService, JwtService jwtService, TokenBlacklistService tokenBlacklistService,
                         AttemptService attemptService, CaptchaService captchaService, 
                         EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.attemptService = attemptService;
        this.captchaService = captchaService;
        this.emailVerificationService = emailVerificationService;
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
        String clientIp = getClientIpAddress(httpRequest);
        String identifier = loginRequest.getEmail();
        
        try {
            // Verificar se captcha é necessário
            boolean requiresCaptcha = attemptService.isCaptchaRequiredForLogin(clientIp);
            
            if (requiresCaptcha) {
                // Validar captcha se fornecido
                if (loginRequest.getCaptchaId() == null || loginRequest.getCaptchaAnswer() == null) {
                    Map<String, Object> errorResponse = createErrorResponse(
                        "Captcha é obrigatório após múltiplas tentativas", 
                        "CAPTCHA_REQUIRED"
                    );
                    errorResponse.put("requiresCaptcha", true);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
                }
                
                if (!captchaService.validateCaptcha(loginRequest.getCaptchaId(), loginRequest.getCaptchaAnswer())) {
                    attemptService.recordLoginAttempt(clientIp);
                    Map<String, Object> errorResponse = createErrorResponse(
                        "Captcha inválido", 
                        "INVALID_CAPTCHA"
                    );
                    errorResponse.put("requiresCaptcha", true);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
                }
            }
            
            Map<String, Object> authResponse = authService.authenticate(
                    loginRequest.getEmail(),
                    loginRequest.getPassword(),
                    httpRequest
            );
            
            // Login bem-sucedido - limpar tentativas
            attemptService.clearLoginAttempts(clientIp);
            
            logger.info("Login realizado com sucesso para: {}", loginRequest.getEmail());
            return ResponseEntity.ok(authResponse);
            
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            // Registrar tentativa falhada
            attemptService.recordLoginAttempt(clientIp);
            
            logger.warn("Falha no login para: {} (IP: {}) - {}", loginRequest.getEmail(), clientIp, e.getMessage());
            
            Map<String, Object> errorResponse;
            HttpStatus status;
            
            // Verificar se é erro de email não verificado
            if (e.getMessage().contains("Email não verificado")) {
                errorResponse = createErrorResponse(e.getMessage(), "EMAIL_NOT_VERIFIED");
                errorResponse.put("success", false);
                status = HttpStatus.BAD_REQUEST;
            } else {
                errorResponse = createErrorResponse("Credenciais inválidas", "INVALID_CREDENTIALS");
                status = HttpStatus.UNAUTHORIZED;
            }
            
            // Verificar se captcha será necessário na próxima tentativa
            boolean willRequireCaptcha = attemptService.isCaptchaRequiredForLogin(clientIp);
            errorResponse.put("requiresCaptcha", willRequireCaptcha);
            
            return ResponseEntity.status(status).body(errorResponse);
        } catch (Exception e) {
            // Registrar tentativa falhada
            attemptService.recordLoginAttempt(clientIp);
            
            logger.warn("Falha no login para: {} (IP: {})", loginRequest.getEmail(), clientIp);
            
            Map<String, Object> errorResponse = createErrorResponse("Credenciais inválidas", "INVALID_CREDENTIALS");
            
            // Verificar se captcha será necessário na próxima tentativa
            boolean willRequireCaptcha = attemptService.isCaptchaRequiredForLogin(clientIp);
            errorResponse.put("requiresCaptcha", willRequireCaptcha);
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
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
                    registerRequest.getEmail(),
                    registerRequest.getPassword(),
                    registerRequest.getFirstName(),
                    registerRequest.getLastName(),
                    registerRequest.getCpf(),
                    httpRequest
            );
            
            logger.info("Usuário registrado com sucesso: {}", registerRequest.getEmail());
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
            String email = null;
            
            if (currentAccessToken != null) {
                try {
                    email = jwtService.extractSubject(currentAccessToken);
                    // Revoga o token de acesso atual
                    tokenBlacklistService.revokeToken(currentAccessToken);
                    logger.info("Token de acesso atual revogado para usuário: {}", email);
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
            if (revokeAll && email != null) {
                tokenBlacklistService.revokeAllUserTokens(email);
                logger.info("Todos os tokens do usuário {} foram revogados", email);
            }
            
            // Chama o logout do AuthService para limpeza adicional
            authService.logout(refreshToken, revokeAll);
            
            logger.info("Logout realizado com sucesso para usuário: {}", email);
            
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
            userInfo.put("username", user.getEmail());
            userInfo.put("email", user.getEmail());
            userInfo.put("fullName", user.getFullName());
            userInfo.put("role", user.getRole());
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
     * Endpoint para atualização do perfil do usuário autenticado.
     * 
     * @param updateProfileRequest dados de atualização do perfil
     * @return informações atualizadas do usuário
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest updateProfileRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();
            
            User updatedUser = authService.updateProfile(
                    currentUser.getId(),
                    updateProfileRequest.getFirstName(),
                    updateProfileRequest.getLastName(),
                    updateProfileRequest.getPhone()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Perfil atualizado com sucesso");
            response.put("user", createUserResponse(updatedUser));
            
            logger.info("Perfil atualizado com sucesso para usuário: {}", currentUser.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Erro na atualização do perfil: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage(), "PROFILE_UPDATE_ERROR"));
        } catch (Exception e) {
            logger.error("Erro interno na atualização do perfil", e);
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
     * Endpoint para solicitar recuperação de senha.
     * 
     * @param forgotPasswordRequest dados para recuperação de senha
     * @param httpRequest requisição HTTP
     * @return confirmação do envio
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest, 
                                          HttpServletRequest httpRequest) {
        String clientIp = getClientIpAddress(httpRequest);
        
        try {
            // Verificar rate limiting (1 tentativa por minuto)
            if (attemptService.isPasswordResetRateLimited(clientIp)) {
                long remainingSeconds = attemptService.getPasswordResetRateLimitRemainingSeconds(clientIp);
                Map<String, Object> errorResponse = createErrorResponse(
                    "Muitas tentativas de recuperação de senha. Tente novamente em " + remainingSeconds + " segundos.", 
                    "RATE_LIMITED"
                );
                errorResponse.put("remainingSeconds", remainingSeconds);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
            }
            
            // Captcha é sempre obrigatório para recuperação de senha
            if (!captchaService.validateCaptcha(forgotPasswordRequest.getCaptchaId(), forgotPasswordRequest.getCaptchaAnswer())) {
                attemptService.recordPasswordResetAttempt(clientIp);
                Map<String, Object> errorResponse = createErrorResponse(
                    "Captcha inválido", 
                    "INVALID_CAPTCHA"
                );
                errorResponse.put("requiresCaptcha", true);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            // Buscar usuário por CPF
            String cpf = forgotPasswordRequest.getCpf();
            Optional<User> userOpt = authService.findByCpf(cpf);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String email = user.getEmail();
                String maskedEmail = EmailMaskUtil.maskEmail(email);
                
                // TODO: Implementar envio de email com token de recuperação
                logger.info("Solicitação de recuperação de senha para CPF: {} (email: {})", cpf, email);
                
                // Limpar tentativas após sucesso
                attemptService.clearPasswordResetAttempts(clientIp);
                
                // Ativar rate limiting por 1 minuto
                attemptService.recordPasswordResetSuccess(clientIp);
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Instruções de recuperação foram enviadas para o email cadastrado");
                response.put("maskedEmail", maskedEmail);
                response.put("success", true);
                
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Tentativa de recuperação para CPF inexistente: {}", cpf);
                
                // Registrar tentativa falhada para CPF inexistente
                attemptService.recordPasswordResetAttempt(clientIp);
                
                Map<String, Object> errorResponse = createErrorResponse(
                    "CPF não encontrado no sistema", 
                    "CPF_NOT_FOUND"
                );
                errorResponse.put("requiresCaptcha", true);
                
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
        } catch (Exception e) {
            // Registrar tentativa falhada
            attemptService.recordPasswordResetAttempt(clientIp);
            
            logger.error("Erro na solicitação de recuperação de senha para CPF: {} (IP: {})", 
                        forgotPasswordRequest.getCpf(), clientIp, e);
            
            Map<String, Object> errorResponse = createErrorResponse("Erro interno do servidor", "INTERNAL_ERROR");
            
            // Verificar se captcha será necessário na próxima tentativa
            boolean willRequireCaptcha = attemptService.isCaptchaRequiredForPasswordReset(clientIp);
            errorResponse.put("requiresCaptcha", willRequireCaptcha);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
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
            Map<String, Object> statistics = authService.getUserStatisticsAsMap();
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
     * Endpoint administrativo para atualizar role de usuário.
     * 
     * @param userId ID do usuário
     * @param updateRoleRequest dados para atualização de role
     * @return confirmação da operação
     */
    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserRole(@PathVariable Long userId, 
                                           @Valid @RequestBody UpdateRoleRequest updateRoleRequest) {
        try {
            Optional<User> userOpt = authService.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Usuário não encontrado", "USER_NOT_FOUND"));
            }
            
            User updatedUser = authService.updateUserRole(userId, updateRoleRequest.getRole());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Role do usuário atualizado com sucesso");
            response.put("userId", userId);
            response.put("username", updatedUser.getEmail());
            response.put("email", updatedUser.getEmail());
            response.put("newRole", updatedUser.getRole());
            
            logger.info("Role do usuário {} (ID: {}) atualizado para {} por admin", 
                       updatedUser.getEmail(), userId, updateRoleRequest.getRole());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao alterar role do usuário", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erro interno do servidor", "INTERNAL_ERROR"));
        }
    }



    /**
     * Endpoint para reenvio de email de verificação.
     * 
     * @param resendRequest dados para reenvio
     * @return resposta de sucesso ou erro
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(@Valid @RequestBody ResendVerificationRequest resendRequest) {
        try {
            logger.info("Solicitação de reenvio de verificação para email: {}", resendRequest.getEmail());
            
            String token = emailVerificationService.regenerateVerificationToken(resendRequest.getEmail());
            
            if (token != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Email de verificação reenviado com sucesso!");
                response.put("sent", true);
                
                logger.info("Email de verificação reenviado para: {}", resendRequest.getEmail());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = createErrorResponse(
                    "Usuário não encontrado ou email já verificado", 
                    "USER_NOT_FOUND_OR_VERIFIED"
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
        } catch (Exception e) {
            logger.error("Erro ao reenviar email de verificação", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Erro interno do servidor", "INTERNAL_ERROR"));
        }
    }

    /**
     * Obtém o endereço IP real do cliente.
     * 
     * @param request requisição HTTP
     * @return endereço IP do cliente
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
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

    /**
     * Cria uma resposta padronizada com dados do usuário.
     * 
     * @param user usuário
     * @return mapa com dados do usuário
     */
    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("email", user.getEmail());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("fullName", user.getFullName());
        userInfo.put("phone", user.getPhone());
        userInfo.put("role", user.getRole());
        userInfo.put("enabled", user.isEnabled());
        userInfo.put("lastLogin", user.getLastLogin());
        userInfo.put("createdAt", user.getCreatedAt());
        userInfo.put("updatedAt", user.getUpdatedAt());
        return userInfo;
    }

    // Classes de Request DTOs
    
    public static class LoginRequest {
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ser válido")
        private String email;
        
        @NotBlank(message = "Password é obrigatório")
        private String password;
        
        // Campos de captcha (opcionais)
        private String captchaId;
        private String captchaAnswer;
        
        // Getters e Setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getCaptchaId() { return captchaId; }
        public void setCaptchaId(String captchaId) { this.captchaId = captchaId; }
        public String getCaptchaAnswer() { return captchaAnswer; }
        public void setCaptchaAnswer(String captchaAnswer) { this.captchaAnswer = captchaAnswer; }
    }
    
    public static class RegisterRequest {
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ser válido")
        private String email;
        
        @NotBlank(message = "Password é obrigatório")
        @Size(min = 8, message = "Password deve ter pelo menos 8 caracteres")
        private String password;
        
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 50, message = "Nome deve ter entre 2 e 50 caracteres")
        private String firstName;
        
        @NotBlank(message = "Sobrenome é obrigatório")
        @Size(min = 2, max = 50, message = "Sobrenome deve ter entre 2 e 50 caracteres")
        private String lastName;
        
        @NotBlank(message = "CPF é obrigatório")
        @ValidCpf(message = "CPF inválido")
        private String cpf;
        
        // Getters e Setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getCpf() { return cpf; }
        public void setCpf(String cpf) { this.cpf = cpf; }
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
        @Size(min = 8, message = "Nova senha deve ter pelo menos 8 caracteres")
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
    
    public static class UpdateRoleRequest {
        @NotNull(message = "Role é obrigatório")
        private UserRole role;
        
        // Getters e Setters
        public UserRole getRole() { return role; }
        public void setRole(UserRole role) { this.role = role; }
    }
    
    public static class ForgotPasswordRequest {
        @NotBlank(message = "CPF é obrigatório")
        @ValidCpf(message = "CPF inválido")
        private String cpf;
        
        // Campos de captcha (sempre obrigatórios para recuperação de senha)
        @NotBlank(message = "ID do captcha é obrigatório")
        private String captchaId;
        
        @NotBlank(message = "Resposta do captcha é obrigatória")
        private String captchaAnswer;
        
        // Getters e Setters
        public String getCpf() { return cpf; }
        public void setCpf(String cpf) { this.cpf = cpf; }
        public String getCaptchaId() { return captchaId; }
        public void setCaptchaId(String captchaId) { this.captchaId = captchaId; }
        public String getCaptchaAnswer() { return captchaAnswer; }
        public void setCaptchaAnswer(String captchaAnswer) { this.captchaAnswer = captchaAnswer; }
    }
    
    public static class ResendVerificationRequest {
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ser válido")
        private String email;
        
        // Getters e Setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}