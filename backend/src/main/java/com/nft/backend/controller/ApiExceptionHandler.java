package com.nft.backend.controller;

import com.nft.backend.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException exception,
            HttpServletRequest request) {
        int status = exception.getStatusCode().value();
        String reason = exception.getReason();
        String message = reason == null || reason.isBlank()
                ? exception.getStatusCode().toString()
                : reason;

        return ResponseEntity.status(exception.getStatusCode())
                .body(new ErrorResponse(
                        Instant.now(),
                        status,
                        exception.getStatusCode().toString(),
                        message,
                        request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map((error) -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Requête invalide.");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.toString(),
                        message,
                        request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unhandled API error on {}", request.getRequestURI(), exception);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(
                        Instant.now(),
                        500,
                        "500 INTERNAL_SERVER_ERROR",
                        "Une erreur serveur est survenue.",
                        request.getRequestURI()));
    }
}
