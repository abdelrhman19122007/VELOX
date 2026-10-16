package com.app.service;

import com.app.dao.SessionDAO;

/**
 * Opaque-token auth backed by the MySQL {@code sessions} table, so sessions
 * survive restarts and can be revoked. Same static API as before.
 */
public final class AuthTokenStore {

    public static final long TTL_MS = 12L * 60 * 60 * 1000;

    private static final SessionDAO SESSIONS = new SessionDAO();

    private AuthTokenStore() {
    }

    public static String issue(String email) {
        return SESSIONS.create(email, TTL_MS);
    }

    /** Returns the account email for a valid header, or null. */
    public static String resolve(String authorizationHeader) {
        if (authorizationHeader == null) {
            return null;
        }
        String h = authorizationHeader.trim();
        if (h.regionMatches(true, 0, "Bearer ", 0, 7)) {
            h = h.substring(7).trim();
        }
        if (h.isEmpty()) {
            return null;
        }
        return SESSIONS.resolve(h);
    }

    public static void revoke(String token) {
        SESSIONS.revoke(token);
    }
}
