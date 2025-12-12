package com.delight.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for rate limit check operations.
 * Contains information about whether the request is allowed and current rate limit status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitCheckResponse {
    
    /**
     * Whether the request is allowed (true) or rate limit exceeded (false)
     */
    private Boolean allowed;
    
    /**
     * Number of remaining requests in the current window
     */
    private Integer remainingRequests;
    
    /**
     * Current number of requests in the window
     */
    private Integer currentCount;
    
    /**
     * Maximum number of requests allowed in the window
     */
    private Integer maxRequests;
    
    /**
     * Time window in seconds
     */
    private Integer windowSeconds;
    
    /**
     * Timestamp (epoch seconds) when the current window resets
     */
    private Long resetAt;
}



