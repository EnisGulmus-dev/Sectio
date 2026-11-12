package com.enis.sectio.user;

public enum Role {
    ADMIN,
    EMPLOYEE;

    public String asAuthority() {
        return "ROLE_" + name();
    }
}
