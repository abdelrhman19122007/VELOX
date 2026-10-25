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

    /** Failed sign-ins for an email since its last successful one (fixed SQL: no column names are built at run time). */
    private static final String EMAIL_FAILURES_SINCE_LAST_SUCCESS = "SELECT COUNT(*) FROM login_attempts a "
            + "WHERE a.email = ? AND NOT a.success AND a.at >= ? AND a.at > COALESCE("
            + "(SELECT MAX(s.at) FROM login_attempts s WHERE s.email = ? AND s.success), TIMESTAMP '1970-01-01')";
    private static final String IP_FAILURES = "SELECT COUNT(*) FROM login_attempts WHERE ip = ? AND NOT success AND at >= ?";

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
        if (count(EMAIL_FAILURES_SINCE_LAST_SUCCESS, email, since, email) >= MAX_FAILURES_PER_EMAIL
                || (ip != null && count(IP_FAILURES, ip, since) >= MAX_FAILURES_PER_IP)) {
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

    private int count(String sql, Object... args) {
        Integer n = db.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }
}
