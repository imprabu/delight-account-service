package com.delight.account.context;

/**
 * Thread-local context for managing tenant (account) information across request processing.
 * This class provides a thread-safe way to store and retrieve the current account ID
 * throughout the request lifecycle.
 */
public class TenantContext {
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * Sets the account ID for the current thread.
     * 
     * @param tenantId the account ID to set
     */
    public static void setAccountId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Gets the account ID for the current thread.
     * 
     * @return the account ID, or null if not set
     */
    public static String getAccountId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Clears the account ID from the current thread.
     * This should be called at the end of request processing to prevent memory leaks.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}