package com.velox.catalog.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.velox.common.error.ApiException;
import com.velox.security.AuthUser;
import com.velox.security.Role;

/**
 * Photo uploads for stores and products (SRS NFR-SEC-09, OWASP A08). An upload is accepted only if its first bytes
 * really are JPEG / PNG / WebP — the file name and the declared content type are ignored, because both are chosen
 * by the attacker. Files get a random name (no path traversal, no overwriting), are capped at 2 MB, and are served
 * from /uploads with nosniff so a browser never runs them.
 */
@Service
public class ImageStorage {

    static final long MAX_BYTES = 2L * 1024 * 1024;

    private final Path dir;

    public ImageStorage(@Value("${velox.uploads.dir:uploads}") String dir) throws IOException {
        this.dir = Path.of(dir).toAbsolutePath().normalize();
        Files.createDirectories(this.dir);
    }

    public Path directory() {
        return dir;
    }

    /** Saves the image and returns its public URL path, e.g. {@code /uploads/3f2c….png}. */
    public String store(AuthUser user, MultipartFile file) {
        user.require(Role.ADMIN, Role.RESTAURANT);
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("EMPTY_FILE", "Choose a photo");
        }
        if (file.getSize() > MAX_BYTES) {
            throw ApiException.badRequest("FILE_TOO_LARGE", "Photos must be 2 MB or smaller");
        }
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(12);
            String ext = imageType(head);
            if (ext == null) {
                throw ApiException.badRequest("NOT_AN_IMAGE", "Only JPEG, PNG or WebP photos");
            }
            String name = UUID.randomUUID() + "." + ext;
            Path target = dir.resolve(name).normalize();
            if (!target.startsWith(dir)) {
                throw ApiException.badRequest("NOT_AN_IMAGE", "Bad file name");
            }
            try (InputStream all = file.getInputStream()) {
                Files.copy(all, target);
            }
            return "/uploads/" + name;
        } catch (IOException e) {
            throw new IllegalStateException("Could not save the upload", e);
        }
    }

    /** Recognises the image by its magic bytes; null when it isn't one we accept. */
    static String imageType(byte[] h) {
        if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (h.length >= 8 && Arrays.equals(Arrays.copyOf(h, 8),
                new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})) {
            return "png";
        }
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
