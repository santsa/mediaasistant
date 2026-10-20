package com.santsa.mediaassistant.exception;

import java.time.format.DateTimeParseException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<String> handleRestClientException(RestClientException ex) {
        log.error("An error occurred using External API: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("An unexpected error occurred. Please try again later.");
    }

    // yyyy-MM-dd
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<String> handleDatetimeParseException(DateTimeParseException ex) {
        log.error("Invalid dateformat: {}", ex.getMessage(), ex);
        return ResponseEntity.badRequest()
                .body("Invalid dateformat. Use yyyy-MM-dd.");
    }

}
