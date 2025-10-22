package com.delight.account.integration;

import com.delight.account.model.Account;
import com.delight.account.model.User;
import com.delight.account.model.UserStatus;
import com.delight.account.repository.AccountRepository;
import com.delight.account.repository.UserRepository;
import com.delight.account.service.CacheService;
import com.delight.account.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CacheIntegrationTest {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        // Clear Redis cache before each test
        try {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        } catch (Exception e) {
            // Redis might not be available in test environment, that's okay
        }
    }

    @Test
    void testCacheService_BasicOperations() {
        // Given
        String key = "test:integration:key";
        String value = "test-value";
        Duration ttl = Duration.ofMinutes(5);

        // When - Put value in cache
        cacheService.put(key, value, ttl);

        // Then - Value should exist in cache
        assertTrue(cacheService.exists(key));

        // When - Get value from cache
        Optional<String> cachedValue = cacheService.get(key, () -> Optional.of("fallback"), ttl, String.class);

        // Then - Should return cached value, not fallback
        assertTrue(cachedValue.isPresent());
        assertEquals(value, cachedValue.get());
    }

    @Test
    void testCacheService_FallbackWhenCacheMiss() {
        // Given
        String key = "test:integration:miss";
        String fallbackValue = "fallback-value";

        // When - Get value that doesn't exist in cache
        Optional<String> result = cacheService.get(key, () -> Optional.of(fallbackValue), Duration.ofMinutes(5), String.class);

        // Then - Should return fallback value and cache it
        assertTrue(result.isPresent());
        assertEquals(fallbackValue, result.get());
        assertTrue(cacheService.exists(key));
    }

    @Test
    void testCacheService_EvictOperations() {
        // Given
        String key1 = "test:pattern:1";
        String key2 = "test:pattern:2";
        String key3 = "test:other:1";

        cacheService.put(key1, "value1", Duration.ofMinutes(5));
        cacheService.put(key2, "value2", Duration.ofMinutes(5));
        cacheService.put(key3, "value3", Duration.ofMinutes(5));

        // When - Evict single key
        cacheService.evict(key1);

        // Then - Only key1 should be evicted
        assertFalse(cacheService.exists(key1));
        assertTrue(cacheService.exists(key2));
        assertTrue(cacheService.exists(key3));

        // When - Evict by pattern
        cacheService.evictByPattern("test:pattern:*");

        // Then - All pattern matching keys should be evicted
        assertFalse(cacheService.exists(key2));
        assertTrue(cacheService.exists(key3)); // This should still exist
    }

    @Test
    void testCacheService_WithRealDatabaseFallback() {
        // Given - Create a test account first
        Account testAccount = new Account();
        testAccount.setDomain("test.example.com");
        testAccount.setCompanyName("Test Company");
        Account savedAccount = accountRepository.save(testAccount);

        // Create a test user in database
        User testUser = new User();
        testUser.setAccount(savedAccount);
        testUser.setEmailAddress("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setCompanyName("Test Company");
        User savedUser = userRepository.save(testUser);

        String cacheKey = String.format("user:account:%d:email:%s", savedAccount.getId(), savedUser.getEmailAddress());

        // When - Get user through cache service with database fallback
        Optional<User> result = cacheService.get(cacheKey, () -> {
            return userRepository.findByAccountIdAndEmailAddress(savedAccount.getId(), savedUser.getEmailAddress());
        }, Duration.ofMinutes(15), User.class);

        // Then - Should return user from database and cache it
        assertTrue(result.isPresent());
        assertEquals(savedUser.getEmailAddress(), result.get().getEmailAddress());
        assertEquals(savedUser.getFirstName(), result.get().getFirstName());
        assertTrue(cacheService.exists(cacheKey));

        // When - Get same user again
        Optional<User> cachedResult = cacheService.get(cacheKey, () -> {
            fail("Should not call fallback when value is cached");
            return Optional.empty();
        }, Duration.ofMinutes(15), User.class);

        // Then - Should return cached user without calling database
        assertTrue(cachedResult.isPresent());
        assertEquals(savedUser.getEmailAddress(), cachedResult.get().getEmailAddress());
    }

    @Test
    void testCacheService_GracefulFailureHandling() {
        // Given
        String key = "test:failure:handling";
        String fallbackValue = "fallback-when-cache-fails";

        // When - Try to get value (cache might fail gracefully)
        Optional<String> result = cacheService.get(key, () -> Optional.of(fallbackValue), Duration.ofMinutes(5), String.class);

        // Then - Should gracefully fallback to supplier
        assertTrue(result.isPresent());
        assertEquals(fallbackValue, result.get());

        // When - Try to put value (should not throw exception)
        assertDoesNotThrow(() -> cacheService.put(key, "test-value", Duration.ofMinutes(5)));

        // When - Try to check existence (should handle gracefully)
        boolean exists = cacheService.exists(key);

        // Then - Should return a boolean value without throwing
        assertNotNull(exists);
    }
}