package com.velox.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Stateless session tokens: {@code base64(publicId).version.expiry.signature}, signed with HMAC-SHA256.
 * The version is the account's {@code token_version}: raising it ends every session of that account
 * ("sign out everywhere", password change, block). The secret comes from the environment, never the code.
 */
@Component
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    static final Duration LIFETIME = Duration.ofDays(30);

    /** What a valid token says; the caller still checks the version against the database. */
    public record Claims(String publicId, int version) {
    }

    private final byte[] secret;
    private final Clock clock;

    public TokenService(@Value("${velox.security.token-secret:}") String secret, Clock clock) {
        this.clock = clock;
        if (secret == null || secret.length() < 32) {
            log.warn("VELOX_TOKEN_SECRET is missing or shorter than 32 characters — using a random one: "
                    + "sessions end when the server restarts. Set it in production.");
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            this.secret = random;
        } else {
            this.secret = secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String issue(String publicId, int version) {
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString(publicId.getBytes(StandardCharsets.UTF_8));
        long expires = clock.instant().plus(LIFETIME).getEpochSecond();
        String payload = id + "." + version + "." + expires;
        return payload + "." + sign(payload);
    }

    public Optional<Claims> verify(String token) {
        if (token == null) {
            return Optional.empty();
        }
        String[] p = token.split("\\.");
        if (p.length != 4) {
            return Optional.empty();
        }
        String payload = p[0] + "." + p[1] + "." + p[2];
        if (!MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.UTF_8), p[3].getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }
        try {
            if (Long.parseLong(p[2]) < clock.instant().getEpochSecond()) {
                return Optional.empty();
            }
            String publicId = new String(Base64.getUrlDecoder().decode(p[0]), StandardCharsets.UTF_8);
            return Optional.of(new Claims(publicId, Integer.parseInt(p[1])));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC unavailable", e);
        }
    }
}
