package com.enis.sectio.auth;

public class PasswordPolicyViolationException extends RuntimeException {

    public PasswordPolicyViolationException() {
        super("New password must contain between 12 and 72 characters");
    }
}
