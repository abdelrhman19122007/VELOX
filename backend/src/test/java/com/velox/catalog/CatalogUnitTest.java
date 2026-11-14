package com.velox.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.velox.catalog.domain.OpeningHours;
import com.velox.catalog.service.CatalogTestAccess;

/** Unit tests: opening hours (incl. after midnight), safe LIKE patterns, image recognition by magic bytes. */
class CatalogUnitTest {

    @Test
    void normalHours() {
        OpeningHours h = new OpeningHours(LocalTime.of(10, 0), LocalTime.of(23, 0));
        assertTrue(h.isOpenAt(LocalTime.of(10, 0)));
        assertTrue(h.isOpenAt(LocalTime.of(22, 59)));
        assertFalse(h.isOpenAt(LocalTime.of(23, 0)));
        assertFalse(h.isOpenAt(LocalTime.of(9, 59)));
    }

    @Test
    void hoursPastMidnight() {
        OpeningHours h = new OpeningHours(LocalTime.of(12, 0), LocalTime.of(2, 0));
        assertTrue(h.isOpenAt(LocalTime.of(23, 30)));
        assertTrue(h.isOpenAt(LocalTime.of(1, 59)));
        assertFalse(h.isOpenAt(LocalTime.of(2, 0)));
        assertFalse(h.isOpenAt(LocalTime.of(11, 0)));
    }

    @Test
    void noHoursMeansAlwaysOpen() {
        assertTrue(OpeningHours.ALWAYS.isOpenAt(LocalTime.of(4, 0)));
        assertTrue(new OpeningHours(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT).isOpenAt(LocalTime.NOON));
    }

    @Test
    void searchWildcardsAreLiteral() {
        assertEquals("50\\% off", CatalogTestAccess.escapeLike("50% off"));
        assertEquals("a\\_b\\\\c", CatalogTestAccess.escapeLike("a_b\\c"));
    }

    @Test
    void imagesAreRecognisedByContentNotName() {
        assertEquals("png", CatalogTestAccess.imageType(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0}));
        assertEquals("jpg", CatalogTestAccess.imageType(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
        assertEquals("webp", CatalogTestAccess.imageType("RIFF0000WEBP".getBytes()));
        assertNull(CatalogTestAccess.imageType("<html><script>".getBytes()), "HTML renamed to .png is refused");
        assertNull(CatalogTestAccess.imageType("GIF89a......".getBytes()));
    }
}
