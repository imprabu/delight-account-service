package com.delight.account.interceptor;

import com.delight.account.util.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

@Component
public class RequestTracingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RequestTracingInterceptor.class);
    
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final String USER_ID_MDC_KEY = "userId";
    private static final String METHOD_MDC_KEY = "method";
    private static final String URI_MDC_KEY = "uri";
    private static final String START_TIME_ATTR = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Generate or extract request ID
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        
        // Set request start time
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_ATTR, startTime);
        
        // Add to MDC for logging
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        MDC.put(METHOD_MDC_KEY, request.getMethod());
        MDC.put(URI_MDC_KEY, request.getRequestURI());
        
        // Extract user ID from request attributes if available (set by JWT filter)
        Object userId = request.getAttribute("userId");
        if (userId != null) {
            MDC.put(USER_ID_MDC_KEY, userId.toString());
        }
        
        // Extract account context from request attributes if available (set by DomainAccountInterceptor)
        Object accountId = request.getAttribute("accountId");
        Object domain = request.getAttribute("domain");
        if (accountId != null) {
            MDC.put("accountId", accountId.toString());
        }
        if (domain != null) {
            MDC.put("domain", domain.toString());
        }
        
        // Add request ID to response header for client correlation
        response.setHeader(REQUEST_ID_HEADER, requestId);
        
        LoggingUtils.logBusinessOperation(logger, "REQUEST", "STARTED", 
            "method", request.getMethod(), 
            "uri", request.getRequestURI());
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            long endTime = System.currentTimeMillis();
            long startTime = (Long) request.getAttribute(START_TIME_ATTR);
            long duration = endTime - startTime;
            
            // Update MDC with final request information
            MDC.put("duration", String.valueOf(duration));
            MDC.put("statusCode", String.valueOf(response.getStatus()));
            
            // Get account context from request attributes (set by DomainAccountInterceptor)
            Object accountId = request.getAttribute("accountId");
            Object domain = request.getAttribute("domain");
            
            if (accountId != null) {
                MDC.put("accountId", accountId.toString());
            }
            if (domain != null) {
                MDC.put("domain", domain.toString());
            }
            
            LoggingUtils.logBusinessOperation(logger, "REQUEST", "COMPLETED", 
                "duration", duration, 
                "statusCode", response.getStatus());
            
        } finally {
            // Clean up MDC
            MDC.clear();
        }
    }
}