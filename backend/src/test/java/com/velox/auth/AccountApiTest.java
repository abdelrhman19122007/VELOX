package com.velox.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.velox.IntegrationTest;

/** Password change and saved addresses, including IDOR attempts between two accounts. */
class AccountApiTest extends IntegrationTest {

    private static Map<String, Object> address(String label, String zone) {
        Map<String, Object> a = new HashMap<>();
        a.put("label", label);
        a.put("zone", zone);
        a.put("address", "شارع الجيش، سمنود");
        a.put("landmark", "جنب الجامع الكبير");
        a.put("phone", "01012345678");
        return a;
    }

    @Test
    void changePasswordEndsOtherSessions() {
        String email = uniqueEmail();
        String oldToken = newCustomer(email, "first-pass");
        assertEquals("WRONG_PASSWORD", post("/api/me/password", Map.of("current", "nope", "password", "second-pass"), oldToken)
                .getBody().get("code").asText());

        ResponseEntity<JsonNode> changed = post("/api/me/password", Map.of("current", "first-pass", "password", "second-pass"), oldToken);
        assertEquals(HttpStatus.OK, changed.getStatusCode());
        String newToken = changed.getBody().get("token").asText();
        assertEquals(HttpStatus.UNAUTHORIZED, get("/api/auth/me", oldToken).getStatusCode(), "old sessions end");
        assertEquals(HttpStatus.OK, get("/api/auth/me", newToken).getStatusCode(), "this device stays signed in");
        assertEquals(HttpStatus.UNAUTHORIZED, post("/api/auth/login", Map.of("email", email, "password", "first-pass"), null).getStatusCode());
    }

    @Test
    void savedAddressesWithLandmarkAndZoneCheck() {
        String token = newCustomer(uniqueEmail(), "secret-123");
        ResponseEntity<JsonNode> added = post("/api/me/addresses", address("البيت", "SAMANNOUD"), token);
        assertEquals(HttpStatus.OK, added.getStatusCode());
        assertEquals("جنب الجامع الكبير", added.getBody().get(0).get("landmark").asText(), "Arabic stored intact");

        ResponseEntity<JsonNode> mars = post("/api/me/addresses", address("X", "MARS"), token);
        assertEquals(HttpStatus.BAD_REQUEST, mars.getStatusCode());
        assertEquals("UNKNOWN_ZONE", mars.getBody().get("code").asText());
    }

    @Test
    void oneAccountCannotDeleteAnothersAddress() {
        String owner = newCustomer(uniqueEmail(), "secret-123");
        long id = post("/api/me/addresses", address("Home", "SAMANNOUD"), owner).getBody().get(0).get("id").asLong();

        String attacker = newCustomer(uniqueEmail(), "secret-123");
        call(HttpMethod.DELETE, "/api/me/addresses/" + id, null, attacker);
        assertEquals(1, get("/api/me/addresses", owner).getBody().size(), "IDOR: the address is still there");
        assertEquals(0, get("/api/me/addresses", attacker).getBody().size(), "and the attacker can't list it");
    }
}
