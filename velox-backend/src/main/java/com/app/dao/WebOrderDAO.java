package com.app.dao;

import com.app.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transactional order persistence. Server-side pricing from DB rows (client
 * totals are never trusted). Also mirrors console orders (explicit totals)
 * and returns, so MySQL stays the single source of truth.
 */
public class WebOrderDAO {

    public record OrderLine(int productId, String name, double unitPrice, int quantity) {
    }

    public record PlacedOrder(int orderId, String orderCode, double subtotal,
                              double deliveryFee, double total, String status,
                              List<OrderLine> lines) {
    }

    /** Web checkout: totals computed here, status PENDING, code ORD-W{id}. */
    public PlacedOrder placeOrder(int userId, String shippingAddress, String zone,
                                  double deliveryFee, List<int[]> items) {
        if (deliveryFee < 0) {
            throw new IllegalArgumentException("Invalid delivery fee.");
        }
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            CheckedCart cart = checkAndLock(conn, items);
            int addressId = ensureAddress(conn, userId, shippingAddress, zone);
            double total = cart.subtotal + deliveryFee;
            int orderId = insertOrder(conn, userId, addressId, cart.subtotal, 0,
                    deliveryFee, total, "PENDING", shippingAddress);
            insertItemsAndStock(conn, orderId, cart.lines);
            setOrderCode(conn, orderId, "ORD-W" + orderId);
            conn.commit();
            return new PlacedOrder(orderId, "ORD-W" + orderId, cart.subtotal,
                    deliveryFee, total, "PENDING", cart.lines);
        } catch (IllegalArgumentException e) {
            rollbackQuiet(conn);
            throw e;
        } catch (SQLException e) {
            rollbackQuiet(conn);
            throw new IllegalStateException("Checkout failed: " + e.getMessage());
        } finally {
            closeQuiet(conn);
        }
    }

    /**
     * Console mirror: same transaction, but totals/status/code come from the
     * console receipt (weight + packaging math lives there).
     */
    public PlacedOrder placeOrderWithTotals(String orderCode, int userId, String shippingAddress,
                                            String zone, double subtotal, double discount,
                                            double deliveryFee, double total, String status,
                                            List<int[]> items) {
        if (orderCode == null || orderCode.isBlank()) {
            throw new IllegalArgumentException("Order code is required.");
        }
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            CheckedCart cart = checkAndLock(conn, items);
            if (Math.abs(cart.subtotal - subtotal) > 0.01) {
                throw new IllegalArgumentException("Console subtotal does not match database prices.");
            }
            int addressId = ensureAddress(conn, userId, shippingAddress, zone);
            int orderId = insertOrder(conn, userId, addressId, subtotal, discount,
                    deliveryFee, total, status, shippingAddress);
            insertItemsAndStock(conn, orderId, cart.lines);
            setOrderCode(conn, orderId, orderCode.trim());
            conn.commit();
            return new PlacedOrder(orderId, orderCode.trim(), subtotal, deliveryFee, total, status, cart.lines);
        } catch (IllegalArgumentException e) {
            rollbackQuiet(conn);
            throw e;
        } catch (SQLException e) {
            rollbackQuiet(conn);
            throw new IllegalStateException("Mirror failed: " + e.getMessage());
        } finally {
            closeQuiet(conn);
        }
    }

    /** Marks a return in MySQL and restores stock. Returns false when unknown. */
    public boolean markReturnedByCode(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) {
            return false;
        }
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            int orderId = -1;
            try (PreparedStatement s = conn.prepareStatement(
                    "SELECT id FROM orders WHERE order_code = ? FOR UPDATE")) {
                s.setString(1, orderCode.trim());
                try (ResultSet rs = s.executeQuery()) {
                    if (rs.next()) {
                        orderId = rs.getInt(1);
                    }
                }
            }
            if (orderId < 0) {
                rollbackQuiet(conn);
                return false;
            }
            try (PreparedStatement s = conn.prepareStatement(
                    "UPDATE orders SET status = 'RETURNED', is_returned = 1 WHERE id = ?")) {
                s.setInt(1, orderId);
                s.executeUpdate();
            }
            try (PreparedStatement s = conn.prepareStatement(
                    "UPDATE products p JOIN order_items oi ON oi.product_id = p.id"
                            + " SET p.stock_quantity = p.stock_quantity + oi.quantity"
                            + " WHERE oi.order_id = ?")) {
                s.setInt(1, orderId);
                s.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            rollbackQuiet(conn);
            System.err.println("[WebOrderDAO] return sync failed: " + e.getMessage());
            return false;
        } finally {
            closeQuiet(conn);
        }
    }

    /** Header + lines for invoice generation (null when unknown). */
    public Map<String, Object> invoiceData(String codeOrId) {
        if (codeOrId == null || codeOrId.isBlank()) {
            return null;
        }
        int numeric;
        try {
            numeric = Integer.parseInt(codeOrId.trim());
        } catch (NumberFormatException e) {
            numeric = -1;
        }
        String sql = "SELECT o.id, o.order_code, o.status, o.order_date, o.total_amount,"
                + " o.discount_amount, o.delivery_fee, o.final_amount, o.shipping_address,"
                + " u.full_name, u.email, u.phone_number"
                + " FROM orders o JOIN users u ON u.id = o.user_id"
                + " WHERE o.id = ? OR o.order_code = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, numeric);
            s.setString(2, codeOrId.trim());
            try (ResultSet rs = s.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("orderId", rs.getInt("id"));
                out.put("orderCode", rs.getString("order_code"));
                out.put("status", rs.getString("status"));
                out.put("orderDate", rs.getTimestamp("order_date").toLocalDateTime().toString());
                out.put("subtotal", rs.getDouble("total_amount"));
                out.put("discount", rs.getDouble("discount_amount"));
                out.put("deliveryFee", rs.getDouble("delivery_fee"));
                out.put("total", rs.getDouble("final_amount"));
                out.put("shipping", rs.getString("shipping_address"));
                out.put("customer", rs.getString("full_name"));
                out.put("email", rs.getString("email"));
                out.put("phone", rs.getString("phone_number"));
                List<Map<String, Object>> lines = new ArrayList<>();
                try (PreparedStatement l = conn.prepareStatement(
                        "SELECT p.name, oi.quantity, oi.unit_price, oi.subtotal"
                                + " FROM order_items oi JOIN products p ON p.id = oi.product_id"
                                + " WHERE oi.order_id = ?")) {
                    l.setInt(1, rs.getInt("id"));
                    try (ResultSet lr = l.executeQuery()) {
                        while (lr.next()) {
                            Map<String, Object> line = new LinkedHashMap<>();
                            line.put("name", lr.getString("name"));
                            line.put("quantity", lr.getInt("quantity"));
                            line.put("unit", lr.getDouble("unit_price"));
                            line.put("subtotal", lr.getDouble("subtotal"));
                            lines.add(line);
                        }
                    }
                }
                out.put("lines", lines);
                return out;
            }
        } catch (SQLException e) {
            System.err.println("[WebOrderDAO] invoice data failed: " + e.getMessage());
            return null;
        }
    }

    // ---- shared internals ----

    private record CheckedCart(List<OrderLine> lines, double subtotal) {
    }

    private CheckedCart checkAndLock(Connection conn, List<int[]> items) throws SQLException {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty.");
        }
        List<OrderLine> lines = new ArrayList<>();
        double subtotal = 0;
        for (int[] it : items) {
            int pid = it[0];
            int qty = it[1];
            if (qty < 1 || qty > 50) {
                throw new IllegalArgumentException("Invalid quantity for product " + pid + ".");
            }
            try (PreparedStatement s = conn.prepareStatement(
                    "SELECT id, name, price, stock_quantity, is_available FROM products WHERE id = ? FOR UPDATE")) {
                s.setInt(1, pid);
                try (ResultSet rs = s.executeQuery()) {
                    if (!rs.next() || rs.getInt("is_available") != 1) {
                        throw new IllegalArgumentException("Product not available: " + pid + ".");
                    }
                    int stock = rs.getInt("stock_quantity");
                    if (stock < qty) {
                        throw new IllegalArgumentException(
                                "Only " + stock + " left in stock for product " + pid + ".");
                    }
                    double price = rs.getDouble("price");
                    lines.add(new OrderLine(pid, rs.getString("name"), price, qty));
                    subtotal += price * qty;
                }
            }
        }
        return new CheckedCart(lines, subtotal);
    }

    private int insertOrder(Connection conn, int userId, int addressId, double subtotal,
                            double discount, double deliveryFee, double total,
                            String status, String shippingAddress) throws SQLException {
        try (PreparedStatement s = conn.prepareStatement(
                "INSERT INTO orders (user_id, address_id, total_amount, discount_amount,"
                        + " delivery_fee, final_amount, status, shipping_address, is_returned)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)",
                Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, userId);
            s.setInt(2, addressId);
            s.setDouble(3, subtotal);
            s.setDouble(4, discount);
            s.setDouble(5, deliveryFee);
            s.setDouble(6, total);
            s.setString(7, status);
            s.setString(8, shippingAddress);
            if (s.executeUpdate() != 1) {
                throw new SQLException("Could not create order.");
            }
            try (ResultSet keys = s.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void insertItemsAndStock(Connection conn, int orderId, List<OrderLine> lines) throws SQLException {
        try (PreparedStatement item = conn.prepareStatement(
                "INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?)");
             PreparedStatement stock = conn.prepareStatement(
                "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ?")) {
            for (OrderLine l : lines) {
                item.setInt(1, orderId);
                item.setInt(2, l.productId());
                item.setInt(3, l.quantity());
                item.setDouble(4, l.unitPrice());
                item.setDouble(5, l.unitPrice() * l.quantity());
                item.addBatch();
                stock.setInt(1, l.quantity());
                stock.setInt(2, l.productId());
                stock.addBatch();
            }
            item.executeBatch();
            stock.executeBatch();
        }
    }

    private void setOrderCode(Connection conn, int orderId, String code) throws SQLException {
        try (PreparedStatement s = conn.prepareStatement(
                "UPDATE orders SET order_code = ? WHERE id = ?")) {
            s.setString(1, code);
            s.setInt(2, orderId);
            s.executeUpdate();
        }
    }

    /** Feature 3: pins a future delivery time on an already-placed order. */
    public void setScheduledFor(int orderId, java.time.LocalDateTime at) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                     "UPDATE orders SET scheduled_for = ? WHERE id = ?")) {
            s.setTimestamp(1, java.sql.Timestamp.valueOf(at));
            s.setInt(2, orderId);
            if (s.executeUpdate() != 1) {
                throw new IllegalStateException("Could not schedule order.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not schedule order: " + e.getMessage());
        }
    }

    /** Finds the DB user id by email, creating the row from the file profile when absent. */
    public int ensureUser(String email, String fullName, String passwordHash, String phone, String governorate) {
        Integer id = findUserId(email);
        if (id != null) {
            return id;
        }
        String sql = "INSERT INTO users (full_name, email, password_hash, phone_number, governorate)"
                + " VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, fullName);
            s.setString(2, email);
            s.setString(3, passwordHash);
            s.setString(4, phone);
            s.setString(5, governorate);
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            // Concurrent register race: re-read once.
            Integer retry = findUserId(email);
            if (retry != null) {
                return retry;
            }
            throw new IllegalStateException("Could not create user: " + e.getMessage());
        }
    }

    public Integer findUserId(String email) {
        if (email == null) {
            return null;
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement("SELECT id FROM users WHERE email = ?")) {
            s.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("[WebOrderDAO] find user failed: " + e.getMessage());
        }
        return null;
    }

    public String findEmailById(int userId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement("SELECT email FROM users WHERE id = ?")) {
            s.setInt(1, userId);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("email");
                }
            }
        } catch (SQLException e) {
            System.err.println("[WebOrderDAO] email lookup failed: " + e.getMessage());
        }
        return null;
    }

    private int ensureAddress(Connection conn, int userId, String shippingAddress, String zone) throws SQLException {
        try (PreparedStatement s = conn.prepareStatement(
                "SELECT id FROM addresses WHERE user_id = ? AND city = ? LIMIT 1")) {
            s.setInt(1, userId);
            s.setString(2, zone);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        try (PreparedStatement s = conn.prepareStatement(
                "INSERT INTO addresses (user_id, address_line, city, zone, phone)"
                        + " VALUES (?, ?, ?, ?, '')",
                Statement.RETURN_GENERATED_KEYS)) {
            s.setInt(1, userId);
            s.setString(2, shippingAddress);
            s.setString(3, zone);
            s.setString(4, zone);
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private static void rollbackQuiet(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
        }
    }

    private static void closeQuiet(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }

    /** Result row for building the file-account copy of a web order. */
    public Map<String, Object> productView(int productId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement s = conn.prepareStatement(
                "SELECT id, name, description, price, product_type, size FROM products WHERE id = ?")) {
            s.setInt(1, productId);
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getInt("id"));
                    m.put("name", rs.getString("name"));
                    m.put("description", rs.getString("description"));
                    m.put("price", rs.getDouble("price"));
                    m.put("product_type", rs.getString("product_type"));
                    m.put("size", rs.getString("size"));
                    return m;
                }
            }
        } catch (SQLException e) {
            System.err.println("[WebOrderDAO] product view failed: " + e.getMessage());
        }
        return Map.of();
    }

    public record Quote(double subtotal, List<OrderLine> lines) {
    }

    public record StoreQuote(long storeId, String storeName, double subtotal, int lines) {
    }

    public record DetailedQuote(double subtotal, List<StoreQuote> stores) {
    }

    /** Price check without locking (re-validated with locks at placement). */
    public Quote quote(List<int[]> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty.");
        }
        List<OrderLine> lines = new ArrayList<>();
        double subtotal = 0;
        try (Connection conn = DatabaseConnection.getConnection()) {
            for (int[] it : items) {
                int pid = it[0];
                int qty = it[1];
                if (qty < 1 || qty > 50) {
                    throw new IllegalArgumentException("Invalid quantity for product " + pid + ".");
                }
                try (PreparedStatement s = conn.prepareStatement(
                        "SELECT id, name, price, stock_quantity, is_available FROM products WHERE id = ?")) {
                    s.setInt(1, pid);
                    try (ResultSet rs = s.executeQuery()) {
                        if (!rs.next() || rs.getInt("is_available") != 1) {
                            throw new IllegalArgumentException("Product not available: " + pid + ".");
                        }
                        if (rs.getInt("stock_quantity") < qty) {
                            throw new IllegalArgumentException(
                                    "Only " + rs.getInt("stock_quantity") + " left in stock for product " + pid + ".");
                        }
                        double price = rs.getDouble("price");
                        lines.add(new OrderLine(pid, rs.getString("name"), price, qty));
                        subtotal += price * qty;
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (SQLException e) {
            throw new IllegalStateException("Quote failed: " + e.getMessage());
        }
        return new Quote(subtotal, lines);
    }

    /**
     * Feature 1: same price check as {@link #quote(List)} but grouped per
     * store (JOIN stores) so one cart can span many stores in a single
     * checkout. No locking; placement re-validates.
     */
    public DetailedQuote quoteDetailed(List<int[]> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty.");
        }
        double subtotal = 0;
        java.util.Map<Long, double[]> sum = new java.util.LinkedHashMap<>();
        java.util.Map<Long, String> names = new java.util.LinkedHashMap<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            for (int[] it : items) {
                int pid = it[0];
                int qty = it[1];
                if (qty < 1 || qty > 50) {
                    throw new IllegalArgumentException("Invalid quantity for product " + pid + ".");
                }
                try (PreparedStatement s = conn.prepareStatement(
                        "SELECT p.id, p.name, p.price, p.stock_quantity, p.is_available,"
                                + " p.store_id, s.name AS store_name FROM products p"
                                + " LEFT JOIN stores s ON s.id = p.store_id WHERE p.id = ?")) {
                    s.setInt(1, pid);
                    try (ResultSet rs = s.executeQuery()) {
                        if (!rs.next() || rs.getInt("is_available") != 1) {
                            throw new IllegalArgumentException("Product not available: " + pid + ".");
                        }
                        if (rs.getInt("stock_quantity") < qty) {
                            throw new IllegalArgumentException(
                                    "Only " + rs.getInt("stock_quantity") + " left in stock for product " + pid + ".");
                        }
                        double line = rs.getDouble("price") * qty;
                        subtotal += line;
                        long sid = rs.getLong("store_id");
                        String sname = rs.getString("store_name");
                        names.putIfAbsent(sid, sname != null ? sname : ("Store " + sid));
                        double[] acc = sum.computeIfAbsent(sid, k -> new double[2]);
                        acc[0] += line;
                        acc[1] += 1;
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (SQLException e) {
            throw new IllegalStateException("Quote failed: " + e.getMessage());
        }
        List<StoreQuote> stores = new ArrayList<>();
        for (java.util.Map.Entry<Long, double[]> e : sum.entrySet()) {
            stores.add(new StoreQuote(e.getKey(), names.get(e.getKey()), e.getValue()[0], (int) e.getValue()[1]));
        }
        return new DetailedQuote(subtotal, stores);
    }
}
