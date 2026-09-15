package com.app.dto;

import com.app.enums.OrderStatus;
import com.app.model.order.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * KAN-90: Purchase history item per order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderHistoryDto {
    private String orderId;
    private String userId;
    private OrderStatus status;
    private LocalDateTime orderDate;
    private int itemCount;
    private double total;
    private String city;
    /** Feature 3: future delivery time, null = ASAP. */
    private LocalDateTime scheduledFor;

    public static OrderHistoryDto from(Order order) {
        return new OrderHistoryDto(
                order.getOrderId(),
                order.getUserId(),
                order.getStatus(),
                order.getOrderDate(),
                order.getProducts().size(),
                order.calculateFinalTotal(),
                order.getCity(),
                null
        );
    }
}
