/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.dao;

/**
 *
 * @author 3bdelr7man
 */


import com.app.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class OrderDAO {

// ميثود لإنشاء طلب جديد وإرجاع الـ Order ID
    public int createOrder(int userId, double totalAmount, String status) {
        return createOrder(userId, 1, totalAmount, totalAmount, status, "");
    }

    public int createOrder(int userId, int addressId, double totalAmount, double finalAmount, String status, String shippingAddress) {
        if (userId <= 0 || totalAmount < 0 || finalAmount < 0) {
            return -1;
        }
        String safeStatus = (status == null || status.trim().isEmpty()) ? "PENDING" : status.trim().toUpperCase();
        String safeAddress = (shippingAddress == null || shippingAddress.trim().isEmpty()) ? "NOT_PROVIDED" : shippingAddress.trim();
        int safeAddressId = addressId <= 0 ? 1 : addressId;
        String sql = "INSERT INTO orders (user_id, address_id, total_amount, final_amount, status, shipping_address) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, safeAddressId);
            stmt.setDouble(3, totalAmount);
            stmt.setDouble(4, finalAmount);
            stmt.setString(5, safeStatus);
            stmt.setString(6, safeAddress);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1); // إرجاع رقم الطلب الجديد
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[OrderDAO] create order failed: " + e.getMessage());
        }
        return -1; // في حالة الفشل
    }
    // ميثود لإضافة عناصر الطلب (Junction Table)
    public boolean addOrderItem(int orderId, int productId, int quantity, double price) {
        if (orderId <= 0 || productId <= 0 || quantity <= 0 || price < 0) {
            return false;
        }
        String sql = "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, orderId);
            stmt.setInt(2, productId);
            stmt.setInt(3, quantity);
            stmt.setDouble(4, price);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[OrderDAO] add item failed: " + e.getMessage());
            return false;
        }
    }

}