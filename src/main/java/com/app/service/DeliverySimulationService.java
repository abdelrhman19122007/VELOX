package com.app.service;

import com.app.enums.Governorate;
import com.app.util.DatabaseConnection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Advances web orders through the delivery flow on a timer and persists
 * every step to MySQL (orders.status, deliveries, driver_locations),
 * replacing the old console-only Thread.sleep theater.
 * Tick: every 30s; PENDING orders start moving after 60s.
 */
@Service
public class DeliverySimulationService {

    private static final double BASE_LAT = 31.4165;
    private static final double BASE_LNG = 31.8133;
    private static final double DRIFT = 0.0012;

    private Integer courierId;

    @Scheduled(fixedDelay = 30000)
    public void advance() {
        try {
            int courier = courierId();
            if (courier < 0) {
                return;
            }
            // Snapshot once per tick: every order moves AT MOST one step,
            // so tracking clients watch a gradual PENDING -> ... -> DELIVERED flow.
            List<Integer> pending = ordersIn("PENDING", 60);
            List<Integer> processing = ordersIn("PROCESSING", 0);
            List<Integer> transit = ordersIn("IN_TRANSIT", 0);
            List<Integer> arrived = ordersIn("ARRIVED", 0);
            // PENDING older than a minute enters the flow.
            for (int id : pending) {
                setStatus(id, "PROCESSING");
            }
            // Step the active ones once per tick.
            for (int id : processing) {
                ensureDelivery(id, courier);
                setDeliveryStatus(id, "ASSIGNED");
                moveDriver(courier);
                setStatus(id, "IN_TRANSIT");
            }
            for (int id : transit) {
                ensureDelivery(id, courier);
                setDeliveryStatus(id, "IN_TRANSIT");
                moveDriver(courier);
                setStatus(id, "ARRIVED");
            }
            for (int id : arrived) {
                ensureDelivery(id, courier);
                setDeliveryStatus(id, "ARRIVED");
                moveDriver(courier);
                setStatus(id, "DELIVERED");
                setDelivered(id);
                setDeliveryStatus(id, "DELIVERED");
            }
            // Return pipeline: one step per tick with a notification each time.
            advanceReturns();
        } catch (Exception e) {
            System.err.println("[Simulator] tick failed: " + e.getMessage());
        }
    }

