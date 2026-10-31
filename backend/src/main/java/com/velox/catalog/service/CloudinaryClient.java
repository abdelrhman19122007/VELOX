package com.velox.catalog.service;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Signed uploads to Cloudinary's REST API (no SDK). The key and secret come from CLOUDINARY_URL and never leave the
 * server; the photo was already checked by its magic bytes in {@link ImageStorage}.
 */
final class CloudinaryClient {

    private static final String FOLDER = "velox";

    private final String cloud;
    private final String key;
    private final String secret;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json = new ObjectMapper();

    private CloudinaryClient(String cloud, String key, String secret) {
        this.cloud = cloud;
        this.key = key;
        this.secret = secret;
    }

    /** cloudinary://API_KEY:API_SECRET@CLOUD_NAME */
    static CloudinaryClient parse(String url) {
        URI u = URI.create(url.trim());
        String[] auth = u.getRawUserInfo() == null ? new String[0] : u.getRawUserInfo().split(":", 2);
        if (!"cloudinary".equals(u.getScheme()) || auth.length != 2 || u.getHost() == null) {
            throw new IllegalStateException("CLOUDINARY_URL must look like cloudinary://key:secret@cloud");
        }
        return new CloudinaryClient(u.getHost(), auth[0], auth[1]);
    }

    /** Uploads the photo and returns its https address. */
    String upload(byte[] image, String ext) {
        try {
            long timestamp = Instant.now().getEpochSecond();
            String signature = sha1Hex("folder=" + FOLDER + "&timestamp=" + timestamp + secret);
            String boundary = "velox" + UUID.randomUUID().toString().replace("-", "");
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            part(body, boundary, "api_key", key);
            part(body, boundary, "timestamp", Long.toString(timestamp));
            part(body, boundary, "folder", FOLDER);
            part(body, boundary, "signature", signature);
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"photo." + ext
                    + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(image);
            body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.cloudinary.com/v1_1/" + cloud + "/image/upload"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode r = json.readTree(res.body());
            if (res.statusCode() >= 300 || !r.hasNonNull("secure_url")) {
                throw new IllegalStateException("Cloudinary refused the photo: " + res.statusCode());
            }
            return r.get("secure_url").asText();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Photo upload interrupted", e);
        } catch (java.io.IOException | java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not upload the photo", e);
        }
    }

    private static void part(ByteArrayOutputStream out, String boundary, String name, String value) throws java.io.IOException {
        out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n")
                .getBytes(StandardCharsets.UTF_8));
    }

    static String sha1Hex(String s) throws java.security.NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8)));
    }
}
