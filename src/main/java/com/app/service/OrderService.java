package com.app.service;

import com.app.dto.OrderHistoryDto;
import com.app.dto.OrderTrackingDto;
import com.app.dto.PagedResponse;
import com.app.enums.OrderStatus;
import com.app.util.DatabaseConnection;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Order history + real-time tracking, backed by MySQL ({@code orders} is the
 * single source of truth). Status changes are persisted, not kept in memory.
 * Canonical flow: PENDING -> PROCESSING -> SHIPPED -> DELIVERED
 */
@Service
public class OrderService {

    private static final List<OrderStatus> TRACKING_FLOW = Arrays.asList(
            OrderStatus.PENDING,
            OrderStatus.PROCESSING,
            OrderStatus.IN_TRANSIT,
            OrderStatus.ARRIVED,
            OrderStatus.DELIVERED
    );

    public PagedResponse<OrderHistoryDto> getOrdersByUserId(String userId, int page, int size) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 100) {
            size = 10;
        }
        Integer dbId = resolveUserId(userId.trim());
        if (dbId == null) {
            return new PagedResponse<>(new ArrayList<>(), page, size, 0, 0);
        }

        int total = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "SELECT COUNT(*) FROM orders WHERE user_id = ?")) {
            s.setInt(1, dbId);
            try (ResultSet rs = s.executeQuery()) {
                rs.next();
                total = rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("History unavailable: " + e.getMessage());
        }

        int totalPages = (int) Math.ceil((double) total / size);
        List<OrderHistoryDto> content = new ArrayList<>();
        String sql = "SELECT o.id, o.order_code, o.status, o.order_date, o.final_amount,"
                + " o.shipping_address, COALESCE(SUM(oi.quantity), 0) AS items"
                + " FROM orders o LEFT JOIN order_items oi ON oi.order_id = o.id"
                + " WHERE o.user_id = ? GROUP BY o.id ORDER BY o.order_date DESC LIMIT ? OFFSET ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, dbId);
            s.setInt(2, size);
            s.setInt(3, page * size);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("order_code");
                    content.add(new OrderHistoryDto(
                            code != null ? code : ("ORD-" + rs.getInt("id")),
                            userId.trim(),
                            OrderStatus.valueOf(rs.getString("status")),
                            rs.getTimestamp("order_date").toLocalDateTime(),
                            rs.getInt("items"),
                            rs.getDouble("final_amount"),
                            rs.getString("shipping_address")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("History unavailable: " + e.getMessage());
        }
        return new PagedResponse<>(content, page, size, total, totalPages);
    }

    public OrderTrackingDto getTracking(String orderId) {
        Row row = requireRow(orderId);
        OrderTrackingDto dto = buildTracking(row);
        dto.setDriverLocation(driverLocationOf(row.id()));
        return dto;
    }

    /** Latest courier position for the order's delivery, if simulated yet. */
    private String driverLocationOf(int orderDbId) {
        String sql = "SELECT l.latitude, l.longitude FROM driver_locations l"
                + " JOIN deliveries d ON d.driver_id = l.driver_id"
                + " WHERE d.order_id = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, orderDbId);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1) + "," + rs.getDouble(2);
                }
            }
        } catch (SQLException e) {
            System.err.println("[OrderService] driver lookup failed: " + e.getMessage());
        }
        return null;
    }

    public OrderTrackingDto updateStatus(String orderId, OrderStatus newStatus) {
        Row row = requireRow(orderId);
        validateTransition(row.status(), newStatus);
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setString(1, newStatus.name());
            s.setInt(2, row.id());
            s.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Status update failed: " + e.getMessage());
        }
        notifyOwner(row.id(), row.code(), newStatus);
        return buildTracking(new Row(row.id(), row.code(), newStatus, LocalDateTime.now()));
    }

    private void notifyOwner(int orderDbId, String orderCode, OrderStatus newStatus) {
        String sql = "SELECT u.id, u.email FROM orders o JOIN users u ON u.id = o.user_id WHERE o.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, orderDbId);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    new NotificationService().notify(rs.getInt("id"),
                            "تحديث حالة الطلب " + orderCode,
                            "طلبك " + orderCode + " أصبح الآن: " + newStatus.name(),
                            "ORDER_STATUS");
                }
            }
        } catch (SQLException e) {
            System.err.println("[OrderService] notify failed: " + e.getMessage());
        }
    }

    /** True when the DB order belongs to the given account email. */
    public boolean ownsOrder(String orderId, String email) {
        if (orderId == null || email == null) {
            return false;
        }
        String sql = "SELECT 1 FROM orders o JOIN users u ON u.id = o.user_id"
                + " WHERE (o.id = ? OR o.order_code = ?) AND u.email = ? LIMIT 1";
        int numeric = toIntOr(orderId, -1);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, numeric);
            s.setString(2, orderId.trim());
            s.setString(3, email.trim().toLowerCase());
            try (ResultSet rs = s.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    // ---- helpers ----

    private record Row(int id, String code, OrderStatus status, LocalDateTime updated) {
    }

    private Row requireRow(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        String sql = "SELECT id, order_code, status, order_date FROM orders WHERE id = ? OR order_code = ? LIMIT 1";
        int numeric = toIntOr(orderId.trim(), -1);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, numeric);
            s.setString(2, orderId.trim());
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    String code = rs.getString("order_code");
                    Timestamp ts = rs.getTimestamp("order_date");
                    return new Row(rs.getInt("id"),
                            code != null ? code : ("ORD-" + rs.getInt("id")),
                            OrderStatus.valueOf(rs.getString("status")),
                            ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Tracking unavailable: " + e.getMessage());
        }
        throw new IllegalArgumentException("Order not found: " + orderId);
    }

    private Integer resolveUserId(String userId) {
        int numeric = toIntOr(userId, -1);
        String sql = "SELECT id FROM users WHERE id = ? OR email = ? OR phone_number = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, numeric);
            s.setString(2, userId.toLowerCase());
            s.setString(3, userId);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("History unavailable: " + e.getMessage());
        }
        return null;
    }

    private static int toIntOr(String v, int fallback) {
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private OrderTrackingDto buildTracking(Row row) {
        int currentIdx = TRACKING_FLOW.indexOf(normalize(row.status()));
        List<OrderTrackingDto.TrackingStep> timeline = new ArrayList<>();
        for (int i = 0; i < TRACKING_FLOW.size(); i++) {
            timeline.add(new OrderTrackingDto.TrackingStep(
                    TRACKING_FLOW.get(i), currentIdx >= 0 && i <= currentIdx));
        }
        return new OrderTrackingDto(row.code(), row.status(), row.updated(), timeline, null);
    }

    private OrderStatus normalize(OrderStatus status) {
        // SHIPPED and paid-but-not-shipped behave like IN_TRANSIT in the timeline.
        if (status == OrderStatus.SHIPPED || status == OrderStatus.PAID) {
            return OrderStatus.IN_TRANSIT;
        }
        return status;
    }

    private void validateTransition(OrderStatus from, OrderStatus to) {
        if (to == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (from == to) {
            return;
        }
        int fromIdx = TRACKING_FLOW.indexOf(normalize(from));
        int toIdx = TRACKING_FLOW.indexOf(normalize(to));
        if (toIdx < 0) {
            throw new IllegalArgumentException("Status " + to + " is not part of tracking flow " + TRACKING_FLOW);
        }
        if (fromIdx < 0 || toIdx != fromIdx + 1) {
            throw new IllegalStateException("Invalid transition: " + from + " -> " + to + ". Expected flow: " + TRACKING_FLOW);
        }
    }
}
