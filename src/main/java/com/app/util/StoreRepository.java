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
    if (storeId <= 0) {
        return products;
    }
    String query = "SELECT id, name, price, description, size, product_type FROM products WHERE store_id = ? AND is_available = 1";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(query)) {
        
        stmt.setInt(1, storeId);
        try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
            String id = String.valueOf(rs.getInt("id"));
            String name = rs.getString("name");
            double price = rs.getDouble("price");
            String description = rs.getString("description");
            if (description == null) {
                description = "";
            }
            
            String sizeStr = rs.getString("size");
            Size size;
            try {
                size = (sizeStr != null && !sizeStr.trim().isEmpty())
                        ? Size.valueOf(sizeStr.trim().toUpperCase())
                        : Size.MEDIUM;
            } catch (IllegalArgumentException ex) {
                size = Size.MEDIUM;
            }

            String typeStr = rs.getString("product_type");
            if (typeStr == null) {
                typeStr = "FOOD";
            }
            switch (typeStr.trim().toUpperCase()) {
                case "CLOTHES" -> products.add(new ClothingItem(id, name, price, description, size));
                case "TECH" -> products.add(new ElectronicsItem(id, name, price, description, size));
                default -> products.add(new FoodItem(id, name, price, description, size));
            }
        }
        }
    } catch (SQLException e) {
        System.err.println("[StoreRepository] Database Error: " + e.getMessage());
    }
    return products;
}
}