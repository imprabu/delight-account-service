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
        ErrorResponse body = ErrorResponse.builder()
            .timestamp(OffsetDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .message(message)
            .code(ex.getMessageKey())
            .path(request.getRequestURI())
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleValidation(Exception ex, HttpServletRequest request) {
        Locale locale = request.getLocale();
        String message = messageSource.getMessage("error.validation", null, locale);
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
