package com.sistema.service.interfaces;

import java.time.Duration;
import java.util.Set;

/**
 * Interface para operações de cache padronizadas.
 * Define métodos comuns para armazenamento temporário de dados.
 */
public interface CacheOperations {
    
    /**
     * Armazena um valor no cache com TTL.
     * 
     * @param key chave única
     * @param value valor a ser armazenado
     * @param duration duração do TTL
     */
    void storeWithTTL(String key, Object value, Duration duration);
    
    /**
     * Armazena um valor no cache com TTL em minutos.
     * 
     * @param key chave única
     * @param value valor a ser armazenado
     * @param minutes minutos de TTL
     */
    void storeWithTTLMinutes(String key, Object value, int minutes);
    
    /**
     * Obtém um valor do cache.
     * 
     * @param key chave
     * @return valor ou null se não encontrado
     */
    Object getValue(String key);
    
    /**
     * Obtém um valor do cache como String.
     * 
     * @param key chave
     * @return valor como String ou null se não encontrado
     */
    String getStringValue(String key);
    
    /**
     * Obtém um valor do cache como Integer.
     * 
     * @param key chave
     * @return valor como Integer ou 0 se não encontrado
     */
    int getIntegerValue(String key);
    
    /**
     * Incrementa um valor no cache.
     * 
     * @param key chave
     * @return novo valor após incremento
     */
    long incrementValue(String key);
    
    /**
     * Incrementa um valor no cache com TTL.
     * 
     * @param key chave
     * @param duration duração do TTL
     * @return novo valor após incremento
     */
    long incrementValueWithTTL(String key, Duration duration);
    
    /**
     * Remove uma chave do cache.
     * 
     * @param key chave a ser removida
     * @return true se removida com sucesso
     */
    boolean removeKey(String key);
    
    /**
     * Remove múltiplas chaves do cache.
     * 
     * @param keys chaves a serem removidas
     * @return número de chaves removidas
     */
    long removeKeys(String... keys);
    
    /**
     * Verifica se uma chave existe no cache.
     * 
     * @param key chave
     * @return true se existe
     */
    boolean keyExists(String key);
    
    /**
     * Define TTL para uma chave existente.
     * 
     * @param key chave
     * @param duration duração do TTL
     * @return true se TTL foi definido com sucesso
     */
    boolean setTTL(String key, Duration duration);
    
    /**
     * Obtém TTL restante de uma chave.
     * 
     * @param key chave
     * @return TTL em segundos (-1 se não tem TTL, -2 se não existe)
     */
    long getTTL(String key);
    
    /**
     * Busca chaves por padrão.
     * 
     * @param pattern padrão de busca (ex: "user:*")
     * @return conjunto de chaves encontradas
     */
    Set<String> findKeysByPattern(String pattern);
    
    /**
     * Conta chaves por padrão.
     * 
     * @param pattern padrão de busca
     * @return número de chaves encontradas
     */
    long countKeysByPattern(String pattern);
    
    /**
     * Remove chaves por padrão.
     * 
     * @param pattern padrão de busca
     * @return número de chaves removidas
     */
    long cleanupKeysByPattern(String pattern);
}