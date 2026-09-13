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
        // إضافة shipping_address والحقول الإضافية لتفادي أخطاء SQL NOT NULL
        String sql = "INSERT INTO orders (user_id, address_id, total_amount, final_amount, status, shipping_address) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, 1);                   // address_id
            stmt.setDouble(3, totalAmount);        // total_amount
            stmt.setDouble(4, totalAmount);        // final_amount
            stmt.setString(5, status);              // status
            stmt.setString(6, "Damietta, Egypt");  // shipping_address (عنوان تجريبي)
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1); // إرجاع رقم الطلب الجديد
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // في حالة الفشل
    }
    // ميثود لإضافة عناصر الطلب (Junction Table)
    public boolean addOrderItem(int orderId, int productId, int quantity, double price) {
        String sql = "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, orderId);
            stmt.setInt(2, productId);
            stmt.setInt(3, quantity);
            stmt.setDouble(4, price);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // اختبار سريع
    public static void main(String[] args) {
        OrderDAO orderDAO = new OrderDAO();
        
        // تجربة إنشاء طلب جديد لليوزر رقم 1
        int orderId = orderDAO.createOrder(1, 420.0, "PENDING");
        
        if (orderId != -1) {
            System.out.println("✅ Order Created Successfully! Order ID: " + orderId);
            // إضافة منتج للطلب (مثلاً الكشري بـ 120 والقميص بـ 300)
            orderDAO.addOrderItem(orderId, 3, 1, 120.0);
            orderDAO.addOrderItem(orderId, 4, 1, 300.0);
            System.out.println("✅ Order Items Added Successfully!");
        } else {
            System.out.println("❌ Failed to create order.");
        }
    }

}