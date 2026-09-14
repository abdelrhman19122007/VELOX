package com.app.service;

import com.app.enums.OrderStatus;
import com.app.model.order.Order;

// كلاس محاكاة التتبع اللحظي لحالة الطلب
public class DeliverySimulator {
    // تشغيل المحاكاة وتحديث الحالة زمنياً
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

    // تحديث حالة الطلب وطباعة رسالة التتبع
    private void update(Order order, OrderStatus status, String message) {
        order.setStatus(status);
        System.out.println("[VELOX Status]: " + message);
    }
}
