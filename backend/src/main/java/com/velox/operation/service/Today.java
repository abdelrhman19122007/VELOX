package com.velox.operation.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** "Today" for the dashboards means today in Egypt, whatever time zone the server runs in. */
final class Today {

    static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");

    private Today() {
    }

    static Instant start(Clock clock) {
        return LocalDate.now(clock.withZone(CAIRO)).atStartOfDay(CAIRO).toInstant();
    }
}
