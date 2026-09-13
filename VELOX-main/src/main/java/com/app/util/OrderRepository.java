/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.util;
import com.app.model.order.Order;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author 3bdelr7man
 */

public class OrderRepository {
    private static final String FILE_PATH = "orders.dat";

    
    public static void saveOrders(List<Order> orders) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(orders);
        } catch (IOException e) {
            System.err.println("[Error] Failed to save orders: " + e.getMessage());
        }
    }

    
    @SuppressWarnings("unchecked")
    public static List<Order> loadOrders() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new ArrayList<>(); 
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_PATH))) {
            return (List<Order>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Error] Failed to load previous orders: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    // دالة لحفظ الفاتورة النصية في ملف مقروء
public static void saveReceiptText(String receiptText) {
    try (FileWriter fw = new FileWriter("invoices_history.txt", true);
         BufferedWriter bw = new BufferedWriter(fw);
         PrintWriter out = new PrintWriter(bw)) {
        out.println(receiptText);
        out.println("-----------------------------------------\n");
    } catch (IOException e) {
        System.err.println("[Error] Failed to save receipt text: " + e.getMessage());
    }
}
}
