package com.raulbolivar.exception;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(String id) {
        super("users", "USER_NOT_FOUND", "User with id " + id + " not found");
    }
}
