package com.delight.account.logging;

import com.delight.account.util.LoggingUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that the enhanced logging system automatically captures
 * account context from MDC in log messages.
 */
class LoggingContextTest {

    private static final Logger logger = LoggerFactory.getLogger(LoggingContextTest.class);

    @BeforeEach
    void setUp() {
        // Clear MDC before each test
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        // Clear MDC after each test
        MDC.clear();
    }

    @Test
    void testBusinessOperationLoggingWithMDCContext() {
        // Set account context in MDC
        MDC.put("accountId", "12345");
        MDC.put("domain", "test.example.com");
        MDC.put("userId", "67890");

        // Test that LoggingUtils automatically includes MDC context
        LoggingUtils.logBusinessOperation(logger, "User activation", "activateUser", "SUCCESS");

        // Verify MDC values are still present
        assertThat(MDC.get("accountId")).isEqualTo("12345");
        assertThat(MDC.get("domain")).isEqualTo("test.example.com");
        assertThat(MDC.get("userId")).isEqualTo("67890");
    }

    @Test
    void testSecurityEventLoggingWithMDCContext() {
        // Set account context in MDC
        MDC.put("accountId", "98765");
        MDC.put("domain", "secure.example.com");

        // Test security event logging
        LoggingUtils.logSecurityEvent(logger, "Authentication", "signin", "FAILED", "Invalid password");

        // Verify MDC values are still present
        assertThat(MDC.get("accountId")).isEqualTo("98765");
        assertThat(MDC.get("domain")).isEqualTo("secure.example.com");
    }

    @Test
    void testDatabaseOperationLoggingWithMDCContext() {
        // Set account context in MDC
        MDC.put("accountId", "11111");
        MDC.put("domain", "db.example.com");

        // Test database operation logging
        LoggingUtils.logDatabaseOperation(logger, "User", "findByEmail", "SUCCESS", "user@example.com");

        // Verify MDC values are still present
        assertThat(MDC.get("accountId")).isEqualTo("11111");
        assertThat(MDC.get("domain")).isEqualTo("db.example.com");
    }

    @Test
    void testCacheOperationLoggingWithMDCContext() {
        // Set account context in MDC
        MDC.put("accountId", "22222");
        MDC.put("domain", "cache.example.com");

        // Test cache operation logging
        LoggingUtils.logCacheOperation(logger, "User", "evict", "SUCCESS", "user:12345");

        // Verify MDC values are still present
        assertThat(MDC.get("accountId")).isEqualTo("22222");
        assertThat(MDC.get("domain")).isEqualTo("cache.example.com");
    }

    @Test
    void testLoggingWithoutMDCContext() {
        // Test that logging works even without MDC context
        LoggingUtils.logBusinessOperation(logger, "Test operation", "testMethod", "SUCCESS");

        // Verify MDC is empty
        assertThat(MDC.get("accountId")).isNull();
        assertThat(MDC.get("domain")).isNull();
    }

    @Test
    void testAddUserContextToMDC() {
        // Test the addUserContext utility method
        LoggingUtils.addUserContext(54321L, 33333L);

        // Verify values are set in MDC
        assertThat(MDC.get("userId")).isEqualTo("54321");
        assertThat(MDC.get("accountId")).isEqualTo("33333");
    }
}