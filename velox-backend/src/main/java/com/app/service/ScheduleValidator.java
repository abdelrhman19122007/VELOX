package com.app.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Feature 3 (scheduled orders): pure validation for the requested
 * delivery time. Null/blank means ASAP (current behavior).
 */
public final class ScheduleValidator {

    /** How far ahead a customer may schedule (days). */
    public static final int MAX_DAYS_AHEAD = 7;

    private ScheduleValidator() {
    }

    /**
     * @param raw ISO datetime from the client (e.g. 2026-09-20T18:30)
     * @return parsed time, or null for ASAP
     * @throws IllegalArgumentException when invalid, past, or too far ahead
     */
    public static LocalDateTime parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        LocalDateTime at;
        try {
            at = LocalDateTime.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid scheduled time, use YYYY-MM-DDTHH:MM.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!at.isAfter(now)) {
            throw new IllegalArgumentException("Scheduled time must be in the future.");
        }
        if (at.isAfter(now.plusDays(MAX_DAYS_AHEAD))) {
            throw new IllegalArgumentException("You can schedule up to 7 days ahead.");
        }
        return at.withSecond(0).withNano(0);
    }
}
