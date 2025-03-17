package com.toyota.restdataprovider.exception;

public class TakenEmailException extends RuntimeException{
    public TakenEmailException(String message){
        super(message);
    }
}
