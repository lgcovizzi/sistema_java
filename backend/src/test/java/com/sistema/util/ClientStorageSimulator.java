package com.sistema.util;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Simulador de armazenamento do lado do cliente para testes.
 * Simula localStorage/sessionStorage do navegador para persistir JWT tokens.
 */
public class ClientStorageSimulator {
    
    private final Map<String, StoredToken> storage = new HashMap<>();
    
    /**
     * Classe interna para representar um token armazenado com metadados.
     */
    public static class StoredToken {
        private final String token;
        private final LocalDateTime storedAt;
        private final String tokenType;
        
        public StoredToken(String token, String tokenType) {
            this.token = token;
            this.tokenType = tokenType;
            this.storedAt = LocalDateTime.now();
        }
        
        public String getToken() {
            return token;
        }
        
        public LocalDateTime getStoredAt() {
            return storedAt;
        }
        
        public String getTokenType() {
            return tokenType;
        }
    }
    
    /**
     * Armazena um JWT token no "localStorage" simulado.
     * 
     * @param key chave para armazenar o token
     * @param token o JWT token
     * @param tokenType tipo do token (access ou refresh)
     */
    public void storeToken(String key, String token, String tokenType) {
        storage.put(key, new StoredToken(token, tokenType));
    }
    
    /**
     * Recupera um token armazenado.
     * 
     * @param key chave do token
     * @return Optional contendo o token se encontrado
     */
    public Optional<StoredToken> getToken(String key) {
        return Optional.ofNullable(storage.get(key));
    }
    
    /**
     * Remove um token do armazenamento.
     * 
     * @param key chave do token a ser removido
     */
    public void removeToken(String key) {
        storage.remove(key);
    }
    
    /**
     * Verifica se existe um token armazenado para a chave.
     * 
     * @param key chave do token
     * @return true se o token existe
     */
    public boolean hasToken(String key) {
        return storage.containsKey(key);
    }
    
    /**
     * Limpa todo o armazenamento.
     */
    public void clear() {
        storage.clear();
    }
    
    /**
     * Retorna o número de tokens armazenados.
     * 
     * @return quantidade de tokens
     */
    public int size() {
        return storage.size();
    }
    
    /**
     * Simula o comportamento de armazenar tokens de autenticação.
     * 
     * @param accessToken token de acesso
     * @param refreshToken token de refresh
     */
    public void storeAuthTokens(String accessToken, String refreshToken) {
        storeToken("accessToken", accessToken, "access");
        storeToken("refreshToken", refreshToken, "refresh");
    }
    
    /**
     * Recupera o token de acesso armazenado.
     * 
     * @return Optional contendo o token de acesso
     */
    public Optional<String> getAccessToken() {
        return getToken("accessToken").map(StoredToken::getToken);
    }
    
    /**
     * Recupera o token de refresh armazenado.
     * 
     * @return Optional contendo o token de refresh
     */
    public Optional<String> getRefreshToken() {
        return getToken("refreshToken").map(StoredToken::getToken);
    }
    
    /**
     * Remove todos os tokens de autenticação (simula logout).
     */
    public void clearAuthTokens() {
        removeToken("accessToken");
        removeToken("refreshToken");
    }
    
    /**
     * Verifica se há tokens de autenticação armazenados.
     * 
     * @return true se há pelo menos um token de auth
     */
    public boolean hasAuthTokens() {
        return hasToken("accessToken") || hasToken("refreshToken");
    }
}