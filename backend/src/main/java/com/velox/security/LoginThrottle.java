package com.velox.security;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.velox.common.error.ApiException;

/**
 * Brute-force protection (OWASP A07, SRS FR-AUTH-03). Every sign-in try is recorded in {@code login_attempts};
 * too many failures for one email or one network inside the window block further tries for a while.
 * Kept in the database so the limit survives restarts and covers several server instances.
 */
@Component
public class LoginThrottle {

    static final Duration WINDOW = Duration.ofMinutes(10);
    static final int MAX_FAILURES_PER_EMAIL = 5;
    static final int MAX_FAILURES_PER_IP = 30;

    private final JdbcTemplate db;
    private final Clock clock;

    public LoginThrottle(JdbcTemplate db, Clock clock) {
        this.db = db;
        this.clock = clock;
    }

    /** Throws 429 TOO_MANY_LOGINS when this email or network has failed too often recently. */
    public void check(String email, String ip) {
        Timestamp since = Timestamp.from(clock.instant().minus(WINDOW));
        if (failuresSinceLastSuccess("email", email, since) >= MAX_FAILURES_PER_EMAIL
                || (ip != null && failures("ip", ip, since) >= MAX_FAILURES_PER_IP)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_LOGINS",
                    "Too many wrong passwords, try again later", Map.of("minutes", WINDOW.toMinutes()));
        }
    }

    public void record(String email, String ip, boolean success) {
        db.update("INSERT INTO login_attempts (email, ip, success, at) VALUES (?,?,?,?)",
                email, ip, success, Timestamp.from(clock.instant()));
    }

    /** An admin password reset lifts the lock (it counts as a success). */
    public void clear(String email) {
        record(email, null, true);
    }

    private int failuresSinceLastSuccess(String column, String value, Timestamp since) {
        Integer n = db.queryForObject("SELECT COUNT(*) FROM login_attempts a WHERE a." + column + " = ? AND NOT a.success "
                + "AND a.at >= ? AND a.at > COALESCE((SELECT MAX(s.at) FROM login_attempts s WHERE s." + column
                + " = ? AND s.success), TIMESTAMP '1970-01-01')", Integer.class, value, since, value);
        return n == null ? 0 : n;
    }

    private int failures(String column, String value, Timestamp since) {
        Integer n = db.queryForObject("SELECT COUNT(*) FROM login_attempts WHERE " + column + " = ? AND NOT success AND at >= ?",
                Integer.class, value, since);
        return n == null ? 0 : n;
    }
}
