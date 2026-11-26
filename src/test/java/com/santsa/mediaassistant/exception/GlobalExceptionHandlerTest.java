package com.santsa.mediaassistant.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.client.RestClientException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void restClientException_returns503WithGenericMessage() {
        ResponseEntity<String> response = handler.handleRestClientException(new RestClientException("FDA down"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isEqualTo("An unexpected error occurred. Please try again later.");
    }

    @Test
    void dateTimeParseException_returns400WithExpectedFormat() {
        DateTimeParseException ex = catchParse("01/12/2026");

        ResponseEntity<String> response = handler.handleDatetimeParseException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Invalid dateformat. Use yyyy-MM-dd.");
    }

    @Test
    void illegalArgumentException_returns400WithExceptionMessage() {
        ResponseEntity<String> response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("Not supported model: 'gpt'"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Not supported model: 'gpt'");
    }

    @Test
    void accessDeniedException_returns403() {
        ResponseEntity<String> response = handler.handleAccessDeniedException(new AccessDeniedException("nope"));

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isEqualTo("You don't have permission for this acction");
    }

    private static DateTimeParseException catchParse(String text) {
        try {
            LocalDate.parse(text);
            throw new AssertionError("expected parse failure");
        } catch (DateTimeParseException e) {
            return e;
        }
    }
}
