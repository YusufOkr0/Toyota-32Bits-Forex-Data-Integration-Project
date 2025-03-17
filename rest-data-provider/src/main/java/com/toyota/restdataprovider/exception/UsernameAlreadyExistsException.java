package com.toyota.restdataprovider.exception;

public class UsernameAlreadyExistsException extends RuntimeException{
    public UsernameAlreadyExistsException(String message){
        super(message);
    }
}
