/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.app.service;

import com.app.model.order.Order;
import com.app.model.product.Product;
import java.util.Map;
/**
 * @author 3bdelr7man
 */
public class PackagingService {

    public static void packageOrder(Order order) {
        System.out.println("\n==========================================");
        System.out.println("[Packaging] Starting packaging process for Order...");

        // التحقق من وجود منتجات داخل الطلب وطباعة تفاصيلها
        if (order.getProducts() != null && !order.getProducts().isEmpty()) {
            System.out.println("[Packaging] Items packed in this order:");
            for (Product product : order.getProducts()) {
                System.out.println(" -> " + product.getName());
            }
        }
    }
}