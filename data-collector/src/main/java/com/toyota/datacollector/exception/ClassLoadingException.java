package com.toyota.datacollector.exception;

public class ClassLoadingException extends RuntimeException {
    public ClassLoadingException(String message, Throwable cause){
        super(message,cause);
    }
}
