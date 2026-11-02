package com.velox.security;

import com.velox.common.error.ApiException;

/**
 * The signed-in account of the current request. {@code id} is the internal database id and is never sent to
 * clients; the API exposes {@code publicId}.
 */
public record AuthUser(long id, String publicId, String email, String name, Role role, String storeId) {

    /** Allow only these roles (403 otherwise). Every protected action calls this — access control on the server. */
    public AuthUser require(Role... allowed) {
        for (Role r : allowed) {
            if (role == r) {
                return this;
            }
        }
        throw ApiException.forbidden();
    }

    public boolean is(Role r) {
        return role == r;
    }
}
