package com.app.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DB connection must always speak UTF-8 so Arabic survives the round-trip.
 */
class DatabaseConnectionTest {

    @Test
    void appendsUtf8ParamsToBareUrl() {
        String url = DatabaseConnection.withUtf8("jdbc:mysql://localhost:3306/velox_db");
        assertTrue(url.contains("characterEncoding=UTF-8"));
        assertTrue(url.contains("useUnicode=true"));
    }

    @Test
    void keepsExistingParamsUntouched() {
        String url = "jdbc:mysql://localhost:3306/velox_db?characterEncoding=UTF-8";
        assertEquals(url, DatabaseConnection.withUtf8(url));
    }

    @Test
    void appendsWithAmpersandWhenParamsExist() {
        String url = DatabaseConnection.withUtf8("jdbc:mysql://localhost:3306/velox_db?serverTimezone=UTC");
        assertTrue(url.contains("serverTimezone=UTC&useUnicode=true"));
    }
}
