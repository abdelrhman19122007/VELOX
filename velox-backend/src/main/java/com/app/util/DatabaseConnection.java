/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.util;

/**
 *
 * @author 3bdelr7man
 */


import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseConnection {
    private static String URL;
    private static String USER;
    private static String PASSWORD;
    private static volatile boolean useLocalFallback;
    private static final String LOCAL_URL = "jdbc:h2:file:./data/velox-local;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE";

    static {
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("application.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                System.out.println("Sorry, unable to find application.properties");
            } else {
                prop.load(input);
                // Env vars take precedence over properties file (never commit secrets)
                URL = firstNonEmpty(System.getenv("VELOX_DB_URL"), prop.getProperty("db.url"), "jdbc:mysql://localhost:3306/velox_db");
                USER = firstNonEmpty(System.getenv("VELOX_DB_USER"), prop.getProperty("db.user"), "root");
                PASSWORD = firstNonEmpty(System.getenv("VELOX_DB_PASSWORD"), prop.getProperty("db.password"), "");
            }
        } catch (Exception e) {
            System.err.println("[DB] Failed to load DB config: " + e.getMessage());
        }
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return "";
    }

    public static Connection getConnection() throws SQLException {
        if (URL == null || URL.isEmpty()) {
            throw new SQLException("DB URL is not configured. Set VELOX_DB_URL or db.url");
        }
        if (!useLocalFallback) {
            try {
                return DriverManager.getConnection(withUtf8(URL), USER, PASSWORD);
            } catch (SQLException mysqlError) {
                useLocalFallback = true;
                System.err.println("[DB] MySQL unavailable; using the persistent local VELOX database.");
            }
        }
        Connection connection = DriverManager.getConnection(LOCAL_URL, "sa", "");
        initializeLocalSchema(connection);
        return connection;
    }

    private static void initializeLocalSchema(Connection connection) throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS users ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, full_name VARCHAR(150) NOT NULL, "
                    + "email VARCHAR(255) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL, "
                    + "phone_number VARCHAR(30) NOT NULL UNIQUE, governorate VARCHAR(60) NOT NULL, "
                    + "role VARCHAR(30) DEFAULT 'CUSTOMER', is_verified TINYINT DEFAULT 0, "
                    + "is_active TINYINT DEFAULT 0, current_budget DECIMAL(12,2) DEFAULT 0, "
                    + "remaining_budget DECIMAL(12,2) DEFAULT 0, loyalty_rewards_consumed INT DEFAULT 0)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS orders ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, "
                    + "status VARCHAR(30) DEFAULT 'PENDING', is_returned TINYINT DEFAULT 0)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS otp_codes ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(255) NOT NULL, "
                    + "code_hash VARCHAR(255) NOT NULL, expires_at TIMESTAMP NOT NULL, "
                    + "attempts INT DEFAULT 0, used TINYINT DEFAULT 0)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS sessions ("
                    + "token VARCHAR(128) PRIMARY KEY, email VARCHAR(255) NOT NULL, "
                    + "expires_at TIMESTAMP NOT NULL)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS local_orders ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, order_code VARCHAR(40) UNIQUE, "
                    + "email VARCHAR(255) NOT NULL, total_amount DECIMAL(12,2) DEFAULT 0, "
                    + "governorate VARCHAR(60), payment_method VARCHAR(40), status VARCHAR(30) DEFAULT 'PENDING', "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
    }

    public static boolean isLocalFallback() {
        return useLocalFallback;
    }

    /** Forces UTF-8 on the wire so Arabic text survives the round-trip. */
    static String withUtf8(String url) {
        if (url.contains("characterEncoding=")) {
            return url;
        }
        return url + (url.contains("?") ? "&" : "?")
                + "useUnicode=true&characterEncoding=UTF-8&characterSetResults=UTF-8&connectionCollation=utf8mb4_unicode_ci";
    }
}
