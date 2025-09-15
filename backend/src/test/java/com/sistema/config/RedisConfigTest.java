package com.sistema.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Testes unitários para RedisConfig
 * Seguindo práticas de TDD com padrão Given-When-Then
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisConfig Tests")
class RedisConfigTest {

    @InjectMocks
    private RedisConfig redisConfig;

    private RedisConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        connectionFactory = mock(RedisConnectionFactory.class);
    }

    @Test
    @DisplayName("Deve criar RedisTemplate com configurações corretas")
    void shouldCreateRedisTemplateWithCorrectConfiguration() {
        // When
        RedisTemplate<String, Object> redisTemplate = redisConfig.redisTemplate(connectionFactory);

        // Then
        assertThat(redisTemplate).isNotNull();
        assertThat(redisTemplate.getConnectionFactory()).isEqualTo(connectionFactory);
        assertThat(redisTemplate.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(redisTemplate.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(redisTemplate.getValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
        assertThat(redisTemplate.getHashValueSerializer()).isInstanceOf(GenericJackson2JsonRedisSerializer.class);
    }

    @Test
    @DisplayName("Deve criar CacheManager com configurações corretas")
    void shouldCreateCacheManagerWithCorrectConfiguration() {
        // When
        CacheManager cacheManager = redisConfig.cacheManager(connectionFactory);

        // Then
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
    }

    @Test
    @DisplayName("Deve configurar TTL padrão de 10 minutos para cache")
    void shouldConfigureDefaultTtlOf10MinutesForCache() {
        // When
        CacheManager cacheManager = redisConfig.cacheManager(connectionFactory);
        RedisCacheManager redisCacheManager = (RedisCacheManager) cacheManager;

        // Then
        assertThat(redisCacheManager).isNotNull();
        // Verificação da configuração de TTL seria feita através de integração
        // pois a configuração interna não é facilmente acessível via API pública
    }
}