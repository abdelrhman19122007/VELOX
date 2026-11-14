package com.velox.catalog.domain;

import java.math.BigDecimal;

/** A shop selling through VELOX ({@code stores}). */
public record Store(String id, StoreType type, String cuisine, String name, String nameAr, String tagline,
                    String taglineAr, String phone, String address, String photoUrl, OpeningHours hours,
                    boolean accepting, Double lat, Double lng, BigDecimal commission) {

    /** Orderable right now: inside its hours and its own "accepting orders" switch is on (FR-CAT-05). */
    public boolean isOrderableAt(java.time.LocalTime time) {
        return accepting && hours.isOpenAt(time);
    }
}
