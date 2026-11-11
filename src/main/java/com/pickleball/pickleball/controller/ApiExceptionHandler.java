package com.pickleball.pickleball.controller;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Turns the exceptions raised by the service into RFC 9457 problem details so the
 * single page app can show the reason ("Every court needs a winner before rotating")
 * instead of a generic failure message.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handle(ResponseStatusException ex) {
        String detail = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), detail);
    }
}
