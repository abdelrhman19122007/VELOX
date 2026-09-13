/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.util;

/**
 *
 * @author 3bdelr7man
 */


import java.sql.*;
import java.util.ArrayList;
import com.app.enums.Size;
import com.app.model.product.*;

public class StoreRepository {
public ArrayList<Product> getProductsByStoreId(int storeId) {
    ArrayList<Product> products = new ArrayList<>();
    String query = "SELECT id, name, price, description, size FROM products WHERE store_id = ?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(query)) {
        
        stmt.setInt(1, storeId);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            String id = String.valueOf(rs.getInt("id"));
            String name = rs.getString("name");
            double price = rs.getDouble("price");
            String description = rs.getString("description");
            
            String sizeStr = rs.getString("size");
            Size size = (sizeStr != null && !sizeStr.trim().isEmpty()) 
                        ? Size.valueOf(sizeStr.trim().toUpperCase()) 
                        : Size.MEDIUM;

            products.add(new FoodItem(id, name, price, description, size));
        }
    } catch (SQLException e) {
        System.out.println("Database Error: " + e.getMessage());
    }
    return products;
}
}