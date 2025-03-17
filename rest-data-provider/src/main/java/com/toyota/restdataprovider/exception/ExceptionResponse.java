package com.toyota.restdataprovider.exception;


import lombok.Data;

import java.time.LocalDateTime;


public record ExceptionResponse(
        int status,
        String message,
        String path,
        LocalDateTime timestamp
) {}
