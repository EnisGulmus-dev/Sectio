package com.enis.sectio.auth;

public class AuthenticatedUserNotFoundException extends RuntimeException {

    public AuthenticatedUserNotFoundException() {
        super("Authenticated user no longer exists");
    }
}
