package com.samdoherty.aggregator.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Ensure errors conform to spec
 */
@ControllerAdvice
public class GlobalExceptionAdvice {

    @ExceptionHandler(SymbolNotFoundException.class)
    public ResponseEntity<ApiError> handleSymbolNotFoundException(
            SymbolNotFoundException ex,
            HttpServletRequest request) {

        return new ResponseEntity<>(ApiError.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Missing")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleValidationException(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        return ResponseEntity.badRequest().body(ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message(ex.getConstraintViolations().iterator().next().getMessage())
                .path(request.getRequestURI())
                .build());
    }
}
