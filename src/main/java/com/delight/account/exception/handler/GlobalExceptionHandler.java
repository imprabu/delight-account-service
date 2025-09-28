package com.delight.account.exception.handler;

import com.delight.account.dto.ErrorResponse;
import com.delight.account.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        Locale locale = request.getLocale();
        String message = messageSource.getMessage(ex.getMessageKey(), ex.getArgs(), locale);
        
        // Determine appropriate HTTP status based on error type
        HttpStatus status = getHttpStatusForError(ex.getMessageKey());
        
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
        if (messageKey.contains("session.invalid") || messageKey.contains("session.notfound")) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (messageKey.contains("signup.failed")) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.BAD_REQUEST;
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<String> handleValidation(Exception ex, HttpServletRequest request) {
        Locale locale = request.getLocale();
        String message = messageSource.getMessage("error.validation", null, locale);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        Locale locale = request.getLocale();
        String message = messageSource.getMessage("error.validation", null, locale);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex, HttpServletRequest request) {
        Locale locale = request.getLocale();
        String message = messageSource.getMessage("error.validation", null, locale);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<String> handleHttpMediaTypeNotSupported(
            org.springframework.web.HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        Locale locale = request.getLocale();
        String message = messageSource.getMessage("error.validation", null, locale);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        Locale locale = request.getLocale();
        String message = messageSource.getMessage("error.signup.failed", null, locale);
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
