/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author 3bdelr7man
 */
package com.app.dao;

import com.app.util.DatabaseConnection;
import com.app.util.PasswordUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    // 1. ميثود للتحقق من تسجيل الدخول (Login) - تقارن hash وليس كلمة سر صريحة
    public boolean authenticateUser(String email, String passwordHash) {
        if (email == null || passwordHash == null || email.trim().isEmpty()) {
            return false;
        }
        String sql = "SELECT * FROM users WHERE email = ? AND password_hash = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email.trim());
            stmt.setString(2, toSha256(passwordHash));
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // لو لقى يوزر بالبيانات دي بيرجع true
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] auth failed: " + e.getMessage());
            return false;
        }
    }

    /** SHA-256 helper حتى لا تُقارن كلمات السر كنص صريح. للإنتاج يُفضل BCrypt. */
    public static String toSha256(String raw) {
        return PasswordUtil.sha256(raw);
    }

    // 2. ميثود لتحديث رصيد اليوزر (Budget) بعد أي عملية شراء
    public boolean updateUserBudget(int userId, double newBudget) {
        if (userId <= 0 || newBudget < 0) {
            return false;
        }
        String sql = "UPDATE users SET current_budget = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDouble(1, newBudget);
            stmt.setInt(2, userId);
            
            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] update budget failed: " + e.getMessage());
            return false;
        }
    }

    /** Full user row by email (null when absent). Keys: id, full_name, email, password_hash, phone_number, governorate. */
    public java.util.Map<String, Object> findFullByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String sql = "SELECT id, full_name, email, password_hash, phone_number, governorate,"
                + " current_budget, remaining_budget FROM users WHERE email = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", rs.getInt("id"));
                    m.put("full_name", rs.getString("full_name"));
                    m.put("email", rs.getString("email"));
                    m.put("password_hash", rs.getString("password_hash"));
                    m.put("phone_number", rs.getString("phone_number"));
                    m.put("governorate", rs.getString("governorate"));
                    m.put("current_budget", rs.getDouble("current_budget"));
                    m.put("remaining_budget", rs.getDouble("remaining_budget"));
                    return m;
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] find failed: " + e.getMessage());
        }
        return null;
    }

    /** Inserts a DB user, returning the generated id (or -1). */
    public int createUser(String fullName, String email, String passwordHash, String phone, String governorate) {
        String sql = "INSERT INTO users (full_name, email, password_hash, phone_number, governorate)"
                + " VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, fullName);
            stmt.setString(2, email.trim().toLowerCase());
            stmt.setString(3, passwordHash);
            stmt.setString(4, phone);
            stmt.setString(5, governorate);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] create failed: " + e.getMessage());
        }
        return -1;
    }

    /** Email owning a phone number (exact or digits-only match), or null. */
    public String findEmailByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        String sql = "SELECT email, phone_number FROM users";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String ph = rs.getString("phone_number");
                if (ph != null && (ph.trim().equals(phone.trim())
                        || ph.replaceAll("[^0-9]", "").equals(digits))) {
                    return rs.getString("email");
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] phone lookup failed: " + e.getMessage());
        }
        return null;
    }

    public boolean updatePasswordHash(int userId, String newHash) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE users SET password_hash = ? WHERE id = ?")) {
            stmt.setString(1, newHash);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] hash upgrade failed: " + e.getMessage());
            return false;
        }
    }

    /** Updates only the non-null profile fields. */
    public boolean updateProfile(int userId, String fullName, String phone, String governorate) {
        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (fullName != null) {
            sql.append("full_name = ?, ");
            params.add(fullName);
        }
        if (phone != null) {
            sql.append("phone_number = ?, ");
            params.add(phone);
        }
        if (governorate != null) {
            sql.append("governorate = ?, ");
            params.add(governorate);
        }
        if (params.isEmpty()) {
            return false;
        }
        sql.setLength(sql.length() - 2);
        sql.append(" WHERE id = ?");
        params.add(userId);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object v = params.get(i);
                if (v instanceof Integer n) {
                    stmt.setInt(i + 1, n);
                } else {
                    stmt.setString(i + 1, String.valueOf(v));
                }
            }
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] profile update failed: " + e.getMessage());
            return false;
        }
    }

    /** Saves a card reference (last 4 + brand only, never the full number). */
    public boolean saveCard(int userId, String holderName, String last4, String brand) {
        if (userId <= 0 || last4 == null || !last4.matches("\\d{4}")) {
            return false;
        }
        String sql = "INSERT INTO user_payment_cards (user_id, cardholder_name, last_4_digits, card_brand)"
                + " VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, holderName == null || holderName.isBlank() ? "" : holderName.trim());
            stmt.setString(3, last4);
            stmt.setString(4, brand == null ? "Unknown" : brand);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] save card failed: " + e.getMessage());
            return false;
        }
    }

    public java.util.List<java.util.Map<String, Object>> listCards(int userId) {
        java.util.List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
        String sql = "SELECT id, cardholder_name, last_4_digits, card_brand, created_at"
                + " FROM user_payment_cards WHERE user_id = ? ORDER BY id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", rs.getInt("id"));
                    m.put("cardholder_name", rs.getString("cardholder_name"));
                    m.put("last_4_digits", rs.getString("last_4_digits"));
                    m.put("card_brand", rs.getString("card_brand"));
                    out.add(m);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] list cards failed: " + e.getMessage());
        }
        return out;
    }

    /** Current wallet balance (remaining_budget). */
    public double getBalance(int userId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT remaining_budget FROM users WHERE id = ?")) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] balance read failed: " + e.getMessage());
        }
        return 0;
    }

    /** Adds funds to both lifetime and remaining budgets. */
    public boolean topUp(int userId, double amount) {
        if (userId <= 0 || amount <= 0) {
            return false;
        }
        String sql = "UPDATE users SET current_budget = current_budget + ?,"
                + " remaining_budget = remaining_budget + ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setDouble(2, amount);
            stmt.setInt(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] topup failed: " + e.getMessage());
            return false;
        }
    }

    /** Atomically debits the wallet only when funds suffice. */
    public boolean debit(int userId, double amount) {
        if (userId <= 0 || amount <= 0) {
            return false;
        }
        String sql = "UPDATE users SET remaining_budget = remaining_budget - ?"
                + " WHERE id = ? AND remaining_budget >= ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, amount);
            stmt.setInt(2, userId);
            stmt.setDouble(3, amount);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] debit failed: " + e.getMessage());
            return false;
        }
    }
}