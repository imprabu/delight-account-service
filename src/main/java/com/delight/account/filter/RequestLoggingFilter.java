package com.delight.account.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.delight.account.util.LoggingUtils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Request logging filter to capture request/response details for better error traceability.
 * Logs all HTTP requests and responses with timing and status information.
 */
@Component
@Order(1)
public class RequestLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Generate request ID if not present
        String requestId = httpRequest.getHeader("X-Request-ID");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        // Set request ID in response headers
        httpResponse.setHeader("X-Request-ID", requestId);

        // Add request context to MDC
        LoggingUtils.addRequestContext(requestId);
        LoggingUtils.addDomainContext(extractDomain(httpRequest));

        long startTime = System.currentTimeMillis();

        try {
            // Log request start
            if (logger.isDebugEnabled()) {
                LoggingUtils.debug(logger, "Request started: {} {} from {}",
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    httpRequest.getRemoteAddr());
            }

            chain.doFilter(request, response);

            // Log successful response
            long duration = System.currentTimeMillis() - startTime;
            if (httpResponse.getStatus() >= 400) {
                LoggingUtils.logBusinessOperation(logger, "HTTP_REQUEST", "ERROR",
                    "method", httpRequest.getMethod(),
                    "uri", httpRequest.getRequestURI(),
                    "status", httpResponse.getStatus(),
                    "duration", duration + "ms",
                    "userAgent", httpRequest.getHeader("User-Agent"),
                    "remoteAddr", httpRequest.getRemoteAddr());
            } else {
                LoggingUtils.logBusinessOperation(logger, "HTTP_REQUEST", "SUCCESS",
                    "method", httpRequest.getMethod(),
                    "uri", httpRequest.getRequestURI(),
                    "status", httpResponse.getStatus(),
                    "duration", duration + "ms");
            }

        } catch (Exception e) {
            // Log request failure
            long duration = System.currentTimeMillis() - startTime;
            LoggingUtils.error(logger, "Request failed: {} {} - {}", 
                httpRequest.getMethod(), httpRequest.getRequestURI(), e.getMessage(), e);
            
            LoggingUtils.logBusinessOperation(logger, "HTTP_REQUEST", "ERROR",
                "method", httpRequest.getMethod(),
                "uri", httpRequest.getRequestURI(),
                "status", "EXCEPTION",
                "duration", duration + "ms",
                "error", e.getClass().getSimpleName(),
                "errorMessage", e.getMessage());
            
            throw e;
        } finally {
            // Clean up MDC
            LoggingUtils.clearBusinessContext();
        }
    }

    /**
     * Extract domain from request for logging context
     */
    private String extractDomain(HttpServletRequest request) {
        String serverName = request.getServerName();
        if (serverName != null) {
            int dotIndex = serverName.indexOf('.');
            if (dotIndex > 0) {
                return serverName.substring(0, dotIndex);
            }
            return serverName;
        }
        return "unknown";
    }
}
