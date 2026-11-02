package com.velox.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.velox.IntegrationTest;

/** Sign-up / sign-in over HTTP, including the attacks the auth module must withstand (security test cases). */
class AuthApiTest extends IntegrationTest {

    @Test
    void registerThenSignIn() {
        String email = uniqueEmail();
        ResponseEntity<JsonNode> reg = post("/api/auth/register", Map.of("name", "Sara", "email", email.toUpperCase(),
                "phone", "01012345678", "password", "first-pass"), null);
        assertEquals(HttpStatus.CREATED, reg.getStatusCode());
        JsonNode user = reg.getBody().get("user");
        assertEquals(email, user.get("email").asText(), "emails are stored lower-case");
        assertEquals("CUSTOMER", user.get("role").asText());
        assertNull(user.get("passwordHash"), "the hash never leaves the server");
        assertFalse(user.get("id").asText().matches("\\d+"), "internal numeric ids are never exposed");

        String token = login(email, "first-pass");
        assertEquals(email, get("/api/auth/me", token).getBody().get("email").asText());
    }

    @Test
    void duplicateEmailAndInvalidInputAreRefused() {
        String email = uniqueEmail();
        newCustomer(email, "first-pass");
        ResponseEntity<JsonNode> again = post("/api/auth/register", Map.of("name", "Xy", "email", email,
                "phone", "01012345678", "password", "first-pass"), null);
        assertEquals(HttpStatus.CONFLICT, again.getStatusCode());
        assertEquals("EMAIL_TAKEN", again.getBody().get("code").asText());

        ResponseEntity<JsonNode> bad = post("/api/auth/register", Map.of("name", "X", "email", "not-an-email",
                "phone", "123", "password", "short"), null);
        assertEquals(HttpStatus.BAD_REQUEST, bad.getStatusCode());
        assertNotNull(bad.getBody().get("details").get("phone"));
    }

    @Test
    void nobodyCanSignUpAsAdminOrStore() {
        for (String role : new String[] {"ADMIN", "RESTAURANT"}) {
            ResponseEntity<JsonNode> r = post("/api/auth/register", Map.of("name", "Eve", "email", uniqueEmail(),
                    "phone", "01012345678", "password", "evil-pass", "role", role), null);
            assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode(), role);
        }
    }

    @Test
    void wrongPasswordAndUnknownEmailLookTheSame() {
        String email = uniqueEmail();
        newCustomer(email, "right-pass");
        ResponseEntity<JsonNode> wrong = post("/api/auth/login", Map.of("email", email, "password", "wrong-pass"), null);
        ResponseEntity<JsonNode> unknown = post("/api/auth/login", Map.of("email", uniqueEmail(), "password", "x"), null);
        assertEquals(HttpStatus.UNAUTHORIZED, wrong.getStatusCode());
        assertEquals(wrong.getBody().get("code"), unknown.getBody().get("code"), "no account enumeration");
    }

    @Test
    void bruteForceLocksTheEmail() {
        String email = uniqueEmail();
        newCustomer(email, "right-pass");
        for (int i = 0; i < 5; i++) {
            assertEquals(HttpStatus.UNAUTHORIZED, post("/api/auth/login", Map.of("email", email, "password", "guess" + i), null).getStatusCode());
        }
        ResponseEntity<JsonNode> locked = post("/api/auth/login", Map.of("email", email, "password", "right-pass"), null);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, locked.getStatusCode(), "even the right password waits");
        assertEquals("TOO_MANY_LOGINS", locked.getBody().get("code").asText());
    }

    @Test
    void sqlInjectionInLoginIsJustAWrongLogin() {
        ResponseEntity<JsonNode> r = post("/api/auth/login", Map.of("email", "' OR '1'='1' --", "password", "' OR '1'='1"), null);
        assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode());
        assertEquals("BAD_LOGIN", r.getBody().get("code").asText());
    }

    @Test
    void protectedEndpointsNeedAValidToken() {
        assertEquals(HttpStatus.UNAUTHORIZED, get("/api/auth/me", null).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, get("/api/auth/me", "forged.0.9999999999.signature").getStatusCode());
    }

    @Test
    void logoutEverywhereKillsOldTokens() {
        String email = uniqueEmail();
        String token = newCustomer(email, "first-pass");
        assertEquals(HttpStatus.NO_CONTENT, post("/api/auth/logout-everywhere", null, token).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, get("/api/auth/me", token).getStatusCode());
        assertEquals(HttpStatus.OK, get("/api/auth/me", login(email, "first-pass")).getStatusCode());
    }

    @Test
    void securityHeadersAndNoLeakOnErrors() {
        ResponseEntity<JsonNode> r = get("/api/auth/me", null);
        assertEquals("nosniff", r.getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals("DENY", r.getHeaders().getFirst("X-Frame-Options"));
        assertEquals("no-store", r.getHeaders().getFirst("Cache-Control"));

        ResponseEntity<JsonNode> broken = call(org.springframework.http.HttpMethod.POST, "/api/auth/login", "{not json", null);
        assertEquals(HttpStatus.BAD_REQUEST, broken.getStatusCode());
        assertFalse(String.valueOf(broken.getBody()).contains("Exception"), "no stack traces or class names");
    }
}
