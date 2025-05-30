package com.toyota.restdataprovider.exception;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }



    @ExceptionHandler({
            InvalidPricingPlanException.class,
            TakenEmailException.class,
            UsernameAlreadyExistsException.class
    })
    public ResponseEntity<ExceptionResponse> handleInvalidPricingPlanException(RuntimeException ex,HttpServletRequest request){
        return buildErrorResponse(ex, HttpStatus.CONFLICT,request);
    }

    @ExceptionHandler(CurrencyPairNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleCurrencyPairNotFoundException(CurrencyPairNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ExceptionResponse> handleBadCredentialsException(BadCredentialsException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ExceptionResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.METHOD_NOT_ALLOWED, request);
    }







    private ResponseEntity<ExceptionResponse> buildErrorResponse(Exception ex,HttpStatus status,HttpServletRequest request){
        ExceptionResponse exceptionResponse = new ExceptionResponse(
                status.value(),
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now()
        );
        log.error("Exception caught: {} - URI: {}", ex.getMessage(), request.getRequestURI(), ex);

        return ResponseEntity
                .status(status)
                .body(exceptionResponse);
    }


}