    /**
     * Return pipeline: one step per tick + wallet refund on approval.
     * Refund = total paid - 2 x delivery fee (original + return shipping),
     * floored at zero. A notification fires at every stage.
     */
    private void advanceReturns() {
        for (ReturnRow r : returnRows("RETURN_REQUESTED")) {
            setReturnStatus(r.id(), "RETURN_UNDER_REVIEW");
            notifyUser(r.userId(), "طلبك قيد المراجعة",
                    "طلب إرجاع الطلب " + r.code() + " قيد المراجعة الآن.", "RETURN");
        }
        for (ReturnRow r : returnRows("RETURN_UNDER_REVIEW")) {
            double refund = Math.max(0, r.total() - 2 * r.deliveryFee());
            try (Connection conn = DatabaseConnection.getConnection()) {
                try (PreparedStatement s = conn.prepareStatement(
                        "UPDATE return_requests SET status = 'RETURN_APPROVED' WHERE id = ?")) {
                    s.setInt(1, r.id());
                    s.executeUpdate();
                }
                try (PreparedStatement s = conn.prepareStatement(
                        "UPDATE users SET remaining_budget = remaining_budget + ? WHERE id = ?")) {
                    s.setDouble(1, refund);
                    s.setInt(2, r.userId());
                    s.executeUpdate();
                }
            } catch (SQLException e) {
                System.err.println("[Simulator] return approve failed: " + e.getMessage());
                continue;
            }
            notifyUser(r.userId(), "تمت الموافقة على الإرجاع",
                    "تمت الموافقة على إرجاع الطلب " + r.code() + " وتحويل "
                            + String.format(java.util.Locale.US, "%.2f", refund) + " EGP لمحفظتك.",
                    "RETURN");
        }
        for (ReturnRow r : returnRows("RETURN_APPROVED")) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement s = conn.prepareStatement(
                         "UPDATE return_requests SET status = 'REFUND_PROCESSED' WHERE id = ?")) {
                s.setInt(1, r.id());
                s.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[Simulator] return finalize failed: " + e.getMessage());
                continue;
            }
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement s = conn.prepareStatement(
                         "UPDATE orders SET status = 'RETURNED', is_returned = 1 WHERE id = ?")) {
                s.setInt(1, r.orderId());
                s.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[Simulator] return order flag failed: " + e.getMessage());
            }
            notifyUser(r.userId(), "تم تحويل المبلغ",
                    "تم تحويل المبلغ المسترد إلى محفظتك عن الطلب " + r.code() + ".", "RETURN");
        }
    }

    private record ReturnRow(int id, int orderId, int userId, String code, double total, double deliveryFee) {
    }

    private List<ReturnRow> returnRows(String status) {
        List<ReturnRow> out = new ArrayList<>();
        String sql = "SELECT r.id, r.order_id, r.user_id, o.order_code, o.final_amount, o.delivery_fee"
                + " FROM return_requests r JOIN orders o ON o.id = r.order_id WHERE r.status = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setString(1, status);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    out.add(new ReturnRow(rs.getInt("id"), rs.getInt("order_id"), rs.getInt("user_id"),
                            rs.getString("order_code"), rs.getDouble("final_amount"),
                            rs.getDouble("delivery_fee")));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Simulator] return list failed: " + e.getMessage());
        }
        return out;
    }

    private void setReturnStatus(int id, String status) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "UPDATE return_requests SET status = ? WHERE id = ?")) {
            s.setString(1, status);
            s.setInt(2, id);
            s.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Simulator] return status failed: " + e.getMessage());
        }
    }

    private void notifyUser(int userId, String title, String message, String type) {
        new NotificationService().notify(userId, title, message, type);
    }

    private int courierId() {
        if (courierId != null) {
            return courierId;
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "SELECT id FROM users WHERE email = 'courier@velox.local' LIMIT 1");
             ResultSet rs = s.executeQuery()) {
            if (rs.next()) {
                courierId = rs.getInt(1);
                return courierId;
            }
        } catch (SQLException e) {
            System.err.println("[Simulator] no courier: " + e.getMessage());
        }
        return -1;
    }

    private List<Integer> ordersIn(String status, int olderThanSeconds) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM orders WHERE status = ?"
                + (olderThanSeconds > 0 ? " AND order_date < NOW() - INTERVAL " + olderThanSeconds + " SECOND" : "");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setString(1, status);
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Simulator] list failed: " + e.getMessage());
        }
        return ids;
    }

    private void setStatus(int orderId, String status) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "UPDATE orders SET status = ? WHERE id = ?")) {
            s.setString(1, status);
            s.setInt(2, orderId);
            s.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Simulator] status failed: " + e.getMessage());
        }
    }

    private void ensureDelivery(int orderId, int courier) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String zone = "CAIRO";
            double fee = 0;
            try (PreparedStatement s = conn.prepareStatement(
                    "SELECT shipping_address, delivery_fee FROM orders WHERE id = ?")) {
                s.setInt(1, orderId);
                try (ResultSet rs = s.executeQuery()) {
                    if (rs.next()) {
                        zone = zoneOf(rs.getString("shipping_address"));
                        fee = rs.getDouble("delivery_fee");
                    }
                }
            }
            try (PreparedStatement s = conn.prepareStatement(
                    "INSERT IGNORE INTO deliveries (order_id, driver_id, zone, delivery_status, delivery_fee, assigned_at)"
                            + " VALUES (?, ?, ?, 'ASSIGNED', ?, NOW())")) {
                s.setInt(1, orderId);
                s.setInt(2, courier);
                s.setString(3, zone);
                s.setDouble(4, fee);
                s.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("[Simulator] delivery row failed: " + e.getMessage());
        }
    }

    private void setDeliveryStatus(int orderId, String status) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "UPDATE deliveries SET delivery_status = ? WHERE order_id = ?")) {
            s.setString(1, status);
            s.setInt(2, orderId);
            s.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Simulator] delivery status failed: " + e.getMessage());
        }
    }

    private void setDelivered(int orderId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "UPDATE deliveries SET delivered_at = NOW() WHERE order_id = ?")) {
            s.setInt(1, orderId);
            s.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Simulator] delivered_at failed: " + e.getMessage());
        }
    }

    private void moveDriver(int courier) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement sel = conn.prepareStatement(
                     "SELECT latitude, longitude FROM driver_locations WHERE driver_id = ?")) {
            sel.setInt(1, courier);
            double lat = BASE_LAT;
            double lng = BASE_LNG;
            boolean exists = false;
            try (ResultSet rs = sel.executeQuery()) {
                if (rs.next()) {
                    exists = true;
                    lat = rs.getDouble(1) + DRIFT;
                    lng = rs.getDouble(2) + DRIFT;
                }
            }
            if (exists) {
                try (PreparedStatement up = conn.prepareStatement(
                        "UPDATE driver_locations SET latitude = ?, longitude = ? WHERE driver_id = ?")) {
                    up.setDouble(1, lat);
                    up.setDouble(2, lng);
                    up.setInt(3, courier);
                    up.executeUpdate();
                }
            } else {
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO driver_locations (driver_id, latitude, longitude) VALUES (?, ?, ?)")) {
                    ins.setInt(1, courier);
                    ins.setDouble(2, lat);
                    ins.setDouble(3, lng);
                    ins.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("[Simulator] driver move failed: " + e.getMessage());
        }
    }

    private static String zoneOf(String shippingAddress) {
        if (shippingAddress != null) {
            String first = shippingAddress.split(",")[0].trim();
            try {
                return Governorate.fromNameLenient(first).name();
            } catch (IllegalArgumentException ignored) {
            }
        }
        return "CAIRO";
    }
}
