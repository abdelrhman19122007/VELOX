package com.app.service;

import com.app.dto.OrderHistoryDto;
import com.app.dto.OrderTrackingDto;
import com.app.dto.PagedResponse;
import com.app.enums.OrderStatus;
import com.app.model.order.Order;
import com.app.util.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * KAN-90: Order history + real-time tracking logic.
 * Canonical flow: PENDING -> PROCESSING -> SHIPPED -> DELIVERED
 */
@Service
public class OrderService {

    private static final List<OrderStatus> TRACKING_FLOW = Arrays.asList(
            OrderStatus.PENDING,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED
    );

    private final Map<String, Order> store = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastUpdated = new ConcurrentHashMap<>();

    public OrderService() {
        // Load persisted orders (file-based) if any; fallback to demo seed.
        try {
            List<Order> persisted = OrderRepository.loadOrders();
            if (persisted != null) {
                for (Order o : persisted) {
                    if (o.getUserId() == null) {
                        o.setUserId(o.getPhone());
                    }
                    store.put(o.getOrderId(), o);
                    lastUpdated.put(o.getOrderId(), o.getOrderDate());
                }
            }
        } catch (Exception ignored) {
        }
        if (store.isEmpty()) {
            seedDemoData();
        }
    }

    public PagedResponse<OrderHistoryDto> getOrdersByUserId(String userId, int page, int size) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 10;

        List<OrderHistoryDto> all = store.values().stream()
                .filter(o -> userId.equalsIgnoreCase(o.getUserId()) || userId.equals(o.getPhone()))
                .sorted((a, b) -> b.getOrderDate().compareTo(a.getOrderDate()))
                .map(OrderHistoryDto::from)
                .collect(Collectors.toList());

        int total = all.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<OrderHistoryDto> content = all.subList(from, to);

        return new PagedResponse<>(content, page, size, total, totalPages);
    }

    public OrderTrackingDto getTracking(String orderId) {
        Order order = requireOrder(orderId);
        return buildTracking(order);
    }

    public OrderTrackingDto updateStatus(String orderId, OrderStatus newStatus) {
        Order order = requireOrder(orderId);
        validateTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        lastUpdated.put(orderId, LocalDateTime.now());
        return buildTracking(order);
    }

    // ---- helpers ----

    private Order requireOrder(String orderId) {
        return Optional.ofNullable(store.get(orderId))
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    private OrderTrackingDto buildTracking(Order order) {
        int currentIdx = TRACKING_FLOW.indexOf(normalize(order.getStatus()));
        List<OrderTrackingDto.TrackingStep> timeline = new ArrayList<>();
        for (int i = 0; i < TRACKING_FLOW.size(); i++) {
            timeline.add(new OrderTrackingDto.TrackingStep(
                    TRACKING_FLOW.get(i), currentIdx >= 0 && i <= currentIdx));
        }
        return new OrderTrackingDto(
                order.getOrderId(),
                order.getStatus(),
                lastUpdated.getOrDefault(order.getOrderId(), order.getOrderDate()),
                timeline
        );
    }

    private OrderStatus normalize(OrderStatus status) {
        // Backward compat: IN_TRANSIT ~ SHIPPED, ARRIVED/PAID ~ between SHIPPED and DELIVERED
        if (status == OrderStatus.IN_TRANSIT) return OrderStatus.SHIPPED;
        if (status == OrderStatus.ARRIVED || status == OrderStatus.PAID) return OrderStatus.SHIPPED;
        return status;
    }

    private void validateTransition(OrderStatus from, OrderStatus to) {
        if (to == null) throw new IllegalArgumentException("status is required");
        if (from == to) return;
        int fromIdx = TRACKING_FLOW.indexOf(normalize(from));
        int toIdx = TRACKING_FLOW.indexOf(normalize(to));
        if (toIdx < 0) {
            throw new IllegalArgumentException("Status " + to + " is not part of tracking flow " + TRACKING_FLOW);
        }
        if (fromIdx < 0 || toIdx != fromIdx + 1) {
            throw new IllegalStateException("Invalid transition: " + from + " -> " + to + ". Expected flow: " + TRACKING_FLOW);
        }
    }

    private void seedDemoData() {
        Order o1 = new Order("ORD-1001", "user-1", "Cairo", "01000000001");
        o1.setStatus(OrderStatus.DELIVERED);
        Order o2 = new Order("ORD-1002", "user-1", "Giza", "01000000001");
        o2.setStatus(OrderStatus.SHIPPED);
        Order o3 = new Order("ORD-1003", "user-1", "Alexandria", "01000000001");
        o3.setStatus(OrderStatus.PROCESSING);
        store.put(o1.getOrderId(), o1);
        store.put(o2.getOrderId(), o2);
        store.put(o3.getOrderId(), o3);
        lastUpdated.put(o1.getOrderId(), LocalDateTime.now().minusDays(2));
        lastUpdated.put(o2.getOrderId(), LocalDateTime.now().minusHours(5));
        lastUpdated.put(o3.getOrderId(), LocalDateTime.now().minusHours(1));
    }
}
