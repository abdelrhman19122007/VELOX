package com.app.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Feature 3: scheduled-order time validation.
 */
class ScheduleValidatorTest {

    @Test
    void blankMeansAsap() {
        assertNull(ScheduleValidator.parse(null));
        assertNull(ScheduleValidator.parse("  "));
    }

    @Test
    void futureTimeWithinAWeekAccepted() {
        LocalDateTime at = LocalDateTime.now().plusDays(2).withHour(18).withMinute(30);
        String raw = at.withSecond(0).withNano(0).toString();
        assertNotNull(ScheduleValidator.parse(raw));
    }

    @Test
    void pastTimeRejected() {
        String raw = LocalDateTime.now().minusHours(1).withSecond(0).withNano(0).toString();
        assertThrows(IllegalArgumentException.class, () -> ScheduleValidator.parse(raw));
    }

    @Test
    void tooFarAheadRejected() {
        String raw = LocalDateTime.now().plusDays(8).withSecond(0).withNano(0).toString();
        assertThrows(IllegalArgumentException.class, () -> ScheduleValidator.parse(raw));
    }

    @Test
    void garbageRejected() {
        assertThrows(IllegalArgumentException.class, () -> ScheduleValidator.parse("tomorrow-ish"));
    }
}
