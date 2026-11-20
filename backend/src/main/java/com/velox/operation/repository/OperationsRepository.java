package com.velox.operation.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The order lists of the store, driver and admin dashboards, driver assignment and the numbers. */
@Repository
public class OperationsRepository {

    /** An order a driver may take: a store order the store accepted, or an errand nobody took yet. */
    private static final String TAKEABLE = "o.driver_id IS NULL AND ((o.kind = 'STORE' AND o.status IN ('ACCEPTED', 'READY')) "
            + "OR (o.kind = 'ERRAND' AND o.status = 'PLACED'))";
    private static final String ACTIVE = "('PLACED', 'ACCEPTED', 'READY', 'PICKED_UP')";
    private static final String STORE_ACTIVE = "SELECT id FROM orders WHERE store_id = ? AND status IN " + ACTIVE
            + " ORDER BY id DESC LIMIT 100";
    private static final String STORE_DONE = "SELECT id FROM orders WHERE store_id = ? "
            + "AND status IN ('DELIVERED', 'REJECTED', 'CANCELLED', 'RETURNED') ORDER BY id DESC LIMIT 100";
    private static final String DRIVER_ACTIVE = "SELECT id FROM orders WHERE driver_id = ? AND status IN " + ACTIVE
            + " ORDER BY id DESC LIMIT 50";
    private static final String DRIVER_DONE = "SELECT id FROM orders WHERE driver_id = ? AND status NOT IN " + ACTIVE
            + " ORDER BY id DESC LIMIT 50";
    private static final String HAS_LARGE = "EXISTS (SELECT 1 FROM order_items i JOIN products p ON p.id = i.product_id "
            + "WHERE i.order_id = o.id AND p.size = 'LARGE')";
    private static final String OFFERS_ALL = "SELECT o.id FROM orders o WHERE " + TAKEABLE + " ORDER BY o.id LIMIT 30";
    private static final String OFFERS_SMALL_ONLY = "SELECT o.id FROM orders o WHERE " + TAKEABLE + " AND NOT " + HAS_LARGE
            + " ORDER BY o.id LIMIT 30";
    private static final String ORDER_HAS_LARGE = "SELECT " + HAS_LARGE + " FROM orders o WHERE o.id = ?";
    private static final String ASSIGN = "UPDATE orders o SET driver_id = ?, updated_at = CURRENT_TIMESTAMP WHERE o.id = ? AND " + TAKEABLE;

    public record Totals(int orders, int delivered, int cancelled, BigDecimal sales, BigDecimal commission) {
    }

    private final JdbcTemplate db;

    public OperationsRepository(JdbcTemplate db) {
        this.db = db;
    }

    /** A store's orders: the ones to work on now, or the finished ones. Unpaid orders are never shown. */
    public List<Long> storeOrders(String storeId, boolean active) {
        return db.queryForList(active ? STORE_ACTIVE : STORE_DONE, Long.class, storeId);
    }

    public List<Long> offers(boolean carriesLarge) {
        return db.queryForList(carriesLarge ? OFFERS_ALL : OFFERS_SMALL_ONLY, Long.class);
    }

    public boolean hasLargeItems(long orderId) {
        return Boolean.TRUE.equals(db.queryForObject(ORDER_HAS_LARGE, Boolean.class, orderId));
    }

    /** Takes the order for this driver. Conditional: when two drivers press at once, only one gets it. */
    public boolean assign(long orderId, long driverId) {
        return db.update(ASSIGN, driverId, orderId) > 0;
    }

    public List<Long> driverOrders(long driverId, boolean active) {
        return db.queryForList(active ? DRIVER_ACTIVE : DRIVER_DONE, Long.class, driverId);
    }

    /** Delivery fees of what the driver delivered since {@code since}: {count, fees}. */
    public Object[] driverEarnings(long driverId, Instant since) {
        return db.queryForObject("SELECT COUNT(*), COALESCE(SUM(delivery_fee), 0) FROM orders "
                        + "WHERE driver_id = ? AND status = 'DELIVERED' AND updated_at >= ?",
                (rs, i) -> new Object[] {rs.getInt(1), rs.getBigDecimal(2)}, driverId, Timestamp.from(since));
    }

    public List<Long> allOrders(String status) {
        return status == null
                ? db.queryForList("SELECT id FROM orders ORDER BY id DESC LIMIT 100", Long.class)
                : db.queryForList("SELECT id FROM orders WHERE status = ? ORDER BY id DESC LIMIT 100", Long.class, status);
    }

    /** Orders since {@code since}, for one store or (storeId null) everyone. Sales = delivered items' value. */
    public Totals totals(String storeId, Instant since) {
        return db.queryForObject("SELECT COUNT(*) FILTER (WHERE o.status <> 'PENDING_PAYMENT'), "
                        + "COUNT(*) FILTER (WHERE o.status = 'DELIVERED'), "
                        + "COUNT(*) FILTER (WHERE o.status IN ('CANCELLED', 'REJECTED')), "
                        + "COALESCE(SUM(o.subtotal - o.discount) FILTER (WHERE o.status = 'DELIVERED'), 0), "
                        + "COALESCE(SUM((o.subtotal - o.discount) * s.commission / 100) FILTER (WHERE o.status = 'DELIVERED'), 0) "
                        + "FROM orders o LEFT JOIN stores s ON s.id = o.store_id WHERE o.created_at >= ? AND (CAST(? AS VARCHAR) IS NULL OR o.store_id = ?)",
                (rs, i) -> new Totals(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getBigDecimal(4),
                        rs.getBigDecimal(5).setScale(2, java.math.RoundingMode.HALF_UP)),
                Timestamp.from(since), storeId, storeId);
    }
}
