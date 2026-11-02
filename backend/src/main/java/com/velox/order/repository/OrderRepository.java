package com.velox.order.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.velox.order.domain.Order;
import com.velox.order.domain.OrderItem;
import com.velox.order.domain.OrderKind;
import com.velox.order.domain.OrderStatus;
import com.velox.order.domain.PaymentMethod;
import com.velox.order.domain.PaymentStatus;

/** SQL for orders, their items and their status timeline. Parameterised queries only. */
@Repository
public class OrderRepository {

    private static final String STALE = "SELECT id FROM orders WHERE status = ? AND updated_at < ?";
    private static final String STALE_WITHOUT_DRIVER = STALE + " AND driver_id IS NULL";

    /** One step of the timeline: status and when. */
    public record Step(OrderStatus status, Instant at) {
    }

    static final RowMapper<Order> ORDER = OrderRepository::map;
    private static final RowMapper<OrderItem> ITEM = (rs, n) -> new OrderItem(rs.getString("product_id"),
            rs.getString("name"), rs.getString("name_ar"), rs.getBigDecimal("unit_price"), rs.getInt("qty"), rs.getString("note"));

    private final JdbcTemplate db;

    public OrderRepository(JdbcTemplate db) {
        this.db = db;
    }

    /** Saves a new order with its items and returns its database id. */
    public long insert(Order o, List<OrderItem> items) {
        Long id = db.queryForObject("INSERT INTO orders (code, kind, customer_id, store_id, status, zone, phone, address, "
                + "landmark, lat, lng, errand_details, subtotal, discount, delivery_fee, total, cash_change_for, payment_method, "
                + "payment_status, promo, scheduled_for) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id", Long.class,
                o.code(), o.kind().name(), o.customerId(), o.storeId(), o.status().name(), o.zone(), o.phone(), o.address(),
                o.landmark(), o.lat(), o.lng(), o.errandDetails(), o.subtotal(), o.discount(), o.deliveryFee(), o.total(),
                o.cashChangeFor(), o.paymentMethod().name(), o.paymentStatus().name(), o.promo(),
                o.scheduledFor() == null ? null : Timestamp.from(o.scheduledFor()));
        if (id == null) {
            throw new IllegalStateException("The order was not saved");
        }
        for (OrderItem item : items) {
            db.update("INSERT INTO order_items (order_id, product_id, name, name_ar, unit_price, qty, note) VALUES (?,?,?,?,?,?,?)",
                    id, item.productId(), item.name(), item.nameAr(), item.unitPrice(), item.qty(), item.note());
        }
        return id;
    }

    public String nextCode() {
        return "ORD-" + db.queryForObject("SELECT nextval('order_code_seq')", Long.class);
    }

    public Optional<Order> findById(long id) {
        return db.query("SELECT * FROM orders WHERE id = ?", ORDER, id).stream().findFirst();
    }

    public Optional<Order> findByCode(String code) {
        return db.query("SELECT * FROM orders WHERE code = ?", ORDER, code).stream().findFirst();
    }

    public List<Order> findByCustomer(long customerId, int limit) {
        return db.query("SELECT * FROM orders WHERE customer_id = ? ORDER BY id DESC LIMIT ?", ORDER, customerId, limit);
    }

    public List<OrderItem> items(long orderId) {
        return db.query("SELECT * FROM order_items WHERE order_id = ? ORDER BY id", ITEM, orderId);
    }

    public List<Step> timeline(long orderId) {
        return db.query("SELECT status, at FROM order_events WHERE order_id = ? ORDER BY id",
                (rs, n) -> new Step(OrderStatus.valueOf(rs.getString(1)), rs.getTimestamp(2).toInstant()), orderId);
    }

    public void addEvent(long orderId, OrderStatus status, Long actorId) {
        db.update("INSERT INTO order_events (order_id, status, actor_id) VALUES (?,?,?)", orderId, status.name(), actorId);
    }

    /**
     * Moves the order only if it is still in {@code from} — so two people pressing at the same moment can't both
     * succeed (e.g. accept and cancel). Returns false when someone else moved it first.
     */
    public boolean moveStatus(long orderId, OrderStatus from, OrderStatus to, String reason) {
        return db.update("UPDATE orders SET status = ?, reject_reason = COALESCE(?, reject_reason), updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND status = ?", to.name(), reason, orderId, from.name()) > 0;
    }

    public void setPaymentStatus(long orderId, PaymentStatus from, PaymentStatus to) {
        db.update("UPDATE orders SET payment_status = ? WHERE id = ? AND payment_status = ?", to.name(), orderId, from.name());
    }

    /** Orders in {@code status} not moved since {@code before} (for automatic timeouts). */
    public List<Long> stale(OrderStatus status, Instant before, boolean onlyWithoutDriver) {
        return db.queryForList(onlyWithoutDriver ? STALE_WITHOUT_DRIVER : STALE, Long.class, status.name(), Timestamp.from(before));
    }

    /** Accounts that follow this order live: its customer, its store's staff and its driver. */
    public List<Long> participants(long orderId) {
        List<Long> ids = new ArrayList<>();
        db.query("SELECT customer_id, driver_id, store_id FROM orders WHERE id = ?", rs -> {
            ids.add(rs.getLong("customer_id"));
            long driver = rs.getLong("driver_id");
            if (!rs.wasNull()) {
                ids.add(driver);
            }
            String store = rs.getString("store_id");
            if (store != null) {
                ids.addAll(db.queryForList("SELECT id FROM users WHERE role = 'RESTAURANT' AND store_id = ?", Long.class, store));
            }
        }, orderId);
        return ids;
    }

    private static Order map(ResultSet rs, int n) throws SQLException {
        long driver = rs.getLong("driver_id");
        Long driverId = rs.wasNull() ? null : driver;
        Timestamp scheduled = rs.getTimestamp("scheduled_for");
        return new Order(rs.getLong("id"), rs.getString("code"), OrderKind.valueOf(rs.getString("kind")), rs.getLong("customer_id"),
                rs.getString("store_id"), driverId, OrderStatus.valueOf(rs.getString("status")), rs.getString("zone"),
                rs.getString("phone"), rs.getString("address"), rs.getString("landmark"), (Double) rs.getObject("lat", Double.class),
                (Double) rs.getObject("lng", Double.class), rs.getString("errand_details"), rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"), rs.getBigDecimal("delivery_fee"), rs.getBigDecimal("total"),
                rs.getBigDecimal("cash_change_for"), PaymentMethod.valueOf(rs.getString("payment_method")),
                PaymentStatus.valueOf(rs.getString("payment_status")), rs.getString("promo"), rs.getString("reject_reason"),
                scheduled == null ? null : scheduled.toInstant(), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    static BigDecimal zeroIfNull(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
