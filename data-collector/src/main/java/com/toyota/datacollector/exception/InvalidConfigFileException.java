package com.toyota.datacollector.exception;

public class InvalidConfigFileException extends RuntimeException{
    public InvalidConfigFileException(String message){
        super(message);
    }
}
