package com.toyota.restdataprovider.exception;

public class CurrencyPairNotFoundException extends RuntimeException{
    public CurrencyPairNotFoundException(String message){
        super(message);
    }
}
