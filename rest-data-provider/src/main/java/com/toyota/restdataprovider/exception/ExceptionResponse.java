package com.toyota.restdataprovider.exception;

import java.time.LocalDateTime;


public record ExceptionResponse(
        int status,
        String message,
        String path,
        LocalDateTime timestamp
) {}
