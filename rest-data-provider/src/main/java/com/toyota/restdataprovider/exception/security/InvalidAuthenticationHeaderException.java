package com.toyota.restdataprovider.exception.security;

import org.springframework.security.core.AuthenticationException;

public class InvalidAuthenticationHeaderException extends AuthenticationException {
    public InvalidAuthenticationHeaderException(String msg) {
        super(msg);
    }
}
