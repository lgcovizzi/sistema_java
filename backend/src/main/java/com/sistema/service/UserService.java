package com.sistema.service;

import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cria um novo usuário
     */
    public User createUser(String firstName, String lastName, String email, String password) {
        // Verificar se email já existe
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email já está em uso");
        }

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        
        // Auto-promoção: primeiro usuário vira admin
        if (userRepository.isFirstUser()) {
            user.setRole(UserRole.ADMIN);
        } else {
            user.setRole(UserRole.USER);
        }

        return userRepository.save(user);
    }



    /**
     * Busca usuário por email
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }



    /**
     * Lista todos os usuários ativos
     */
    @Transactional(readOnly = true)
    public List<User> findActiveUsers() {
        return userRepository.findByEnabledTrue();
    }

    /**
     * Lista usuários por role
     */
    @Transactional(readOnly = true)
    public List<User> findUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    /**
     * Busca usuários por texto (nome ou email)
     */
    @Transactional(readOnly = true)
    public List<User> searchUsers(String searchTerm) {
        return userRepository.searchUsers(searchTerm);
    }

    /**
     * Atualiza status ativo/inativo do usuário
     */
    public User updateUserStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        
        user.setActive(active);
        return userRepository.save(user);
    }

    /**
     * Atualiza role do usuário
     */
    public User updateUserRole(Long userId, UserRole newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        
        user.setRole(newRole);
        return userRepository.save(user);
    }

    /**
     * Atualiza último login do usuário
     */
    public void updateLastLogin(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            userRepository.updateLastLogin(userOpt.get().getId(), LocalDateTime.now());
        }
    }

    /**
     * Verifica se é o primeiro usuário do sistema
     */
    @Transactional(readOnly = true)
    public boolean isFirstUser() {
        return userRepository.isFirstUser();
    }

    /**
     * Obtém estatísticas dos usuários
     */
    @Transactional(readOnly = true)
    public UserStatistics getUserStatistics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByEnabledTrue();
        long adminUsers = userRepository.countByRole(UserRole.ADMIN);
        long regularUsers = userRepository.countByRole(UserRole.USER);
        
        return new UserStatistics(totalUsers, activeUsers, adminUsers, regularUsers);
    }

    /**
     * Classe para estatísticas de usuários
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

        public long getTotalUsers() {
            return totalUsers;
        }

        public long getActiveUsers() {
            return activeUsers;
        }

        public long getAdminUsers() {
            return adminUsers;
        }

        public long getRegularUsers() {
            return regularUsers;
        }

        public long getInactiveUsers() {
            return totalUsers - activeUsers;
        }

        public double getActiveUserPercentage() {
            return totalUsers > 0 ? (double) activeUsers / totalUsers * 100 : 0;
        }
    }
}