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
}