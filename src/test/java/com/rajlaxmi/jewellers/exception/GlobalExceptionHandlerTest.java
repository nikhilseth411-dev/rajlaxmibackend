package com.rajlaxmi.jewellers.exception;

import com.rajlaxmi.jewellers.dto.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void malformedJsonReturnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleMalformedJson(
                new HttpMessageNotReadableException("bad json", new MockHttpInputMessage(new byte[0]))
        );

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Malformed JSON request.", response.getBody().getMessage());
        assertFalse(response.getBody().isSuccess());
    }

    @Test
    void missingParameterReturnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleMissingParameter(
                new MissingServletRequestParameterException("q", "String")
        );

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Required parameter 'q' is missing.", response.getBody().getMessage());
    }

    @Test
    void typeMismatchReturnsBadRequest() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleTypeMismatch(
                new MethodArgumentTypeMismatchException(
                        "not-a-number",
                        Long.class,
                        "id",
                        null,
                        new NumberFormatException("invalid")
                )
        );

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid value for 'id'.", response.getBody().getMessage());
    }
}
