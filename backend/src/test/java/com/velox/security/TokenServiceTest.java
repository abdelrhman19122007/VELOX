package com.velox.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

/** Unit tests: signed tokens can't be forged, altered or used after they expire. */
class TokenServiceTest {

    private static final String SECRET = "unit-test-secret-at-least-32-characters";
    private final Clock now = Clock.fixed(Instant.parse("2026-10-16T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void validTokenCarriesAccountAndVersion() {
        TokenService tokens = new TokenService(SECRET, now);
        TokenService.Claims claims = tokens.verify(tokens.issue("Uabc", 3)).orElseThrow();
        assertEquals("Uabc", claims.publicId());
        assertEquals(3, claims.version());
    }

    @Test
    void tamperedTokenIsRejected() {
        TokenService tokens = new TokenService(SECRET, now);
        String[] parts = tokens.issue("Uabc", 0).split("\\.");
        // raise the version / change the account without the secret → signature no longer matches
        String forged = parts[0] + ".9." + parts[2] + "." + parts[3];
        assertTrue(tokens.verify(forged).isEmpty());
        assertTrue(tokens.verify("garbage").isEmpty());
        assertTrue(tokens.verify(null).isEmpty());
    }

    @Test
    void tokenFromAnotherSecretIsRejected() {
        String token = new TokenService("another-secret-of-at-least-32-characters", now).issue("Uabc", 0);
        assertTrue(new TokenService(SECRET, now).verify(token).isEmpty());
    }

    @Test
    void expiredTokenIsRejected() {
        String token = new TokenService(SECRET, now).issue("Uabc", 0);
        Clock later = Clock.offset(now, TokenService.LIFETIME.plus(Duration.ofMinutes(1)));
        assertTrue(new TokenService(SECRET, later).verify(token).isEmpty());
    }
}
