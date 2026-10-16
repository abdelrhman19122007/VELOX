package com.velox.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;

/** Search (Arabic / English, injection attempts) and favourites. */
class SearchAndFavoritesApiTest extends CatalogApiTestBase {

    @Test
    void searchFindsArabicAndEnglishNames() {
        String admin = admin();
        String marker = "حواوشي" + UUID.randomUUID().toString().substring(0, 4);
        String[] store = newStore(admin, "مطعم " + marker, "", "");
        newProduct(admin, store[0], "سندوتش " + marker, "SANDWICHES");

        JsonNode r = get("/api/search?q=" + marker, null).getBody();
        assertEquals(1, r.get("stores").size());
        assertEquals(1, r.get("products").size());
        assertTrue(get("/api/search?q=kofta", null).getBody().get("products").size() > 0, "English names, any case");
    }

    @Test
    void wildcardsAndInjectionDontDumpTheCatalogue() {
        String admin = admin();
        newStore(admin, "أي محل", "", "");
        assertEquals(0, get("/api/search?q=%25%25", null).getBody().get("stores").size(), "% is literal, not 'everything'");
        assertEquals(0, get("/api/search?q=__", null).getBody().get("stores").size(), "_ is literal");

        var sqli = get("/api/search?q=' OR '1'='1", null);
        assertEquals(HttpStatus.OK, sqli.getStatusCode());
        assertEquals(0, sqli.getBody().get("stores").size());
        assertEquals(0, get("/api/search?q=a", null).getBody().get("stores").size(), "too short");
    }

    @Test
    void favoritesBelongToTheirOwner() {
        String admin = admin();
        String[] store = newStore(admin, "مفضل", "", "");
        String alice = newCustomer(uniqueEmail(), "secret-123");
        String bob = newCustomer(uniqueEmail(), "secret-123");

        assertEquals(1, post("/api/me/favorites/store:" + store[0], null, alice).getBody().size());
        assertEquals(0, get("/api/me/favorites", bob).getBody().size());
        assertEquals(0, post("/api/me/favorites/store:" + store[0], null, alice).getBody().size(), "second tap removes it");
        assertEquals("INVALID_FAVORITE", post("/api/me/favorites/anything", null, alice).getBody().get("code").asText());
        assertEquals(HttpStatus.UNAUTHORIZED, get("/api/me/favorites", null).getStatusCode());
    }
}
