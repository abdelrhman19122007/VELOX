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

public class ProductDAO {

    // ميثود لجلب وعرض جميع المنتجات من قاعدة البيانات
    public void displayAllProducts() {
        String sql = "SELECT * FROM products";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            System.out.println(" --- Product List --- ");
            boolean hasProducts = false;
            
            while (rs.next()) {
                hasProducts = true;
                // افترضنا هنا أسماء الأعمدة (عدلها لو هدى كاتباها باسم مختلف زي title أو product_name)
                int id = rs.getInt("id"); 
                String name = rs.getString("name"); 
                double price = rs.getDouble("price");
                
                System.out.println("ID: " + id + " | Name: " + name + " | Price: " + price + " EGP");
            }
            
            if (!hasProducts) {
                System.out.println("لا توجد منتجات في قاعدة البيانات حالياً.");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ خطأ أثناء جلب المنتجات!");
            e.printStackTrace();
        }
    }

    // ميثود للاختبار السريع
    public static void main(String[] args) {
        ProductDAO productDAO = new ProductDAO();
        productDAO.displayAllProducts();
    }
}
