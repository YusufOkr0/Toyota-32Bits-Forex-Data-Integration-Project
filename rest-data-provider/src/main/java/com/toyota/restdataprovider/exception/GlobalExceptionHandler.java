package com.toyota.restdataprovider.exception;



import com.toyota.restdataprovider.exception.security.InvalidAuthenticationHeaderException;
import com.toyota.restdataprovider.exception.security.InvalidTokenException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ExceptionResponse> handleAuthenticationExceptions(
            AuthenticationException ex,
            HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, request);
    }


    @ExceptionHandler({InvalidPricingPlanException.class,TakenEmailException.class, UsernameAlreadyExistsException.class})
    public ResponseEntity<ExceptionResponse> handleInvalidPricingPlanException(RuntimeException ex,HttpServletRequest request){
        return buildErrorResponse(ex, HttpStatus.CONFLICT,request);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest().body(errors);
    }



    private ResponseEntity<ExceptionResponse> buildErrorResponse(Exception ex,HttpStatus status,HttpServletRequest request){
        ExceptionResponse exceptionResponse = new ExceptionResponse(
                status.value(),
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(status)
                .body(exceptionResponse);
    }


}

