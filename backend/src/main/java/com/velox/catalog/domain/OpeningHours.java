package com.velox.catalog.domain;

import java.time.LocalTime;

/**
 * When a store works. {@code null} hours = no published hours (online shops, 24 h pharmacies).
 * Handles late-night hours that end after midnight (e.g. 12:00 → 02:00), common for restaurants in Samannoud.
 */
public record OpeningHours(LocalTime opens, LocalTime closes) {

    public static final OpeningHours ALWAYS = new OpeningHours(null, null);

    public boolean isOpenAt(LocalTime time) {
        if (opens == null || closes == null) {
            return true;
        }
        if (opens.equals(closes)) {
            return true;                                   // 24 hours
        }
        if (opens.isBefore(closes)) {
            return !time.isBefore(opens) && time.isBefore(closes);
        }
        return !time.isBefore(opens) || time.isBefore(closes); // past midnight
    }
}
