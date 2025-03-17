package com.toyota.restdataprovider.exception;


public class UserNotFoundException extends RuntimeException{
        public UserNotFoundException(String message){
            super(message);
        }
}
