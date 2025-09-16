package com.sistema.service.base;

import com.sistema.service.interfaces.CacheOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Classe base para serviços que utilizam Redis.
 * Fornece operações comuns para cache e armazenamento temporário.
 * Implementa CacheOperations para padronizar operações de cache.
 */
public abstract class BaseRedisService extends BaseService implements CacheOperations {
    
    @Autowired
    protected RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Armazena um valor no Redis com TTL.
     * 
     * @param key chave
     * @param value valor
     * @param duration duração do TTL
     */
    protected void setWithTTL(String key, Object value, Duration duration) {
        executeWithErrorHandling(
            () -> {
                redisTemplate.opsForValue().set(key, value, duration);
                return null;
            },
            formatErrorMessage("armazenar valor no Redis", "chave: " + key)
        );
        logger.debug("Valor armazenado no Redis com TTL: {} ({})", key, duration);
    }
    
    /**
     * Armazena um valor no Redis com TTL em minutos.
     * 
     * @param key chave
     * @param value valor
     * @param minutes minutos de TTL
     */
    protected void setWithTTLMinutes(String key, Object value, int minutes) {
        setWithTTL(key, value, Duration.ofMinutes(minutes));
    }
    
    /**
     * Obtém um valor do Redis.
     * 
     * @param key chave
     * @return valor ou null se não encontrado
     */
    protected Object get(String key) {
        return executeWithErrorHandling(
            () -> redisTemplate.opsForValue().get(key),
            formatErrorMessage("obter valor do Redis", "chave: " + key)
        );
    }
    
