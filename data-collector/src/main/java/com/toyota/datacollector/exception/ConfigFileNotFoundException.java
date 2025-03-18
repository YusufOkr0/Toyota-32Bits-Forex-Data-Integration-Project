package com.toyota.datacollector.exception;

public class ConfigFileNotFoundException extends RuntimeException{
    public ConfigFileNotFoundException(String message){
        super(message);
    }
}
