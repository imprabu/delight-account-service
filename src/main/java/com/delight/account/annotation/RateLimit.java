package com.delight.account.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply rate limiting to controller methods
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * Maximum number of requests allowed within the time window
     */
    int maxRequests() default 100;
    
    /**
     * Time window in seconds
     */
    int windowSizeInSeconds() default 60;
    
    /**
     * Key prefix for rate limiting (will be combined with IP address or user identifier)
     */
    String keyPrefix() default "default";
}