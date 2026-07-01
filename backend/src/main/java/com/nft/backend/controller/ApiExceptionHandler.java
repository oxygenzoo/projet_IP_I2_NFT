package com.nft.backend.controller;

import com.nft.backend.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

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
}
