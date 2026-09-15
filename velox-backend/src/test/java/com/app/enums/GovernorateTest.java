package com.app.enums;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KAN-126: unit tests for the Governorate shipping-price table.
 */
class GovernorateTest {

    @Test
    void allTwentySevenGovernoratesExist() {
        assertEquals(27, Governorate.values().length);
    }

    @Test
    void codesAreUniqueFromOneToTwentySeven() {
        Set<Integer> codes = new HashSet<>();
        for (Governorate g : Governorate.values()) {
            assertTrue(codes.add(g.getCode()), "duplicate code: " + g);
            assertTrue(g.getCode() >= 1 && g.getCode() <= 27);
        }
    }

    @Test
    void coreZonePricesMatchDeliveryPricing() {
        assertEquals(20.0, Governorate.CAIRO.getShippingPrice());
        assertEquals(25.0, Governorate.GIZA.getShippingPrice());
        assertEquals(40.0, Governorate.ALEXANDRIA.getShippingPrice());
        assertEquals(60.0, Governorate.DAMIETTA.getShippingPrice());
    }

    @Test
    void everyGovernorateIsPricedWithPositiveDeliveryDays() {
        for (Governorate g : Governorate.values()) {
            assertTrue(g.getShippingPrice() > 0, g + " has no price");
            assertTrue(g.getDeliveryDays() >= 1, g + " has no delivery days");
            assertNotNull(g.getArabicName());
            assertFalse(g.getArabicName().isBlank());
        }
    }

    @Test
    void fromNameIsCaseInsensitiveAndTrims() {
        assertEquals(Governorate.CAIRO, Governorate.fromName("cairo"));
        assertEquals(Governorate.GIZA, Governorate.fromName("  Giza "));
        assertEquals(Governorate.KAFR_EL_SHEIKH, Governorate.fromName("kafr el sheikh"));
    }

    @Test
    void fromNameRejectsUnknownAndNull() {
        assertThrows(IllegalArgumentException.class, () -> Governorate.fromName("ATLANTIS"));
        assertThrows(IllegalArgumentException.class, () -> Governorate.fromName(null));
    }

    @Test
    void fromCodeRoundTrips() {
        assertEquals(Governorate.CAIRO, Governorate.fromCode(1));
        assertEquals(Governorate.SOHAG, Governorate.fromCode(27));
        assertThrows(IllegalArgumentException.class, () -> Governorate.fromCode(0));
        assertThrows(IllegalArgumentException.class, () -> Governorate.fromCode(28));
    }

    @Test
    void lenientLookupAcceptsLegacyMisspellings() {
        assertEquals(Governorate.MENOFIA, Governorate.fromNameLenient("MONUFIA"));
        assertEquals(Governorate.SHARKIA, Governorate.fromNameLenient("SHARQIA"));
        assertEquals(Governorate.ASYUT, Governorate.fromNameLenient("ASSIUT"));
        assertEquals(Governorate.MATROUH, Governorate.fromNameLenient("MATRUH"));
        assertThrows(IllegalArgumentException.class, () -> Governorate.fromNameLenient("NOWHERE"));
    }

    @Test
    void fromCodeOrNameAcceptsBothForms() {
        assertEquals(Governorate.FAYOUM, Governorate.fromCodeOrName("7"));
        assertEquals(Governorate.FAYOUM, Governorate.fromCodeOrName("fayoum"));
        assertThrows(IllegalArgumentException.class, () -> Governorate.fromCodeOrName(""));
        assertThrows(IllegalArgumentException.class, () -> Governorate.fromCodeOrName(null));
    }
}
