package com.sistema.service;

import com.sistema.entity.User;
import com.sistema.repository.UserRepository;
import com.sistema.service.base.BaseService;
import com.sistema.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Serviço responsável pelo gerenciamento de tokens de reset de senha.
 * Estende BaseService para aproveitar funcionalidades comuns de logging e validação.
 */
@Service
@Transactional
public class PasswordResetService extends BaseService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.password-reset.token.expiration.hours:2}")
    private int tokenExpirationHours;

    @Autowired
    public PasswordResetService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Gera um token de reset de senha para o usuário.
     * 
     * @param user O usuário para o qual gerar o token
     * @return O token gerado
     */
    public String generatePasswordResetToken(User user) {
        validateNotNull(user, "Usuário não pode ser nulo");
        
        try {
            // Gera um token seguro
            String token = SecurityUtils.generateSecureToken();
            
            // Define a expiração do token
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenExpirationHours);
            
            // Atualiza o usuário com o token
            user.setResetPasswordToken(token);
            user.setResetPasswordTokenExpiresAt(expiresAt);
            
            // Salva no banco
            userRepository.save(user);
            
            logInfo("Token de reset de senha gerado para usuário: " + user.getEmail());
            return token;
            
        } catch (Exception e) {
            logError("Erro ao gerar token de reset de senha para usuário: " + user.getEmail(), e);
            throw new RuntimeException("Erro interno ao gerar token de reset", e);
        }
    }

    /**
     * Valida um token de reset de senha.
     * 
     * @param token O token a ser validado
     * @return Optional contendo o usuário se o token for válido, vazio caso contrário
     */
    @Transactional(readOnly = true)
    public Optional<User> validatePasswordResetToken(String token) {
        validateNotEmpty(token, "token");
        
        try {
            Optional<User> userOpt = userRepository.findByResetPasswordToken(token);
            
            if (userOpt.isEmpty()) {
                logWarn("Token de reset não encontrado: " + token);
                return Optional.empty();
            }
            
            User user = userOpt.get();
            
            // Verifica se o token não expirou
            if (user.isResetPasswordTokenExpired()) {
                logWarn("Token de reset expirado para usuário: " + user.getEmail());
                return Optional.empty();
            }
            
            logInfo("Token de reset validado com sucesso para usuário: " + user.getEmail());
            return Optional.of(user);
            
        } catch (Exception e) {
            logError("Erro ao validar token de reset: " + token, e);
            return Optional.empty();
        }
    }

    /**
     * Redefine a senha do usuário usando um token válido.
     * 
     * @param token A nova senha
     * @param newPassword A nova senha
     * @return true se a senha foi redefinida com sucesso, false caso contrário
     */
    public boolean resetPassword(String token, String newPassword) {
        validateNotEmpty(token, "token");
        validateNotEmpty(newPassword, "newPassword");
        
        try {
            Optional<User> userOpt = validatePasswordResetToken(token);
            
            if (userOpt.isEmpty()) {
                logWarn("Tentativa de reset com token inválido: " + token);
                return false;
            }
            
            User user = userOpt.get();
            
            // Atualiza a senha (será codificada pelo AuthService)
            user.setPassword(newPassword);
            
            // Limpa o token de reset
            user.clearResetPasswordToken();
            
            // Salva as alterações
            userRepository.save(user);
            
            logInfo("Senha redefinida com sucesso para usuário: " + user.getEmail());
            return true;
            
        } catch (Exception e) {
            logError("Erro ao redefinir senha com token: " + token, e);
            return false;
        }
    }

    /**
     * Inicia o processo de recuperação de senha enviando um email com o token.
     * 
     * @param email O email do usuário
     * @return true se o processo foi iniciado com sucesso, false caso contrário
     */
    public boolean initiatePasswordReset(String email) {
        validateNotEmpty(email, "email");
        
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                // Por segurança, não revelamos se o email existe ou não
                logWarn("Tentativa de reset para email não encontrado: " + email);
                return true; // Retorna true para não revelar se o email existe
            }
            
            User user = userOpt.get();
            
            // Gera o token
            String token = generatePasswordResetToken(user);
            
            // Envia o email
            boolean emailSent = emailService.sendPasswordResetEmail(user, token);
            if (emailSent) {
                logInfo("Email de recuperação enviado para: " + user.getEmail());
                logInfo("Processo de reset de senha iniciado para usuário: " + email);
                return true;
            } else {
                logWarn("Falha ao enviar email de recuperação para: " + user.getEmail());
                return false;
            }
            
        } catch (Exception e) {
            logError("Erro ao iniciar processo de reset de senha para email: " + email, e);
            return false;
        }
    }

    /**
     * Limpa tokens de reset expirados do banco de dados.
     * 
     * @return Número de tokens limpos
     */
    public int cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int cleaned = userRepository.clearExpiredResetPasswordTokens(now);
            
            if (cleaned > 0) {
                logInfo("Limpeza de tokens expirados: " + cleaned + " tokens removidos");
            }
            
            return cleaned;
            
        } catch (Exception e) {
            logError("Erro ao limpar tokens expirados", e);
            return 0;
        }
    }

    /**
     * Verifica se um usuário tem um token de reset ativo.
     * 
     * @param email O email do usuário
     * @return true se o usuário tem um token ativo, false caso contrário
     */
    @Transactional(readOnly = true)
    public boolean hasActiveResetToken(String email) {
        validateNotEmpty(email, "email");
        
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                return false;
            }
            
            User user = userOpt.get();
            return user.getResetPasswordToken() != null && !user.isResetPasswordTokenExpired();
            
        } catch (Exception e) {
            logError("Erro ao verificar token ativo para email: " + email, e);
            return false;
        }
    }
}