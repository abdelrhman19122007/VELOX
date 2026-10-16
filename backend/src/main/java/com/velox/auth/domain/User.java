package com.velox.auth.domain;

import java.time.Instant;

import com.velox.security.AuthUser;
import com.velox.security.Role;

/** An account as stored in the database ({@code users}). The password hash never leaves the auth module. */
public record User(long id, String publicId, String email, String name, String phone, Role role, String storeId,
                   String passwordHash, int tokenVersion, boolean blocked, Instant createdAt) {

    public AuthUser toAuthUser() {
        return new AuthUser(id, publicId, email, name, role, storeId);
    }
}
