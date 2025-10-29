package com.delight.account.util;

import org.slf4j.Logger;
import org.slf4j.MDC;

/**
 * Utility class for standardized logging patterns across the application.
 * Provides consistent logging methods with structured information for better observability.
 * Log format: <Domain> - <request_id> - <account_id> - <class name> - <actual log>
 */
public class LoggingUtils {

    /**
     * Formats log message according to the standard format: <Domain> - <request_id> - <account_id> - <class name> - <actual log>
     */
    private static String formatLogMessage(Logger logger, String message) {
        String domain = MDC.get("domain");
        String requestId = MDC.get("requestId");
        String accountId = MDC.get("accountId");
        String className = logger.getName();
        
        // Extract simple class name from full package name
        if (className != null && className.contains(".")) {
            className = className.substring(className.lastIndexOf(".") + 1);
        }
        
        return String.format("%s - %s - %s - %s - %s",
            domain != null ? domain : "UNKNOWN",
            requestId != null ? requestId : "NO_REQUEST_ID",
            accountId != null ? accountId : "NO_ACCOUNT_ID",
            className != null ? className : "UNKNOWN_CLASS",
            message
        );
    }

    /**
     * Log a service method entry with parameters
     */
    public static void logMethodEntry(Logger logger, String methodName, Object... params) {
        if (logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Entering method: ").append(methodName);
            if (params.length > 0) {
                sb.append(" with parameters: ");
                for (int i = 0; i < params.length; i += 2) {
                    if (i + 1 < params.length) {
                        sb.append(params[i]).append("=").append(params[i + 1]);
                        if (i + 2 < params.length) {
                            sb.append(", ");
                        }
                    }
                }
            }
            logger.debug(formatLogMessage(logger, sb.toString()));
        }
    }

    /**
     * Log a service method exit with result
     */
    public static void logMethodExit(Logger logger, String methodName, Object result) {
        if (logger.isDebugEnabled()) {
            logger.debug(formatLogMessage(logger, "Exiting method: " + methodName + " with result: " + result));
        }
    }

    /**
     * Log a service method exit without result
     */
    public static void logMethodExit(Logger logger, String methodName) {
        if (logger.isDebugEnabled()) {
            logger.debug(formatLogMessage(logger, "Exiting method: " + methodName));
        }
    }

    /**
     * Log business operation with structured information
     */
    public static void logBusinessOperation(Logger logger, String operation, String status, Object... params) {
        StringBuilder sb = new StringBuilder();
        sb.append("Business operation: ").append(operation).append(" - Status: ").append(status);
        
        if (params.length > 0) {
            sb.append(" - Details: ");
            for (int i = 0; i < params.length; i += 2) {
                if (i + 1 < params.length) {
                    sb.append(params[i]).append("=").append(params[i + 1]);
                    if (i + 2 < params.length) {
                        sb.append(", ");
                    }
                }
            }
        }
        logger.info(formatLogMessage(logger, sb.toString()));
    }

    /**
     * Log security-related events with structured information
     */
    public static void logSecurityEvent(Logger logger, String event, String status, Object... params) {
        StringBuilder sb = new StringBuilder();
        sb.append("Security event: ").append(event).append(" - Status: ").append(status);
        
        if (params.length > 0) {
            sb.append(" - Details: ");
            for (int i = 0; i < params.length; i += 2) {
                if (i + 1 < params.length) {
                    sb.append(params[i]).append("=").append(params[i + 1]);
                    if (i + 2 < params.length) {
                        sb.append(", ");
                    }
                }
            }
        }
        logger.warn(formatLogMessage(logger, sb.toString()));
    }

    /**
     * Log performance metrics
     */
    public static void logPerformanceMetric(Logger logger, String operation, long durationMs, Object... params) {
        StringBuilder sb = new StringBuilder();
        sb.append("Performance metric: ").append(operation).append(" - Duration: ").append(durationMs).append("ms");
        if (params.length > 0) {
            sb.append(" - Details: ");
            for (int i = 0; i < params.length; i += 2) {
                if (i + 1 < params.length) {
                    sb.append(params[i]).append("=").append(params[i + 1]);
                    if (i + 2 < params.length) {
                        sb.append(", ");
                    }
                }
            }
        }
        logger.info(formatLogMessage(logger, sb.toString()));
    }

    /**
     * Log cache operations with structured information
     */
    public static void logCacheOperation(Logger logger, String operation, String cacheKey, String status, Object... params) {
        StringBuilder sb = new StringBuilder();
        sb.append("Cache operation: ").append(operation).append(" - Key: ").append(cacheKey).append(" - Status: ").append(status);
        
        if (params.length > 0) {
            sb.append(" - Details: ");
            for (int i = 0; i < params.length; i += 2) {
                if (i + 1 < params.length) {
                    sb.append(params[i]).append("=").append(params[i + 1]);
                    if (i + 2 < params.length) {
                        sb.append(", ");
                    }
                }
            }
        }
        logger.debug(formatLogMessage(logger, sb.toString()));
    }

    /**
     * Log database operations with structured information
     */
    public static void logDatabaseOperation(Logger logger, String operation, String table, String status, Object... params) {
        StringBuilder sb = new StringBuilder();
        sb.append("Database operation: ").append(operation).append(" - Table: ").append(table).append(" - Status: ").append(status);
        
        if (params.length > 0) {
            sb.append(" - Details: ");
            for (int i = 0; i < params.length; i += 2) {
                if (i + 1 < params.length) {
                    sb.append(params[i]).append("=").append(params[i + 1]);
                    if (i + 2 < params.length) {
                        sb.append(", ");
                    }
                }
            }
        }
        logger.debug(formatLogMessage(logger, sb.toString()));
    }

