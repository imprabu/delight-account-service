package com.delight.account.exception.handler;

import java.time.OffsetDateTime;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.delight.account.dto.ErrorResponse;
import com.delight.account.exception.ApiException;
import com.delight.account.util.LoggingUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Handles ApiException - Custom business logic exceptions
     * 
     * @param ex The ApiException containing error details
     * @param request The HTTP request that caused the exception
     * @return ResponseEntity<ErrorResponse> - Structured error response with:
     *         - timestamp: When the error occurred
     *         - status: HTTP status code (400, 401, 429, 500)
     *         - error: HTTP status reason phrase
     *         - message: Localized error message for user
     *         - code: Machine-readable error code for client handling
     *         - path: Request URI where error occurred
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        Locale locale = request.getLocale();
        String message = messageSource.getMessage(ex.getMessageKey(), ex.getArgs(), locale);
        
        // Determine appropriate HTTP status based on error type
        HttpStatus status = getHttpStatusForError(ex.getMessageKey());
        
        // Log the API exception with enhanced context
        String argsString = ex.getArgs() != null ? String.join(",", 
            java.util.Arrays.stream(ex.getArgs()).map(Object::toString).toArray(String[]::new)) : "none";
        
        if (status.value() >= 500) {
            LoggingUtils.logInternalServerError(logger, "API Exception: " + message, ex, 
                "errorCode", ex.getMessageKey(), "args", argsString);
        } else {
            LoggingUtils.logBadRequest(logger, "API Exception: " + message, 
                "errorCode", ex.getMessageKey(), "args", argsString);
        }
        
        ErrorResponse body = ErrorResponse.builder()
            .timestamp(OffsetDateTime.now())
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .code(ex.getMessageKey())
            .path(request.getRequestURI())
            .build();
        return ResponseEntity.status(status).body(body);
    }
    
    private HttpStatus getHttpStatusForError(String messageKey) {
        // Invalid signin attempts -> 400 Bad Request (invalid credentials, inactive user, etc.)
        if (messageKey.contains("signin.invalid")) {
            return HttpStatus.BAD_REQUEST;
        }
        // Rate limit exceeded -> 429 Too Many Requests
        if (messageKey.contains("rate.limit")) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (messageKey.contains("session.invalid") || messageKey.contains("session.notfound")) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (messageKey.contains("signup.failed")) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.BAD_REQUEST;
    }

    /**
     * Handles validation exceptions - MethodArgumentNotValidException and ConstraintViolationException
     * 
     * @param ex The validation exception
     * @param request The HTTP request that caused the exception
     * @return ResponseEntity<ErrorResponse> - Structured error response for validation failures:
     *         - Status: 400 Bad Request
     *         - Body: Structured ErrorResponse with validation error message
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleValidation(Exception ex, HttpServletRequest request) {
        Locale locale = request.getLocale();
        String message = messageSource.getMessage("error.validation", null, locale);
        
        // Log validation errors with enhanced context
        LoggingUtils.logValidationError(logger, "Validation Error: " + message, 
            "validation", ex.getClass().getSimpleName(), ex.getMessage());
        
        ErrorResponse body = ErrorResponse.builder()
            .timestamp(OffsetDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message(message)
            .code("error.validation")
            .path(request.getRequestURI())
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        Locale locale = request.getLocale();
        String message = messageSource.getMessage("error.validation", null, locale);
        
        ErrorResponse body = ErrorResponse.builder()
            .timestamp(OffsetDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message(message)
            .code("error.validation.type")
            .path(request.getRequestURI())
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex, HttpServletRequest request) {
        Locale locale = request.getLocale();
        String message = messageSource.getMessage("error.validation", null, locale);
        
        ErrorResponse body = ErrorResponse.builder()
            .timestamp(OffsetDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message(message)
            .code("error.validation.body")
            .path(request.getRequestURI())
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupported(
            org.springframework.web.HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        Locale locale = request.getLocale();
        String message = messageSource.getMessage("error.validation", null, locale);
        
        ErrorResponse body = ErrorResponse.builder()
            .timestamp(OffsetDateTime.now())
            .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
            .error(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())
            .message(message)
            .code("error.validation.mediatype")
            .path(request.getRequestURI())
            .build();
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    /**
     * Handles all unhandled exceptions - Fallback exception handler
     * 
     * @param ex The unhandled exception
     * @param request The HTTP request that caused the exception
     * @return ResponseEntity<ErrorResponse> - Structured error response for internal server errors:
     *         - timestamp: When the error occurred
     *         - status: 500 Internal Server Error
     *         - error: "Internal Server Error"
     *         - message: Generic error message (not exposing internal details)
     *         - code: "error.internal" for client identification
     *         - path: Request URI where error occurred
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        Locale locale = request.getLocale();
        // Use a generic error message for internal server errors
        String message = messageSource.getMessage("error.internal", null, 
            "An internal server error occurred. Please try again later.", locale);
        
        // Log the general exception with enhanced context and stack trace
        LoggingUtils.logInternalServerError(logger, "Unhandled exception occurred at path: " + request.getRequestURI(), 
            ex, "errorCode", "error.internal", "exceptionType", ex.getClass().getSimpleName());
        
        ErrorResponse body = ErrorResponse.builder()
            .timestamp(OffsetDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
            .message(message)
            .code("error.internal")
            .path(request.getRequestURI())
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
