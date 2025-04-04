package com.toyota.exception;

public class ConfigFileLoadingException extends RuntimeException{
    public ConfigFileLoadingException(String message) {
        super(message);
    }
}
