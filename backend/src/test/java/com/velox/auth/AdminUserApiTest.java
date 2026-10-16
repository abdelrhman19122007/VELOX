package com.velox.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.velox.IntegrationTest;

/** Admin account management, and privilege escalation attempts by normal accounts. */
class AdminUserApiTest extends IntegrationTest {

    private String userId(String token) {
        return get("/api/auth/me", token).getBody().get("id").asText();
    }

    @Test
    void customersCannotUseAdminEndpoints() {
        String customer = newCustomer(uniqueEmail(), "secret-123");
        assertEquals(HttpStatus.FORBIDDEN, get("/api/admin/users", customer).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                post("/api/admin/users/" + userId(customer) + "/reset-password", null, customer).getStatusCode());
    }

    @Test
    void blockingEndsSessionsAndSignIn() {
        String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String email = uniqueEmail();
        String victim = newCustomer(email, "secret-123");

        ResponseEntity<JsonNode> blocked = call(HttpMethod.PATCH, "/api/admin/users/" + userId(victim), Map.of("blocked", true), admin);
        assertEquals(HttpStatus.NO_CONTENT, blocked.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, get("/api/auth/me", victim).getStatusCode(), "session ends at once");
        assertEquals("BLOCKED", post("/api/auth/login", Map.of("email", email, "password", "secret-123"), null)
                .getBody().get("code").asText());

        Integer audited = db.queryForObject("SELECT COUNT(*) FROM audit_log WHERE action = 'USER_BLOCK'", Integer.class);
        assertTrue(audited != null && audited > 0, "sensitive action is audited");
    }

    @Test
    void temporaryPasswordWorksAndLiftsTheLock() {
        String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String email = uniqueEmail();
        String id = userId(newCustomer(email, "secret-123"));
        for (int i = 0; i < 5; i++) {
            post("/api/auth/login", Map.of("email", email, "password", "guess" + i), null);
        }
        String temp = post("/api/admin/users/" + id + "/reset-password", null, admin).getBody().get("password").asText();
        assertEquals(10, temp.length());
        assertEquals(HttpStatus.OK, post("/api/auth/login", Map.of("email", email, "password", temp), null).getStatusCode());
    }

    @Test
    void adminCannotBlockThemselves() {
        String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        assertEquals(HttpStatus.BAD_REQUEST,
                call(HttpMethod.PATCH, "/api/admin/users/" + userId(admin), Map.of("blocked", true), admin).getStatusCode());
    }
}
