package com.velox.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests: passwords are salted, slow-hashed and never stored in clear. */
class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashMatchesOnlyTheRightPassword() {
        String hash = hasher.hash("correct horse");
        assertTrue(hasher.matches("correct horse", hash));
        assertFalse(hasher.matches("correct horsE", hash));
        assertFalse(hasher.matches(null, hash));
        assertFalse(hasher.matches("x", "not-a-hash"));
    }

    @Test
    void samePasswordGivesDifferentHashesAndNoClearText() {
        String a = hasher.hash("velox12345");
        String b = hasher.hash("velox12345");
        assertNotEquals(a, b, "random salt");
        assertFalse(a.contains("velox12345"));
        assertTrue(a.startsWith("pbkdf2$" + PasswordHasher.ROUNDS + "$"));
    }

    @Test
    void temporaryPasswordsAreReadable() {
        String temp = hasher.randomReadable(10);
        assertEquals(10, temp.length());
        assertTrue(temp.matches("[a-z2-9]+"));
        assertFalse(temp.matches(".*[01ilo].*"));
    }
}
