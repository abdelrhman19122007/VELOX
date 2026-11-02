package com.velox.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.velox.IntegrationTest;

/** The OpenAPI description the web, mobile and test members work from. */
class ApiDocsTest extends IntegrationTest {

    @Test
    void describesTheApiWithTokenAuth() {
        ResponseEntity<JsonNode> docs = get("/v3/api-docs", null);
        assertEquals(HttpStatus.OK, docs.getStatusCode());
        JsonNode body = docs.getBody();
        assertEquals("VELOX API", body.get("info").get("title").asText());
        assertTrue(body.get("paths").has("/api/auth/login"));
        assertEquals("bearer", body.get("components").get("securitySchemes").get("token").get("scheme").asText());
        assertFalse(body.toString().contains("\"name\":\"me\""), "the signed-in account is never a request parameter");
    }

    @Test
    void docsDoNotWeakenSecurityHeaders() {
        ResponseEntity<JsonNode> docs = get("/v3/api-docs", null);
        assertEquals("nosniff", docs.getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals("DENY", docs.getHeaders().getFirst("X-Frame-Options"));
    }
}
