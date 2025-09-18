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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço para gerenciamento de verificação de email.
 * Responsável por gerar, validar e gerenciar tokens de verificação de email.
 */
@Service
@Transactional
public class EmailVerificationService extends BaseService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.email.verification.token.expiration.hours:24}")
    private int tokenExpirationHours;

    @Value("${app.email.verification.enabled:true}")
    private boolean emailVerificationEnabled;

    /**
     * Gera um novo token de verificação para o usuário.
     * 
     * @param user usuário para gerar token
     * @return token de verificação gerado
     */
    public String generateVerificationToken(User user) {
        validateNotNull(user, "user");
        validateNotNull(user.getId(), "user.id");

        // Gerar token único e seguro
        String token = generateSecureToken();
        
        // Definir expiração
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenExpirationHours);
        
        // Atualizar usuário
        user.setVerificationToken(token);
        user.setVerificationTokenExpiresAt(expiresAt);
        user.setEmailVerified(false);
        
        userRepository.save(user);
        
        // Enviar email de verificação
        try {
            boolean emailSent = emailService.sendVerificationEmail(user, token);
            if (emailSent) {
                logInfo("Token de verificação gerado e email enviado para usuário: " + user.getEmail());
            } else {
                logWarn("Token de verificação gerado mas email não foi enviado para usuário: " + user.getEmail());
            }
        } catch (Exception e) {
            logError("Erro ao enviar email de verificação para usuário: " + user.getEmail(), e);
            // Não falha a geração do token se o email não puder ser enviado
        }
        
        return token;
    }

    /**
     * Verifica um token de verificação de email.
     * 
     * @param token token a ser verificado
     * @return true se token é válido e usuário foi verificado
     */
    public boolean verifyEmailToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            logWarn("Token de verificação é nulo ou vazio");
            return false;
        }

        if (!emailVerificationEnabled) {
            logWarn("Verificação de email está desabilitada");
            return false;
        }

        Optional<User> userOpt = userRepository.findByVerificationToken(token);
        
        if (userOpt.isEmpty()) {
            logWarn("Token de verificação não encontrado: " + token);
            return false;
        }

        User user = userOpt.get();

        // Verificar se token não expirou
        if (user.isVerificationTokenExpired()) {
            logWarn("Token de verificação expirado para usuário: " + user.getEmail());
            return false;
        }

        // Verificar se email já foi verificado
        if (user.isEmailVerified()) {
            logInfo("Email já verificado para usuário: " + user.getEmail());
            return true;
        }

        // Marcar email como verificado
        user.setEmailVerified(true);
        user.clearVerificationToken();
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        
        logInfo("Email verificado com sucesso para usuário: " + user.getEmail());
        
        return true;
    }

    /**
     * Verifica se um usuário precisa verificar o email.
     * 
     * @param user usuário a verificar
     * @return true se precisa verificar email
     */
    public boolean needsEmailVerification(User user) {
        validateNotNull(user, "user");
        
        if (!emailVerificationEnabled) {
            return false;
        }
        
        return !user.isEmailVerified();
    }

    /**
     * Regenera token de verificação para um usuário.
     * 
     * @param email email do usuário
     * @return novo token gerado ou null se usuário não encontrado
     */
    public String regenerateVerificationToken(String email) {
        validateNotEmpty(email, "email");

        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            logWarn("Usuário não encontrado para regenerar token: " + email);
            return null;
        }

        User user = userOpt.get();

        if (user.isEmailVerified()) {
            logInfo("Email já verificado, não é necessário regenerar token: " + email);
            return null;
        }

        return generateVerificationToken(user);
    }

    /**
     * Busca usuário por token de verificação.
     * 
     * @param token token de verificação
     * @return usuário encontrado ou Optional.empty()
     */
    public Optional<User> findUserByVerificationToken(String token) {
        validateNotEmpty(token, "token");
        return userRepository.findByVerificationToken(token);
    }

    /**
     * Lista usuários com email não verificado.
     * 
     * @return lista de usuários não verificados
     */
    public List<User> findUnverifiedUsers() {
        return userRepository.findByEmailVerifiedFalse();
    }

    /**
     * Remove tokens de verificação expirados.
     * 
     * @return número de tokens removidos
     */
    public int cleanupExpiredTokens() {
        List<User> usersWithExpiredTokens = userRepository.findUsersWithExpiredVerificationTokens(LocalDateTime.now());
        
        int count = 0;
        for (User user : usersWithExpiredTokens) {
            user.clearVerificationToken();
            userRepository.save(user);
            count++;
        }
        
        if (count > 0) {
            logInfo("Removidos " + count + " tokens de verificação expirados");
        }
        
        return count;
    }

    /**
     * Verifica se verificação de email está habilitada.
     * 
     * @return true se habilitada
     */
    public boolean isEmailVerificationEnabled() {
        return emailVerificationEnabled;
    }

    /**
     * Obtém estatísticas de verificação de email.
     * 
     * @return mapa com estatísticas
     */
    public EmailVerificationStats getVerificationStats() {
        long totalUsers = userRepository.count();
        long verifiedUsers = userRepository.countByEmailVerifiedTrue();
        long unverifiedUsers = userRepository.countByEmailVerifiedFalse();
        
        return new EmailVerificationStats(totalUsers, verifiedUsers, unverifiedUsers);
    }

    /**
     * Gera um token seguro único.
     * 
     * @return token seguro
     */
    private String generateSecureToken() {
        // Combinar UUID com timestamp para garantir unicidade
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis());
        String combined = uuid + timestamp;
        
        // Aplicar hash SHA-256 para segurança adicional
        return SecurityUtils.hashSHA256(combined);
    }

    /**
     * Classe para estatísticas de verificação de email.
     */
    public static class EmailVerificationStats {
        private final long totalUsers;
        private final long verifiedUsers;
        private final long unverifiedUsers;
        private final double verificationRate;

        public EmailVerificationStats(long totalUsers, long verifiedUsers, long unverifiedUsers) {
            this.totalUsers = totalUsers;
            this.verifiedUsers = verifiedUsers;
            this.unverifiedUsers = unverifiedUsers;
            this.verificationRate = totalUsers > 0 ? (double) verifiedUsers / totalUsers * 100 : 0;
        }

        public long getTotalUsers() { return totalUsers; }
        public long getVerifiedUsers() { return verifiedUsers; }
        public long getUnverifiedUsers() { return unverifiedUsers; }
        public double getVerificationRate() { return verificationRate; }
    }
}