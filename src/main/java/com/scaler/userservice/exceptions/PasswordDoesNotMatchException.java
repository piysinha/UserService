package com.scaler.userservice.exceptions;

public class PasswordDoesNotMatchException extends Exception{
    public PasswordDoesNotMatchException(String message) {
        super(message);
    }
}
