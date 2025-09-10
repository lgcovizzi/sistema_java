package com.example.sistemajava.user;

import com.example.sistemajava.email.EmailService;
import com.example.sistemajava.token.PasswordResetToken;
import com.example.sistemajava.token.PasswordResetTokenRepository;
import com.example.sistemajava.token.VerificationToken;
import com.example.sistemajava.token.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontendBaseUrl}")
    private String frontendBaseUrl;

    public UserService(UserRepository userRepository,
                       VerificationTokenRepository verificationTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    @CacheEvict(value = "userByEmail", key = "#email", beforeInvocation = true)
    public User register(String email, String rawPassword, String cpf) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email já cadastrado");
        }
        if (userRepository.existsByCpf(cpf)) {
            throw new IllegalArgumentException("CPF já cadastrado");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setCpf(cpf);
        // Primeiro usuário do banco será ADMIN, demais USER
        boolean isFirstUser = userRepository.count() == 0;
        user.setRole(isFirstUser ? Role.ADMIN : Role.USER);
        user = userRepository.save(user);

        sendVerificationEmail(user);
        return user;
    }

    @Cacheable(value = "userByEmail", key = "#email")
    public Optional<User> findByEmailCached(String email) {
        return userRepository.findByEmail(email);
    }

    public void sendVerificationEmail(User user) {
        verificationTokenRepository.deleteByUser(user);
        VerificationToken token = new VerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        verificationTokenRepository.save(token);

        String link = frontendBaseUrl + "/activate?token=" + token.getToken();
        emailService.send(user.getEmail(), "Ative sua conta", "Clique para ativar: " + link);
    }

    @Transactional
    public void activate(String token) {
        VerificationToken vt = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));
        if (vt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expirado");
        }
        User user = vt.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        verificationTokenRepository.deleteByUser(user);
    }

    public void requestPasswordReset(String email, String cpf) {
        Optional<User> userOpt = userRepository.findByEmail(email)
                .filter(u -> u.getCpf().equals(cpf));
        if (userOpt.isEmpty()) {
            return; // não revela existência
        }
        User user = userOpt.get();
        passwordResetTokenRepository.deleteByUser(user);
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiresAt(Instant.now().plus(2, ChronoUnit.HOURS));
        passwordResetTokenRepository.save(token);
        String link = frontendBaseUrl + "/reset-password?token=" + token.getToken();
        emailService.send(user.getEmail(), "Redefinição de senha", "Use este link: " + link);
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String cpf) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));
        if (prt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expirado");
        }
        User user = prt.getUser();
        if (!user.getCpf().equals(cpf)) {
            throw new IllegalArgumentException("CPF não confere");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetTokenRepository.deleteByUser(user);
    }

    public void recoverEmailByCpf(String cpf) {
        userRepository.findByCpf(cpf).ifPresent(user -> {
            emailService.send(user.getEmail(), "Recuperação de email", "Seu email cadastrado é: " + user.getEmail());
        });
    }
}


