package com.velox.security;

/**
 * The four kinds of account. Only customers sign up themselves; drivers, store staff and admins are created by an
 * admin, so nobody can give themselves the driver screens (and other people's addresses) by ticking a box.
 */
public enum Role {
    CUSTOMER, RESTAURANT, DRIVER, ADMIN;

    public boolean canSelfRegister() {
        return this == CUSTOMER;
    }
}
