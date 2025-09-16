package com.sistema.service.interfaces;

import com.sistema.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Interface para operações de segurança padronizadas.
 * Define métodos comuns para autenticação, autorização e controle de acesso.
 */
public interface SecurityOperations {
    
    /**
     * Autentica um usuário com credenciais.
     * 
     * @param usernameOrEmail username ou email
     * @param password senha
     * @param request requisição HTTP
     * @return mapa com tokens e informações do usuário
     * @throws org.springframework.security.core.AuthenticationException se credenciais inválidas
     */
    Map<String, Object> authenticate(String usernameOrEmail, String password, HttpServletRequest request);
    
    /**
     * Valida se um token é válido para um usuário específico.
     * 
     * @param token token JWT
     * @param username nome do usuário
     * @return true se válido
     */
    boolean isTokenValidForUser(String token, String username);
    
    /**
     * Revoga um token específico.
     * 
     * @param token token a ser revogado
     * @return true se revogado com sucesso
     */
    boolean revokeToken(String token);
    
    /**
     * Verifica se um token está revogado.
     * 
     * @param token token a ser verificado
     * @return true se revogado
     */
    boolean isTokenRevoked(String token);
    
    /**
     * Revoga todos os tokens de um usuário.
     * 
     * @param username nome do usuário
     * @return true se operação bem-sucedida
     */
    boolean revokeAllUserTokens(String username);
    
    /**
     * Verifica se um token foi globalmente revogado para o usuário.
     * 
     * @param token token a ser verificado
     * @param username nome do usuário
     * @return true se globalmente revogado
     */
    boolean isTokenGloballyRevoked(String token, String username);
    
    /**
     * Valida permissões de acesso para um recurso.
     * 
     * @param user usuário
     * @param resource recurso solicitado
     * @param action ação solicitada
     * @return true se tem permissão
     */
    boolean hasPermission(User user, String resource, String action);
    
    /**
     * Verifica se o usuário tem uma role específica.
     * 
     * @param user usuário
     * @param role role requerida
     * @return true se tem a role
     */
    boolean hasRole(User user, String role);
    
    /**
     * Valida se o usuário está ativo e habilitado.
     * 
     * @param user usuário
     * @return true se ativo
     */
    boolean isUserActive(User user);
    
    /**
     * Obtém informações de segurança do usuário.
     * 
     * @param user usuário
     * @return mapa com informações de segurança
     */
    Map<String, Object> getUserSecurityInfo(User user);
    
    /**
     * Registra evento de segurança.
     * 
     * @param event tipo de evento
     * @param user usuário (pode ser null)
     * @param details detalhes do evento
     * @param request requisição HTTP (pode ser null)
     */
    void logSecurityEvent(String event, User user, String details, HttpServletRequest request);
    
    /**
     * Valida força da senha.
     * 
     * @param password senha a ser validada
     * @return true se senha é forte
     */
    boolean isPasswordStrong(String password);
    
    /**
     * Gera identificador único para controle de tentativas.
     * 
     * @param request requisição HTTP
     * @param additionalInfo informação adicional (email, username)
     * @return identificador único
     */
    String generateAttemptIdentifier(HttpServletRequest request, String additionalInfo);
}