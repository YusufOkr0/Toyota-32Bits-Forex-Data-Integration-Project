package com.toyota.exception;

public class SubscriberLoadingException extends RuntimeException{
    public SubscriberLoadingException(String message, Throwable cause) {
        super(message, cause);
    }
}
