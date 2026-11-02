package com.velox;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Base for tests that run the real app over HTTP against a real PostgreSQL (Testcontainers) with the real
 * migrations — the same schema as production. One database for the whole test run.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"velox.security.token-secret=test-secret-that-is-at-least-32-chars-long",
                "velox.bootstrap.admin-email=admin@velox.test", "velox.bootstrap.admin-password=admin-password-123"})
public abstract class IntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@velox.test";
    protected static final String ADMIN_PASSWORD = "admin-password-123";

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected TestRestTemplate http;
    @Autowired
    protected JdbcTemplate db;

    protected static String uniqueEmail() {
        return "u" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    protected ResponseEntity<JsonNode> call(HttpMethod method, String path, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return http.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    protected ResponseEntity<JsonNode> post(String path, Object body, String token) {
        return call(HttpMethod.POST, path, body, token);
    }

    protected ResponseEntity<JsonNode> get(String path, String token) {
        return call(HttpMethod.GET, path, null, token);
    }

    /** Registers a customer and returns their token. */
    protected String newCustomer(String email, String password) {
        return post("/api/auth/register", Map.of("name", "Test User", "email", email, "phone", "01012345678",
                "password", password), null).getBody().get("token").asText();
    }

    protected String login(String email, String password) {
        return post("/api/auth/login", Map.of("email", email, "password", password), null).getBody().get("token").asText();
    }
}
