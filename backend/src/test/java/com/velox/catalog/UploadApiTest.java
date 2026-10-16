package com.velox.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

import com.fasterxml.jackson.databind.JsonNode;

/** Photo uploads: real images only, size-limited, served so a browser never executes them. */
class UploadApiTest extends CatalogApiTestBase {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 'I', 'H', 'D', 'R'};

    private ResponseEntity<JsonNode> upload(String token, String filename, byte[] content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        LinkedMultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        return http.postForEntity("/api/uploads", new HttpEntity<>(form, headers), JsonNode.class);
    }

    @Test
    void realImageIsStoredUnderARandomNameAndServedSafely() {
        ResponseEntity<JsonNode> r = upload(admin(), "../../etc/passwd.png", PNG);
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        String url = r.getBody().get("url").asText();
        assertTrue(url.matches("/uploads/[0-9a-f-]{36}\\.png"), "random name — the path in the file name is ignored: " + url);

        ResponseEntity<byte[]> served = http.getForEntity(url, byte[].class);
        assertEquals(HttpStatus.OK, served.getStatusCode());
        assertEquals(MediaType.IMAGE_PNG, served.getHeaders().getContentType());
        assertEquals("nosniff", served.getHeaders().getFirst("X-Content-Type-Options"));
    }

    @Test
    void htmlDisguisedAsImageIsRefused() {
        ResponseEntity<JsonNode> r = upload(admin(), "photo.png", "<html><script>alert(document.cookie)</script></html>".getBytes());
        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertEquals("NOT_AN_IMAGE", r.getBody().get("code").asText());
    }

    @Test
    void bigFilesAreRefused() {
        byte[] big = new byte[2 * 1024 * 1024 + 10];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        assertEquals("FILE_TOO_LARGE", upload(admin(), "big.png", big).getBody().get("code").asText());
    }

    @Test
    void customersCannotUpload() {
        assertEquals(HttpStatus.FORBIDDEN, upload(newCustomer(uniqueEmail(), "secret-123"), "a.png", PNG).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, upload(null, "a.png", PNG).getStatusCode());
    }
}
