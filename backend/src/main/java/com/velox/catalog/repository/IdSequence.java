package com.velox.catalog.repository;

import org.springframework.jdbc.core.JdbcTemplate;

/** Readable ids with a type prefix (R1, R2… / F1, F2…): the next number after the highest one in use. */
final class IdSequence {

    private IdSequence() {
    }

    /** {@code table} is a constant from this package, never user input. */
    static String next(JdbcTemplate db, String table, String prefix) {
        Integer max = db.queryForObject("SELECT COALESCE(MAX(CAST(SUBSTRING(id FROM ?) AS INTEGER)), 0) FROM " + table
                + " WHERE id ~ ?", Integer.class, prefix.length() + 1, "^" + prefix + "[0-9]+$");
        return prefix + ((max == null ? 0 : max) + 1);
    }
}
