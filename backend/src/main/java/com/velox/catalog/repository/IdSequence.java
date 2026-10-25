package com.velox.catalog.repository;

import org.springframework.jdbc.core.JdbcTemplate;

/** Readable ids with a type prefix (R1, R2… / F1, F2…): the next number after the highest one in use. */
final class IdSequence {

    private IdSequence() {
    }

    private static final String MAX_STORE_ID =
            "SELECT COALESCE(MAX(CAST(SUBSTRING(id FROM ?) AS INTEGER)), 0) FROM stores WHERE id ~ ?";
    private static final String MAX_PRODUCT_ID =
            "SELECT COALESCE(MAX(CAST(SUBSTRING(id FROM ?) AS INTEGER)), 0) FROM products WHERE id ~ ?";

    /** {@code table} is "stores" or "products"; the SQL is fixed text, never built from input. */
    static String next(JdbcTemplate db, String table, String prefix) {
        String sql = switch (table) {
            case "stores" -> MAX_STORE_ID;
            case "products" -> MAX_PRODUCT_ID;
            default -> throw new IllegalArgumentException("Unknown table " + table);
        };
        Integer max = db.queryForObject(sql, Integer.class, prefix.length() + 1, "^" + prefix + "[0-9]+$");
        return prefix + ((max == null ? 0 : max) + 1);
    }
}