    /**
     * Obtém um valor do Redis como String.
     * 
     * @param key chave
     * @return valor como String ou null se não encontrado
     */
    protected String getString(String key) {
        Object value = get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Obtém um valor do Redis como Integer.
     * 
     * @param key chave
     * @return valor como Integer ou 0 se não encontrado
     */
    protected int getInteger(String key) {
        try {
            Object value = get(key);
            if (value == null) {
                return 0;
            }
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            logger.warn("Erro ao converter valor para Integer: {}", key);
            return 0;
        }
    }
    
    /**
     * Incrementa um valor no Redis.
     * 
     * @param key chave
     * @return novo valor após incremento
     */
    protected long increment(String key) {
        return executeWithErrorHandling(
            () -> redisTemplate.opsForValue().increment(key),
            formatErrorMessage("incrementar valor no Redis", "chave: " + key)
        );
    }
    
    /**
     * Incrementa um valor no Redis com TTL.
     * 
     * @param key chave
     * @param duration duração do TTL
     * @return novo valor após incremento
     */
    protected long incrementWithTTL(String key, Duration duration) {
        return executeWithErrorHandling(
            () -> {
                long newValue = redisTemplate.opsForValue().increment(key);
                if (newValue == 1) { // Primeira vez, define TTL
                    redisTemplate.expire(key, duration);
                }
                return newValue;
            },
            formatErrorMessage("incrementar valor no Redis com TTL", "chave: " + key)
        );
    }
    
    /**
     * Remove uma chave do Redis.
     * 
     * @param key chave a ser removida
     * @return true se a chave foi removida
     */
    protected boolean delete(String key) {
        return executeWithErrorHandling(
            () -> Boolean.TRUE.equals(redisTemplate.delete(key)),
            formatErrorMessage("remover chave do Redis", "chave: " + key)
        );
    }
    
    /**
     * Remove múltiplas chaves do Redis.
     * 
     * @param keys chaves a serem removidas
     * @return número de chaves removidas
     */
    protected long delete(String... keys) {
        return executeWithErrorHandling(
            () -> {
                Long deleted = redisTemplate.delete(Set.of(keys));
                return deleted != null ? deleted : 0L;
            },
            formatErrorMessage("remover múltiplas chaves do Redis", "quantidade: " + keys.length)
        );
    }
    
    /**
     * Verifica se uma chave existe no Redis.
     * 
     * @param key chave
     * @return true se a chave existe
     */
    protected boolean exists(String key) {
        return executeWithErrorHandling(
            () -> Boolean.TRUE.equals(redisTemplate.hasKey(key)),
            formatErrorMessage("verificar existência de chave no Redis", "chave: " + key)
        );
    }
    
    /**
     * Define TTL para uma chave existente.
     * 
     * @param key chave
     * @param duration duração do TTL
     * @return true se TTL foi definido
     */
    protected boolean expire(String key, Duration duration) {
        return executeWithErrorHandling(
            () -> Boolean.TRUE.equals(redisTemplate.expire(key, duration)),
            formatErrorMessage("definir TTL para chave no Redis", "chave: " + key)
        );
    }
    
    /**
     * Obtém TTL de uma chave.
     * 
     * @param key chave
     * @return TTL em segundos (-1 se sem TTL, -2 se chave não existe)
     */
    protected long getTTL(String key) {
        return executeWithErrorHandling(
            () -> {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                return ttl != null ? ttl : -2L;
            },
            formatErrorMessage("obter TTL de chave no Redis", "chave: " + key)
        );
    }
    
    /**
     * Busca chaves por padrão.
     * 
     * @param pattern padrão de busca
     * @return conjunto de chaves encontradas
     */
    protected Set<String> findKeys(String pattern) {
        return executeWithErrorHandling(
            () -> redisTemplate.keys(pattern),
            formatErrorMessage("buscar chaves no Redis", "padrão: " + pattern)
        );
    }
    
    /**
     * Conta chaves por padrão.
     * 
     * @param pattern padrão de busca
     * @return número de chaves encontradas
     */
    protected long countKeys(String pattern) {
        Set<String> keys = findKeys(pattern);
        return keys != null ? keys.size() : 0L;
    }
    
    /**
     * Limpa chaves por padrão.
     * 
     * @param pattern padrão de busca
     * @return número de chaves removidas
     */
    protected long cleanupKeys(String pattern) {
        Set<String> keys = findKeys(pattern);
        if (keys != null && !keys.isEmpty()) {
            Long deleted = redisTemplate.delete(keys);
            long deletedCount = deleted != null ? deleted : 0L;
            logger.info("Limpeza de chaves Redis: {} chaves removidas para padrão: {}", deletedCount, pattern);
            return deletedCount;
        }
        return 0L;
    }
    
    // Implementação da interface CacheOperations
    
    @Override
    public void storeWithTTL(String key, Object value, Duration duration) {
        setWithTTL(key, value, duration);
    }
    
    @Override
    public void storeWithTTLMinutes(String key, Object value, int minutes) {
        setWithTTLMinutes(key, value, minutes);
    }
    
    @Override
    public Object getValue(String key) {
        return get(key);
    }
    
    @Override
    public String getStringValue(String key) {
        return getString(key);
    }
    
    @Override
    public int getIntegerValue(String key) {
        return getInteger(key);
    }
    
    @Override
    public long incrementValue(String key) {
        return increment(key);
    }
    
    @Override
    public long incrementValueWithTTL(String key, Duration duration) {
        return incrementWithTTL(key, duration);
    }
    
    @Override
    public boolean removeKey(String key) {
        return delete(key);
    }
    
    @Override
    public long removeKeys(String... keys) {
        return delete(keys);
    }
    
    @Override
    public boolean keyExists(String key) {
        return exists(key);
    }
    
    @Override
    public boolean setTTL(String key, Duration duration) {
        return expire(key, duration);
    }
    
    @Override
    public long getTTL(String key) {
        return getTTL(key);
    }
    
    @Override
    public Set<String> findKeysByPattern(String pattern) {
        return findKeys(pattern);
    }
    
    @Override
    public long countKeysByPattern(String pattern) {
        return countKeys(pattern);
    }
    
    @Override
    public long cleanupKeysByPattern(String pattern) {
        return cleanupKeys(pattern);
    }
}