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
import java.util.Properties;

public class DatabaseConnection {
    private static String URL;
    private static String USER;
    private static String PASSWORD;

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
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}