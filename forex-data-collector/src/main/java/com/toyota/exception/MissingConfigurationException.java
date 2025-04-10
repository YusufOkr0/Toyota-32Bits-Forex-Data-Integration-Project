package com.toyota.exception;

public class MissingConfigurationException extends RuntimeException{
    public MissingConfigurationException(String message){
        super(message);
    }
}
