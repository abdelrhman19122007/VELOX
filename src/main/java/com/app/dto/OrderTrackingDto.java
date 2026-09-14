package com.app.dto;

import com.app.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * KAN-90: Real-time tracking response.
 * Timeline follows: PENDING -> PROCESSING -> IN_TRANSIT -> SHIPPED -> ARRIVED -> DELIVERED
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingDto {
    private String orderId;
    private OrderStatus currentStatus;
    private LocalDateTime lastUpdated;
    private List<TrackingStep> timeline;
    /** Latest courier position as "lat,lng" (null when no simulation tick yet). */
    private String driverLocation;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackingStep {
        private OrderStatus status;
        private boolean reached;
    }
}
