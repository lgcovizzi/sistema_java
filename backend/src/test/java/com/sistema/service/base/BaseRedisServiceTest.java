package com.sistema.service.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para BaseRedisService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BaseRedisService Tests")
class BaseRedisServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private TestableBaseRedisService baseRedisService;

    @BeforeEach
    void setUp() {
        baseRedisService = new TestableBaseRedisService();
        baseRedisService.redisTemplate = redisTemplate;
        
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("Storage Operations Tests")
    class StorageOperationsTests {

        @Test
        @DisplayName("Should store value with TTL duration")
        void shouldStoreValueWithTTLDuration() {
            // Given
            String key = "test:key";
            String value = "test value";
            Duration duration = Duration.ofMinutes(5);

            // When
            assertDoesNotThrow(() -> baseRedisService.setWithTTL(key, value, duration));

            // Then
            verify(valueOperations).set(key, value, duration);
        }

        @Test
        @DisplayName("Should store value with TTL in minutes")
        void shouldStoreValueWithTTLMinutes() {
            // Given
            String key = "test:key";
            String value = "test value";
            int minutes = 10;

            // When
            assertDoesNotThrow(() -> baseRedisService.setWithTTLMinutes(key, value, minutes));

            // Then
            verify(valueOperations).set(eq(key), eq(value), any(Duration.class));
        }

        @Test
        @DisplayName("Should handle exception when storing value")
        void shouldHandleExceptionWhenStoringValue() {
            // Given
            String key = "test:key";
            String value = "test value";
            Duration duration = Duration.ofMinutes(5);
            
            doThrow(new RuntimeException("Redis error")).when(valueOperations)
                .set(key, value, duration);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                baseRedisService.setWithTTL(key, value, duration)
            );

            assertTrue(exception.getMessage().contains("armazenar valor no Redis"));
            assertTrue(exception.getMessage().contains("Redis error"));
        }
    }

    @Nested
    @DisplayName("Retrieval Operations Tests")
    class RetrievalOperationsTests {

        @Test
        @DisplayName("Should get value from Redis")
        void shouldGetValueFromRedis() {
            // Given
            String key = "test:key";
            String expectedValue = "test value";
            
            when(valueOperations.get(key)).thenReturn(expectedValue);

            // When
            Object result = baseRedisService.get(key);

            // Then
            assertEquals(expectedValue, result);
            verify(valueOperations).get(key);
        }

        @Test
        @DisplayName("Should return null when key not found")
        void shouldReturnNullWhenKeyNotFound() {
            // Given
            String key = "nonexistent:key";
            
            when(valueOperations.get(key)).thenReturn(null);

            // When
            Object result = baseRedisService.get(key);

            // Then
            assertNull(result);
            verify(valueOperations).get(key);
        }

        @Test
        @DisplayName("Should get string value from Redis")
        void shouldGetStringValueFromRedis() {
            // Given
            String key = "test:key";
            String expectedValue = "test value";
            
            when(valueOperations.get(key)).thenReturn(expectedValue);

            // When
            String result = baseRedisService.getString(key);

            // Then
            assertEquals(expectedValue, result);
        }

        @Test
        @DisplayName("Should return null for string when key not found")
        void shouldReturnNullForStringWhenKeyNotFound() {
            // Given
            String key = "nonexistent:key";
            
            when(valueOperations.get(key)).thenReturn(null);

            // When
            String result = baseRedisService.getString(key);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Should get integer value from Redis")
        void shouldGetIntegerValueFromRedis() {
            // Given
            String key = "test:key";
            Integer expectedValue = 42;
            
            when(valueOperations.get(key)).thenReturn(expectedValue);

            // When
            int result = baseRedisService.getInteger(key);

            // Then
            assertEquals(expectedValue.intValue(), result);
        }

        @Test
        @DisplayName("Should return zero for integer when key not found")
        void shouldReturnZeroForIntegerWhenKeyNotFound() {
            // Given
            String key = "nonexistent:key";
            
            when(valueOperations.get(key)).thenReturn(null);

            // When
            int result = baseRedisService.getInteger(key);

            // Then
            assertEquals(0, result);
        }

        @Test
        @DisplayName("Should return zero for integer when value is not numeric")
        void shouldReturnZeroForIntegerWhenValueIsNotNumeric() {
            // Given
            String key = "test:key";
            String nonNumericValue = "not a number";
            
            when(valueOperations.get(key)).thenReturn(nonNumericValue);

            // When
            int result = baseRedisService.getInteger(key);

            // Then
            assertEquals(0, result);
        }

        @Test
        @DisplayName("Should handle exception when getting value")
        void shouldHandleExceptionWhenGettingValue() {
            // Given
            String key = "test:key";
            
            when(valueOperations.get(key)).thenThrow(new RuntimeException("Redis error"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                baseRedisService.get(key)
            );

            assertTrue(exception.getMessage().contains("obter valor do Redis"));
        }
    }

    @Nested
    @DisplayName("Increment Operations Tests")
    class IncrementOperationsTests {

        @Test
        @DisplayName("Should increment value in Redis")
        void shouldIncrementValueInRedis() {
            // Given
            String key = "test:counter";
            long expectedValue = 5L;
            
            when(valueOperations.increment(key)).thenReturn(expectedValue);

            // When
            long result = baseRedisService.increment(key);

            // Then
            assertEquals(expectedValue, result);
            verify(valueOperations).increment(key);
        }

        @Test
        @DisplayName("Should increment value with TTL")
        void shouldIncrementValueWithTTL() {
            // Given
            String key = "test:counter";
            Duration duration = Duration.ofMinutes(5);
            long expectedValue = 3L;
            
            when(valueOperations.increment(key)).thenReturn(expectedValue);
            when(redisTemplate.hasKey(key)).thenReturn(false);
            when(redisTemplate.expire(key, duration)).thenReturn(true);

            // When
            long result = baseRedisService.incrementWithTTL(key, duration);

            // Then
            assertEquals(expectedValue, result);
            verify(valueOperations).increment(key);
            verify(redisTemplate).expire(key, duration);
        }

        @Test
        @DisplayName("Should not set TTL when key already exists")
        void shouldNotSetTTLWhenKeyAlreadyExists() {
            // Given
            String key = "test:counter";
            Duration duration = Duration.ofMinutes(5);
            long expectedValue = 3L; // Valor > 1 indica que a chave já existe
            
            when(valueOperations.increment(key)).thenReturn(expectedValue);

            // When
            long result = baseRedisService.incrementWithTTL(key, duration);

            // Then
            assertEquals(expectedValue, result);
            verify(valueOperations).increment(key);
            verify(redisTemplate, never()).expire(key, duration);
        }

        @Test
        @DisplayName("Should handle exception when incrementing")
        void shouldHandleExceptionWhenIncrementing() {
            // Given
            String key = "test:counter";
            
            when(valueOperations.increment(key)).thenThrow(new RuntimeException("Redis error"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                baseRedisService.increment(key)
            );

            assertTrue(exception.getMessage().contains("incrementar valor no Redis"));
        }
    }

    @Nested
    @DisplayName("Key Management Tests")
    class KeyManagementTests {

        @Test
        @DisplayName("Should delete single key")
        void shouldDeleteSingleKey() {
            // Given
            String key = "test:key";
            
            when(redisTemplate.delete(key)).thenReturn(true);

            // When
            boolean result = baseRedisService.delete(key);

            // Then
            assertTrue(result);
            verify(redisTemplate).delete(key);
        }

        @Test
        @DisplayName("Should delete multiple keys")
        void shouldDeleteMultipleKeys() {
            // Given
            String[] keys = {"key1", "key2", "key3"};
            long expectedCount = 3L;
            
            when(redisTemplate.delete(Set.of(keys))).thenReturn(expectedCount);

            // When
            long result = baseRedisService.delete(keys);

            // Then
            assertEquals(expectedCount, result);
            verify(redisTemplate).delete(Set.of(keys));
        }

        @Test
        @DisplayName("Should check if key exists")
        void shouldCheckIfKeyExists() {
            // Given
            String key = "test:key";
            
            when(redisTemplate.hasKey(key)).thenReturn(true);

            // When
            boolean result = baseRedisService.exists(key);

            // Then
            assertTrue(result);
            verify(redisTemplate).hasKey(key);
        }

        @Test
        @DisplayName("Should set TTL for key")
        void shouldSetTTLForKey() {
            // Given
            String key = "test:key";
            Duration duration = Duration.ofMinutes(10);
            
            when(redisTemplate.expire(key, duration)).thenReturn(true);

            // When
            boolean result = baseRedisService.expire(key, duration);

            // Then
            assertTrue(result);
            verify(redisTemplate).expire(key, duration);
        }

        @Test
        @DisplayName("Should get TTL for key")
        void shouldGetTTLForKey() {
            // Given
            String key = "test:key";
            long expectedTTL = 300L; // 5 minutes in seconds
            
            when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(expectedTTL);

            // When
            long result = baseRedisService.getTTLInternal(key);

            // Then
            assertEquals(expectedTTL, result);
            verify(redisTemplate).getExpire(key, TimeUnit.SECONDS);
        }

        @Test
        @DisplayName("Should find keys by pattern")
        void shouldFindKeysByPattern() {
            // Given
            String pattern = "test:*";
            Set<String> expectedKeys = Set.of("test:key1", "test:key2");
            
            when(redisTemplate.keys(pattern)).thenReturn(expectedKeys);

            // When
            Set<String> result = baseRedisService.findKeys(pattern);

            // Then
            assertEquals(expectedKeys, result);
            verify(redisTemplate).keys(pattern);
        }

        @Test
        @DisplayName("Should count keys by pattern")
        void shouldCountKeysByPattern() {
            // Given
            String pattern = "test:*";
            Set<String> keys = Set.of("test:key1", "test:key2", "test:key3");
            
            when(redisTemplate.keys(pattern)).thenReturn(keys);

            // When
            long result = baseRedisService.countKeys(pattern);

            // Then
            assertEquals(3L, result);
        }

        @Test
        @DisplayName("Should cleanup keys by pattern")
        void shouldCleanupKeysByPattern() {
            // Given
            String pattern = "test:*";
            Set<String> keys = Set.of("test:key1", "test:key2");
            
            when(redisTemplate.keys(pattern)).thenReturn(keys);
            when(redisTemplate.delete(keys)).thenReturn(2L);

            // When
            long result = baseRedisService.cleanupKeys(pattern);

            // Then
            assertEquals(2L, result);
            verify(redisTemplate).delete(keys);
        }

        @Test
        @DisplayName("Should return zero when no keys to cleanup")
        void shouldReturnZeroWhenNoKeysToCleanup() {
            // Given
            String pattern = "test:*";
            Set<String> emptyKeys = Set.of();
            
            when(redisTemplate.keys(pattern)).thenReturn(emptyKeys);

            // When
            long result = baseRedisService.cleanupKeys(pattern);

            // Then
            assertEquals(0L, result);
            verify(redisTemplate, never()).delete(any(Set.class));
        }
    }

    @Nested
    @DisplayName("CacheOperations Interface Tests")
    class CacheOperationsInterfaceTests {

        @Test
        @DisplayName("Should implement storeWithTTL interface method")
        void shouldImplementStoreWithTTLInterfaceMethod() {
            // Given
            String key = "test:key";
            String value = "test value";
            Duration duration = Duration.ofMinutes(5);

            // When
            assertDoesNotThrow(() -> baseRedisService.storeWithTTL(key, value, duration));

            // Then
            verify(valueOperations).set(key, value, duration);
        }

        @Test
        @DisplayName("Should implement getValue interface method")
        void shouldImplementGetValueInterfaceMethod() {
            // Given
            String key = "test:key";
            String expectedValue = "test value";
            
            when(valueOperations.get(key)).thenReturn(expectedValue);

            // When
            Object result = baseRedisService.getValue(key);

            // Then
            assertEquals(expectedValue, result);
        }

        @Test
        @DisplayName("Should implement removeKey interface method")
        void shouldImplementRemoveKeyInterfaceMethod() {
            // Given
            String key = "test:key";
            
            when(redisTemplate.delete(key)).thenReturn(true);

            // When
            boolean result = baseRedisService.removeKey(key);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should implement keyExists interface method")
        void shouldImplementKeyExistsInterfaceMethod() {
            // Given
            String key = "test:key";
            
            when(redisTemplate.hasKey(key)).thenReturn(true);

            // When
            boolean result = baseRedisService.keyExists(key);

            // Then
            assertTrue(result);
        }
    }

    /**
     * Implementação testável de BaseRedisService para permitir testes.
     */
    private static class TestableBaseRedisService extends BaseRedisService {
        // Expõe métodos protected para teste
        
        @Override
        public void setWithTTL(String key, Object value, Duration duration) {
            super.setWithTTL(key, value, duration);
        }
        
        @Override
        public void setWithTTLMinutes(String key, Object value, int minutes) {
            super.setWithTTLMinutes(key, value, minutes);
        }
        
        @Override
        public Object get(String key) {
            return super.get(key);
        }
        
        @Override
        public String getString(String key) {
            return super.getString(key);
        }
        
        @Override
        public int getInteger(String key) {
            return super.getInteger(key);
        }
        
        @Override
        public long increment(String key) {
            return super.increment(key);
        }
        
        @Override
        public long incrementWithTTL(String key, Duration duration) {
            return super.incrementWithTTL(key, duration);
        }
        
        @Override
        public boolean delete(String key) {
            return super.delete(key);
        }
        
        @Override
        public long delete(String... keys) {
            return super.delete(keys);
        }
        
        @Override
        public boolean exists(String key) {
            return super.exists(key);
        }
        
        @Override
        public boolean expire(String key, Duration duration) {
            return super.expire(key, duration);
        }
        
        @Override
        public long getTTLInternal(String key) {
            return super.getTTLInternal(key);
        }
        
        @Override
        public Set<String> findKeys(String pattern) {
            return super.findKeys(pattern);
        }
        
        @Override
        public long countKeys(String pattern) {
            return super.countKeys(pattern);
        }
        
        @Override
        public long cleanupKeys(String pattern) {
            return super.cleanupKeys(pattern);
        }
    }
}