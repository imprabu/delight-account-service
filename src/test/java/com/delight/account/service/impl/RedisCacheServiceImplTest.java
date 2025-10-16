package com.delight.account.service.impl;

import com.delight.account.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new RedisCacheServiceImpl(redisTemplate, Duration.ofHours(1));
    }

    @Test
    void testGet_CacheHit() {
        // Given
        String key = "test:key";
        String cachedValue = "cached-value";
        Supplier<Optional<String>> fallback = () -> Optional.of("fallback-value");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(cachedValue);

        // When
        Optional<String> result = cacheService.get(key, fallback, Duration.ofMinutes(10), String.class);

        // Then
        assertTrue(result.isPresent());
        assertEquals(cachedValue, result.get());
        verify(valueOperations).get(key);
        verifyNoMoreInteractions(valueOperations);
    }

    @Test
    void testGet_CacheMiss_FallbackReturnsValue() {
        // Given
        String key = "test:key";
        String fallbackValue = "fallback-value";
        Supplier<Optional<String>> fallback = () -> Optional.of(fallbackValue);
        Duration ttl = Duration.ofMinutes(10);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(null);

        // When
        Optional<String> result = cacheService.get(key, fallback, ttl, String.class);

        // Then
        assertTrue(result.isPresent());
        assertEquals(fallbackValue, result.get());
        verify(valueOperations).get(key);
        verify(valueOperations).set(key, fallbackValue, Duration.ofMinutes(10));
    }

    @Test
    void testGet_CacheMiss_FallbackReturnsEmpty() {
        // Given
        String key = "test:key";
        Supplier<Optional<String>> fallback = () -> Optional.empty();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(null);

        // When
        Optional<String> result = cacheService.get(key, fallback, Duration.ofMinutes(10), String.class);

        // Then
        assertFalse(result.isPresent());
        verify(valueOperations).get(key);
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    void testGet_RedisException_FallsBackToDatabase() {
        // Given
        String key = "test:key";
        String fallbackValue = "fallback-value";
        Supplier<Optional<String>> fallback = () -> Optional.of(fallbackValue);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        Optional<String> result = cacheService.get(key, fallback, Duration.ofMinutes(10), String.class);

        // Then
        assertTrue(result.isPresent());
        assertEquals(fallbackValue, result.get());
        verify(valueOperations).get(key);
    }

    @Test
    void testPut_Success() {
        // Given
        String key = "test:key";
        String value = "test-value";
        Duration ttl = Duration.ofMinutes(15);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        cacheService.put(key, value, ttl);

        // Then
        verify(valueOperations).set(key, value, ttl);
    }

    @Test
    void testPut_RedisException_DoesNotThrow() {
        // Given
        String key = "test:key";
        String value = "test-value";
        Duration ttl = Duration.ofMinutes(15);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis connection failed"))
            .when(valueOperations).set(key, value, ttl);

        // When & Then
        assertDoesNotThrow(() -> cacheService.put(key, value, ttl));
    }

    @Test
    void testEvict_Success() {
        // Given
        String key = "test:key";

        when(redisTemplate.delete(key)).thenReturn(true);

        // When
        cacheService.evict(key);

        // Then
        verify(redisTemplate).delete(key);
    }

    @Test
    void testExists_KeyExists() {
        // Given
        String key = "test:key";

        when(redisTemplate.hasKey(key)).thenReturn(true);

        // When
        boolean result = cacheService.exists(key);

        // Then
        assertTrue(result);
        verify(redisTemplate).hasKey(key);
    }

    @Test
    void testExists_KeyDoesNotExist() {
        // Given
        String key = "test:key";

        when(redisTemplate.hasKey(key)).thenReturn(false);

        // When
        boolean result = cacheService.exists(key);

        // Then
        assertFalse(result);
        verify(redisTemplate).hasKey(key);
    }

    @Test
    void testExists_RedisException_ReturnsFalse() {
        // Given
        String key = "test:key";

        when(redisTemplate.hasKey(key)).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        boolean result = cacheService.exists(key);

        // Then
        assertFalse(result);
        verify(redisTemplate).hasKey(key);
    }
}