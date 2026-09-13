package com.app.service;

import com.app.enums.OrderStatus;
import com.app.model.order.Order;

public class DeliverySimulator {
    public void startLiveTracking(Order order) {
        System.out.println("\n=========================================");
        System.out.println("        VELOX LIVE TRACKING SYSTEM");
        System.out.println("=========================================");

        try {
            update(order, OrderStatus.PROCESSING, "Order is being processed.");
            Thread.sleep(1200);
            update(order, OrderStatus.IN_TRANSIT, "Delivery agent is IN TRANSIT with order.");
            Thread.sleep(1200);
            update(order, OrderStatus.ARRIVED, "Delivery agent HAS ARRIVED at destination.");
            Thread.sleep(1200);
            update(order, OrderStatus.DELIVERED, "Order DELIVERED successfully.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Tracking interrupted.");
        }
    }

    private void update(Order order, OrderStatus status, String message) {
        order.setStatus(status);
        System.out.println("[VELOX Status]: " + message);
    }
}
