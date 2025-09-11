package com.sistema.java.service;

import com.sistema.java.model.entity.Usuario;
import com.sistema.java.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;

/**
 * Implementação do UserDetailsService para autenticação Spring Security
 * Referência: Controle de Acesso - project_rules.md
 * Referência: Login e Registro - project_rules.md
 */
@Service
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
    
    private final UsuarioRepository usuarioRepository;
    
    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }
    
    /**
     * Carrega usuário por email para autenticação
     * Referência: Login e Registro - project_rules.md
     * 
     * @param email Email do usuário (usado como username)
     * @return UserDetails com informações do usuário
     * @throws UsernameNotFoundException Se usuário não for encontrado
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Tentando carregar usuário com email: {}", email);
        
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> {
                logger.warn("Usuário não encontrado com email: {}", email);
                return new UsernameNotFoundException("Usuário não encontrado com email: " + email);
            });
        
        // Verificar se o usuário está ativo
        if (!usuario.isAtivo()) {
            logger.warn("Tentativa de login com usuário inativo: {}", email);
            throw new UsernameNotFoundException("Usuário inativo: " + email);
        }
        
        logger.debug("Usuário carregado com sucesso: {} - Papel: {}", email, usuario.getPapel());
        
        return User.builder()
            .username(usuario.getEmail())
            .password(usuario.getSenha())
            .authorities(getAuthorities(usuario))
            .accountExpired(false)
            .accountLocked(!usuario.isAtivo())
            .credentialsExpired(false)
            .disabled(!usuario.isAtivo())
            .build();
    }
    
    /**
     * Converte o papel do usuário em authorities do Spring Security
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param usuario Usuário do sistema
     * @return Collection de authorities
     */
    private Collection<? extends GrantedAuthority> getAuthorities(Usuario usuario) {
        String role = "ROLE_" + usuario.getPapel().name();
        logger.debug("Atribuindo role: {} para usuário: {}", role, usuario.getEmail());
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }
    
    /**
     * Carrega usuário por ID (método auxiliar)
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param id ID do usuário
     * @return UserDetails com informações do usuário
     * @throws UsernameNotFoundException Se usuário não for encontrado
     */
    public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
        logger.debug("Tentando carregar usuário com ID: {}", id);
        
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> {
                logger.warn("Usuário não encontrado com ID: {}", id);
                return new UsernameNotFoundException("Usuário não encontrado com ID: " + id);
            });
        
        // Verificar se o usuário está ativo
        if (!usuario.isAtivo()) {
            logger.warn("Tentativa de acesso com usuário inativo ID: {}", id);
            throw new UsernameNotFoundException("Usuário inativo ID: " + id);
        }
        
        logger.debug("Usuário carregado por ID com sucesso: {} - Papel: {}", usuario.getEmail(), usuario.getPapel());
        
        return User.builder()
            .username(usuario.getEmail())
            .password(usuario.getSenha())
            .authorities(getAuthorities(usuario))
            .accountExpired(false)
            .accountLocked(!usuario.isAtivo())
            .credentialsExpired(false)
            .disabled(!usuario.isAtivo())
            .build();
    }
    
    /**
     * Verifica se um usuário existe e está ativo
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param email Email do usuário
     * @return true se usuário existe e está ativo
     */
    public boolean existsActiveUser(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
            .map(Usuario::isAtivo)
            .orElse(false);
    }
    
    /**
     * Obtém usuário por email (método auxiliar para outros serviços)
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param email Email do usuário
     * @return Usuario se encontrado e ativo
     * @throws UsernameNotFoundException Se usuário não for encontrado ou inativo
     */
    public Usuario getActiveUserByEmail(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
        
        if (!usuario.isAtivo()) {
            throw new UsernameNotFoundException("Usuário inativo: " + email);
        }
        
        return usuario;
    }
}