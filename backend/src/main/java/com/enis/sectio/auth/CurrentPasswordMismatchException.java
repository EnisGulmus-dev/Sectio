package com.enis.sectio.auth;

public class CurrentPasswordMismatchException extends RuntimeException {

    public CurrentPasswordMismatchException() {
        super("Current password is incorrect");
    }
}
