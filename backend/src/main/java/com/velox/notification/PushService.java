package com.velox.notification;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Push notifications through Firebase Cloud Messaging (HTTP v1), so phones hear about orders with the app closed.
 * On when two files exist in the Firebase folder (default ./.firebase — never committed):
 * {@code web-config.json} (public app config + vapidKey) and {@code service-account.json} (the private key).
 * Without them everything works and nothing is pushed.
 */
@Service
public class PushService {

    private static final Logger log = LoggerFactory.getLogger(PushService.class);
    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    /** A notification in both languages; each device gets its own language. */
    public record Message(String titleAr, String bodyAr, String titleEn, String bodyEn, String orderCode) {
    }

    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ExecutorService sender = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "push-sender");
        t.setDaemon(true);
        return t;
    });
    private final JsonNode webConfig;
    private final JsonNode serviceAccount;
    private String accessToken;
    private Instant accessTokenExpires = Instant.EPOCH;

    /**
     * The two settings come from environment variables holding the JSON itself (for hosts without a disk:
     * VELOX_FIREBASE_WEB_CONFIG, VELOX_FIREBASE_SERVICE_ACCOUNT), or else from the two files in the Firebase folder.
     */
    public PushService(JdbcTemplate db, ObjectMapper json, @Value("${velox.firebase.dir:.firebase}") String dir,
                       @Value("${VELOX_FIREBASE_WEB_CONFIG:}") String webConfigJson,
                       @Value("${VELOX_FIREBASE_SERVICE_ACCOUNT:}") String serviceAccountJson) {
        this.db = db;
        this.json = json;
        this.webConfig = webConfigJson.isBlank() ? read(Path.of(dir, "web-config.json")) : parse("VELOX_FIREBASE_WEB_CONFIG", webConfigJson);
        this.serviceAccount = serviceAccountJson.isBlank() ? read(Path.of(dir, "service-account.json"))
                : parse("VELOX_FIREBASE_SERVICE_ACCOUNT", serviceAccountJson);
        if (enabled()) {
            log.info("Push notifications on (Firebase project {})", serviceAccount.path("project_id").asText());
        }
    }

    public boolean enabled() {
        return webConfig != null && serviceAccount != null;
    }

    /** The public Firebase settings the apps need to subscribe, or null when push is off. */
    public Map<String, String> clientConfig() {
        if (!enabled()) {
            return null;
        }
        Map<String, String> c = new LinkedHashMap<>();
        for (String k : List.of("apiKey", "authDomain", "projectId", "storageBucket", "messagingSenderId", "appId", "androidAppId", "vapidKey")) {
            if (webConfig.hasNonNull(k)) {
                c.put(k, webConfig.get(k).asText());
            }
        }
        return c;
    }

    public void register(long userId, String token, String platform, String lang) {
        String p = List.of("web", "android", "ios").contains(platform) ? platform : "android";
        String l = "en".equals(lang) ? "en" : "ar";
        if (db.update("UPDATE push_devices SET user_id = ?, platform = ?, lang = ?, updated_at = CURRENT_TIMESTAMP WHERE token = ?",
                userId, p, l, token) == 0) {
            db.update("INSERT INTO push_devices (token, user_id, platform, lang) VALUES (?,?,?,?)", token, userId, p, l);
        }
    }

    public void unregister(long userId, String token) {
        db.update("DELETE FROM push_devices WHERE token = ? AND user_id = ?", token, userId);
    }

    /** Sends to every device of these accounts, off the request thread (an order never waits for Google). */
    public void send(Collection<Long> userIds, Message m) {
        if (!enabled() || userIds.isEmpty()) {
            return;
        }
        for (Long userId : userIds.stream().distinct().toList()) {
            db.query("SELECT token, lang FROM push_devices WHERE user_id = ?", rs -> {
                String token = rs.getString(1);
                boolean en = "en".equals(rs.getString(2));
                sender.submit(() -> deliver(token, en ? m.titleEn() : m.titleAr(), en ? m.bodyEn() : m.bodyAr(), m.orderCode()));
            }, userId);
        }
    }

    private void deliver(String token, String title, String body, String orderCode) {
        try {
            Map<String, Object> message = Map.of("token", token, "notification", Map.of("title", title, "body", body),
                    "data", Map.of("orderCode", orderCode == null ? "" : orderCode),
                    "android", Map.of("priority", "high"));
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://fcm.googleapis.com/v1/projects/"
                            + serviceAccount.path("project_id").asText() + "/messages:send"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + accessToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("message", message))))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 404 || res.body().contains("UNREGISTERED")) {
                db.update("DELETE FROM push_devices WHERE token = ?", token);   // app uninstalled
            } else if (res.statusCode() >= 300) {
                log.warn("Push failed: {} {}", res.statusCode(), res.body());
            }
        } catch (Exception e) {
            log.warn("Push failed: {}", e.getMessage());
        }
    }

    private synchronized String accessToken() throws Exception {
        if (accessToken != null && Instant.now().isBefore(accessTokenExpires)) {
            return accessToken;
        }
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=" + URLEncoder.encode(
                        "urn:ietf:params:oauth:grant-type:jwt-bearer", StandardCharsets.UTF_8) + "&assertion="
                        + signedJwt(Instant.now().getEpochSecond())))
                .build();
        JsonNode token = json.readTree(http.send(req, HttpResponse.BodyHandlers.ofString()).body());
        if (!token.hasNonNull("access_token")) {
            throw new IllegalStateException("Google refused the service account");
        }
        accessToken = token.get("access_token").asText();
        accessTokenExpires = Instant.now().plusSeconds(token.path("expires_in").asLong(3600) - 120);
        return accessToken;
    }

    /** The service account's token request: a JWT signed with its private key (RS256). */
    String signedJwt(long now) throws Exception {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String header = b64.encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String claims = b64.encodeToString(json.writeValueAsBytes(Map.of(
                "iss", serviceAccount.path("client_email").asText(), "scope", SCOPE,
                "aud", "https://oauth2.googleapis.com/token", "iat", now, "exp", now + 3600)));
        String pem = serviceAccount.path("private_key").asText()
                .replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "").replaceAll("\\s", "");
        PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
        Signature rsa = Signature.getInstance("SHA256withRSA");
        rsa.initSign(key);
        rsa.update((header + "." + claims).getBytes(StandardCharsets.UTF_8));
        return header + "." + claims + "." + b64.encodeToString(rsa.sign());
    }

    private JsonNode parse(String name, String value) {
        try {
            return json.readTree(value);
        } catch (Exception e) {
            log.warn("{} is not valid JSON - push stays off", name);   // never log the value: it may hold the private key
            return null;
        }
    }

    private JsonNode read(Path file) {
        try {
            return Files.exists(file) ? json.readTree(Files.readString(file)) : null;
        } catch (Exception e) {
            log.warn("Can't read {}: {}", file, e.getMessage());
            return null;
        }
    }
}
