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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    // 1. ميثود للتحقق من تسجيل الدخول (Login)
    public boolean authenticateUser(String email, String passwordHash) {
        String sql = "SELECT * FROM users WHERE email = ? AND password_hash = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email);
            stmt.setString(2, passwordHash);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // لو لقى يوزر بالبيانات دي بيرجع true
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. ميثود لتحديث رصيد اليوزر (Budget) بعد أي عملية شراء
    public boolean updateUserBudget(int userId, double newBudget) {
        String sql = "UPDATE users SET current_budget = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDouble(1, newBudget);
            stmt.setInt(2, userId);
            
            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
public static void main(String[] args) {
        UserDAO userDAO = new UserDAO();

        // تجربة تسجيل الدخول باستخدام بيانات اليوزر التجريبية الموجودة في الداتابيز
        String testEmail = "hoda@example.com";
        String testPassword = "test_hash_123";

        boolean isAuthenticated = userDAO.authenticateUser(testEmail, testPassword);

        if (isAuthenticated) {
            System.out.println("✅ User Authentication Successful for: " + testEmail);
        } else {
            System.out.println("❌ Authentication Failed! User not found or invalid credentials.");
        }
    }
}