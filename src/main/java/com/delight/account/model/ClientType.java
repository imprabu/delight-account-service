package com.delight.account.model;

/**
 * Enum representing different client types for session management.
 * Each client type has different session TTL requirements.
 */
public enum ClientType {
    /**
     * Web browser clients - 24 hours session validity
     */
    WEB,
    
    /**
     * Android mobile app clients - 7 days session validity
     */
    ANDROID,
    
    /**
     * iOS mobile app clients - 7 days session validity
     */
    IOS;
    
    /**
     * Determines if the client type is a mobile client
     * @return true if client is mobile (Android or iOS), false otherwise
     */
    public boolean isMobile() {
        return this == ANDROID || this == IOS;
    }
    
    /**
     * Gets the default session TTL in hours for this client type
     * @return TTL in hours (24 for web, 168 for mobile)
     */
    public int getDefaultTtlHours() {
        return isMobile() ? 168 : 24; // 7 days for mobile, 24 hours for web
    }
    
    /**
     * Parses client type from x-request-source header value
     * @param requestSource the value from x-request-source header
     * @return ClientType enum value, defaults to WEB if not recognized
     */
    public static ClientType fromRequestSource(String requestSource) {
        if (requestSource == null || requestSource.trim().isEmpty()) {
            return WEB;
        }
        
        String source = requestSource.trim().toLowerCase();
        switch (source) {
            case "android":
                return ANDROID;
            case "ios":
                return IOS;
            default:
                return WEB;
        }
    }
}