package com.toyota.exception;

public class ClassLoadingException extends RuntimeException {
    public ClassLoadingException(String message, Throwable cause){
        super(message,cause);
    }

    public ClassLoadingException(String message){
        super(message);
    }
}
