package com.sistema.service.base;

import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Classe base para serviços relacionados a usuários.
 * Fornece operações comuns de CRUD e validação de usuários.
 */
public abstract class BaseUserService extends BaseService {
    
    @Autowired
    protected UserRepository userRepository;
    
    @Autowired
    protected PasswordEncoder passwordEncoder;
    
    /**
     * Busca usuário por ID com validação.
     * 
     * @param userId ID do usuário
     * @return usuário encontrado
     * @throws IllegalArgumentException se ID inválido
     * @throws RuntimeException se usuário não encontrado
     */
    protected User findUserByIdRequired(Long userId) {
        validateId(userId, "userId");
        
        Optional<User> userOpt = userRepository.findById(userId);
        logSearchOperation("Usuário", userId, userOpt);
        
        return userOpt.orElseThrow(() -> {
            String message = String.format("Usuário não encontrado com ID: %d", userId);
            logger.warn(message);
            return new RuntimeException(message);
        });
    }
    
    /**
     * Busca usuário por email com validação.
     * 
     * @param email email do usuário
     * @return usuário encontrado
     * @throws IllegalArgumentException se email inválido
     * @throws RuntimeException se usuário não encontrado
     */
    protected User findUserByEmailRequired(String email) {
        validateNotEmpty(email, "email");
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        logSearchOperation("Usuário por email", email, userOpt);
        
        return userOpt.orElseThrow(() -> {
            String message = String.format("Usuário não encontrado com email: %s", email);
            logger.warn(message);
            return new RuntimeException(message);
        });
    }
    

    
    /**
     * Busca usuário por email.
     * 
     * @param email email do usuário
     * @return usuário encontrado ou Optional.empty()
     */
    protected Optional<User> findUserByEmail(String email) {
        validateNotEmpty(email, "email");
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        logSearchOperation("Usuário por email", email, userOpt);
        
        return userOpt;
    }
    
    /**
     * Verifica se email já está em uso.
     * 
     * @param email email a verificar
     * @param excludeUserId ID do usuário a excluir da verificação (para updates)
     * @throws IllegalArgumentException se email já está em uso
     */
    protected void validateEmailNotInUse(String email, Long excludeUserId) {
        validateNotEmpty(email, "email");
        
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(excludeUserId)) {
            String message = String.format("Email já está em uso: %s", email);
            logger.warn(message);
            throw new IllegalArgumentException(message);
        }
    }
    

    
    /**
     * Valida força da senha.
     * 
     * @param password senha a validar
     * @throws IllegalArgumentException se senha não atende critérios
     */
    protected void validatePasswordStrength(String password) {
        validateNotEmpty(password, "password");
        
        if (password.length() < 8) {
            throw new IllegalArgumentException("Senha deve ter pelo menos 8 caracteres");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Senha deve conter pelo menos uma letra maiúscula");
        }
        
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Senha deve conter pelo menos uma letra minúscula");
        }
        
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Senha deve conter pelo menos um número");
        }
        
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new IllegalArgumentException("Senha deve conter pelo menos um caractere especial");
        }
    }
    
    /**
     * Codifica uma senha.
     * 
     * @param rawPassword senha em texto plano
     * @return senha codificada
     */
    protected String encodePassword(String rawPassword) {
        validateNotEmpty(rawPassword, "rawPassword");
        return passwordEncoder.encode(rawPassword);
    }
    
    /**
     * Verifica se senha corresponde ao hash.
     * 
     * @param rawPassword senha em texto plano
     * @param encodedPassword senha codificada
     * @return true se senhas correspondem
     */
    protected boolean matchesPassword(String rawPassword, String encodedPassword) {
        validateNotEmpty(rawPassword, "rawPassword");
        validateNotEmpty(encodedPassword, "encodedPassword");
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    /**
     * Atualiza último login do usuário.
     * 
     * @param userId ID do usuário
     */
    protected void updateLastLogin(Long userId) {
        executeWithErrorHandling(
            () -> {
                User user = findUserByIdRequired(userId);
                user.setLastLoginAt(getCurrentTimestamp());
                userRepository.save(user);
                logUpdateOperation("Último login do usuário", userId);
            },
            formatErrorMessage("atualizar último login", "userId: " + userId)
        );
    }
    
    /**
     * Verifica se usuário está ativo.
     * 
     * @param user usuário a verificar
     * @throws RuntimeException se usuário inativo
     */
    protected void validateUserActive(User user) {
        validateNotNull(user, "user");
        
        if (!user.isEnabled()) {
            String message = String.format("Usuário está inativo: %s", user.getUsername());
            logger.warn(message);
            throw new RuntimeException(message);
        }
    }
    
    /**
     * Verifica se usuário tem role específica.
     * 
     * @param user usuário a verificar
     * @param requiredRole role necessária
     * @throws RuntimeException se usuário não tem a role
     */
    protected void validateUserRole(User user, UserRole requiredRole) {
        validateNotNull(user, "user");
        validateNotNull(requiredRole, "requiredRole");
        
        if (!user.getRole().equals(requiredRole)) {
            String message = String.format("Usuário não tem permissão necessária. Requerida: %s, Atual: %s", 
                                          requiredRole, user.getRole());
            logger.warn(message);
            throw new RuntimeException(message);
        }
    }
    
    /**
     * Obtém estatísticas básicas de usuários.
     * 
     * @return mapa com estatísticas
     */
    protected UserStatistics getUserStatistics() {
        return executeWithErrorHandling(
            () -> {
                long totalUsers = userRepository.count();
                long activeUsers = userRepository.countByEnabledTrue();
                long adminUsers = userRepository.countByRole(UserRole.ADMIN);
                long regularUsers = userRepository.countByRole(UserRole.USER);
                
                return new UserStatistics(totalUsers, activeUsers, adminUsers, regularUsers);
            },
            "obter estatísticas de usuários"
        );
    }
    
    /**
     * Classe para estatísticas de usuários.
     */
    public static class UserStatistics {
        private final long totalUsers;
        private final long activeUsers;
        private final long adminUsers;
        private final long regularUsers;
        
        public UserStatistics(long totalUsers, long activeUsers, long adminUsers, long regularUsers) {
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
            this.adminUsers = adminUsers;
            this.regularUsers = regularUsers;
        }
        
        // Getters
        public long getTotalUsers() { return totalUsers; }
        public long getActiveUsers() { return activeUsers; }
        public long getAdminUsers() { return adminUsers; }
        public long getRegularUsers() { return regularUsers; }
        public long getInactiveUsers() { return totalUsers - activeUsers; }
        
        public double getActiveUserPercentage() {
            return totalUsers > 0 ? (double) activeUsers / totalUsers * 100 : 0;
        }
    }
}