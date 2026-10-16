package com.velox.security;

/** The four kinds of account. Store and admin accounts are only created by an admin. */
public enum Role {
    CUSTOMER, RESTAURANT, DRIVER, ADMIN;

    public boolean canSelfRegister() {
        return this == CUSTOMER || this == DRIVER;
    }
}