    /**
     * Standard info logging with the new format
     */
    public static void info(Logger logger, String message, Object... params) {
        if (params.length > 0) {
            message = String.format(message.replace("{}", "%s"), params);
        }
        logger.info(formatLogMessage(logger, message));
    }

    /**
     * Standard debug logging with the new format
     */
    public static void debug(Logger logger, String message, Object... params) {
        if (logger.isDebugEnabled()) {
            if (params.length > 0) {
                message = String.format(message.replace("{}", "%s"), params);
            }
            logger.debug(formatLogMessage(logger, message));
        }
    }

    /**
     * Standard warn logging with the new format
     */
    public static void warn(Logger logger, String message, Object... params) {
        if (params.length > 0) {
            message = String.format(message.replace("{}", "%s"), params);
        }
        logger.warn(formatLogMessage(logger, message));
    }

    /**
     * Standard error logging with the new format
     */
    public static void error(Logger logger, String message, Throwable throwable, Object... params) {
        if (params.length > 0) {
            message = String.format(message.replace("{}", "%s"), params);
        }
        logger.error(formatLogMessage(logger, message), throwable);
    }

    /**
     * Standard error logging with the new format (without throwable)
     */
    public static void error(Logger logger, String message, Object... params) {
        if (params.length > 0) {
            message = String.format(message.replace("{}", "%s"), params);
        }
        logger.error(formatLogMessage(logger, message));
    }

    /**
     * Add user context to MDC for correlation
     */
    public static void addUserContext(Long userId, Long accountId) {
        if (userId != null) {
            MDC.put("userId", userId.toString());
        }
        if (accountId != null) {
            MDC.put("accountId", accountId.toString());
        }
    }

    /**
     * Add domain context to MDC
     */
    public static void addDomainContext(String domain) {
        if (domain != null) {
            MDC.put("domain", domain);
        }
    }

    /**
     * Add request context to MDC
     */
    public static void addRequestContext(String requestId) {
        if (requestId != null) {
            MDC.put("requestId", requestId);
        }
    }

    /**
     * Add business context to MDC
     */
    public static void addBusinessContext(String operation, String entity) {
        if (operation != null) {
            MDC.put("operation", operation);
        }
        if (entity != null) {
            MDC.put("entity", entity);
        }
    }

    /**
     * Clear business context from MDC
     */
    public static void clearBusinessContext() {
        MDC.remove("operation");
        MDC.remove("entity");
    }

    /**
     * Log a 400 Bad Request error with full context
     */
    public static void logBadRequest(Logger logger, String message, Object... contextParams) {
        logBusinessOperation(logger, "BAD_REQUEST_ERROR", "ERROR", 
            "errorType", "BAD_REQUEST",
            "message", message,
            contextParams);
    }

    /**
     * Log a 500 Internal Server Error with full context and stack trace
     */
    public static void logInternalServerError(Logger logger, String message, Throwable throwable, Object... contextParams) {
        // Log the exception with stack trace
        error(logger, message, throwable);
        
        // Log additional business context
        logBusinessOperation(logger, "INTERNAL_SERVER_ERROR", "ERROR", 
            "errorType", "INTERNAL_SERVER_ERROR",
            "exceptionType", throwable.getClass().getSimpleName(),
            "exceptionMessage", throwable.getMessage(),
            contextParams);
    }

    /**
     * Log a validation error with detailed validation context
     */
    public static void logValidationError(Logger logger, String message, String validationField, String validationMessage, Object... contextParams) {
        logBusinessOperation(logger, "VALIDATION_ERROR", "ERROR", 
            "errorType", "VALIDATION_ERROR",
            "validationField", validationField,
            "validationMessage", validationMessage,
            "message", message,
            contextParams);
    }

    /**
     * Log an authentication/authorization error
     */
    public static void logAuthError(Logger logger, String message, String authType, String reason, Object... contextParams) {
        logBusinessOperation(logger, "AUTH_ERROR", "ERROR", 
            "errorType", "AUTH_ERROR",
            "authType", authType,
            "authReason", reason,
            "message", message,
            contextParams);
    }

    /**
     * Log a rate limiting error
     */
    public static void logRateLimitError(Logger logger, String message, String rateLimitType, int limit, int current, Object... contextParams) {
        logBusinessOperation(logger, "RATE_LIMIT_ERROR", "ERROR", 
            "errorType", "RATE_LIMIT_ERROR",
            "rateLimitType", rateLimitType,
            "rateLimit", limit,
            "currentRequests", current,
            "message", message,
            contextParams);
    }

    /**
     * Set error context in MDC for correlation
     */
    public static void setErrorContext(String errorCode, String errorType, int httpStatus) {
        MDC.put("errorCode", errorCode);
        MDC.put("errorType", errorType);
        MDC.put("httpStatus", String.valueOf(httpStatus));
    }

    /**
     * Clear error context from MDC
     */
    public static void clearErrorContext() {
        MDC.remove("errorCode");
        MDC.remove("errorType");
        MDC.remove("httpStatus");
    }
}