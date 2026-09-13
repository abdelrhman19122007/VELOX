package com.app.controller;

import com.app.dto.OrderHistoryDto;
import com.app.dto.OrderTrackingDto;
import com.app.dto.PagedResponse;
import com.app.enums.OrderStatus;
import com.app.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * KAN-90: Purchase History + Real-Time Order Status Tracking.
 *
 * GET /api/orders/history?userId={id}&page=0&size=10
 * GET /api/orders/{orderId}/tracking
 * PATCH /api/orders/{orderId}/status?status=SHIPPED
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/history")
    public ResponseEntity<PagedResponse<OrderHistoryDto>> getHistory(
            @RequestParam String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ResponseEntity.ok(orderService.getOrdersByUserId(userId, page, size));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<OrderTrackingDto> getTracking(@PathVariable String orderId) {
        try {
            return ResponseEntity.ok(orderService.getTracking(orderId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderTrackingDto> updateStatus(
            @PathVariable String orderId,
            @RequestParam OrderStatus status) {
        try {
            return ResponseEntity.ok(orderService.updateStatus(orderId, status));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}
