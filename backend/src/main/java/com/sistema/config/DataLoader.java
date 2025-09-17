package com.sistema.config;

import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * DataLoader para inicializar dados padrão na aplicação.
 * Cria usuários iniciais se não existirem.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Iniciando carregamento de dados iniciais...");
        
        createDefaultUsers();
        
        logger.info("Carregamento de dados iniciais concluído.");
    }

    /**
     * Cria usuários padrão se não existirem.
     */
    private void createDefaultUsers() {
        // Criar usuário admin se não existir
        if (!userRepository.existsByEmail("admin@sistema.com")) {
            User admin = new User();
            admin.setEmail("admin@sistema.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFirstName("Administrador");
            admin.setLastName("Sistema");
            admin.setCpf("11144477735"); // CPF válido para usuário admin
            admin.setRole(UserRole.ADMIN);
            admin.setEnabled(true);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(admin);
            logger.info("Usuário admin criado com sucesso: {}", admin.getEmail());
        } else {
            logger.info("Usuário admin já existe: admin@sistema.com");
        }

        // Criar usuário de teste se não existir
        if (!userRepository.existsByEmail("test@sistema.com")) {
            User testUser = new User();
            testUser.setEmail("test@sistema.com");
            testUser.setPassword(passwordEncoder.encode("test123"));
            testUser.setFirstName("Usuário");
            testUser.setLastName("Teste");
            testUser.setCpf("98765432100"); // CPF válido para usuário de teste
            testUser.setRole(UserRole.USER);
            testUser.setEnabled(true);
            testUser.setCreatedAt(LocalDateTime.now());
            testUser.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(testUser);
            logger.info("Usuário test@sistema.com criado com sucesso");
        } else {
            logger.info("Usuário test@sistema.com já existe");
        }

        // Criar usuário demo se não existir
        if (!userRepository.existsByEmail("demo@sistema.com")) {
            User demoUser = new User();
            demoUser.setEmail("demo@sistema.com");
            demoUser.setPassword(passwordEncoder.encode("demo123"));
            demoUser.setFirstName("Demo");
            demoUser.setLastName("User");
            demoUser.setCpf("12345678909"); // CPF válido para usuário demo
            demoUser.setRole(UserRole.USER);
            demoUser.setEnabled(true);
            demoUser.setCreatedAt(LocalDateTime.now());
            demoUser.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(demoUser);
            logger.info("Usuário demo@sistema.com criado com sucesso");
        } else {
            logger.info("Usuário demo já existe");
        }

        long totalUsers = userRepository.count();
        logger.info("Total de usuários no sistema: {}", totalUsers);
    }
}