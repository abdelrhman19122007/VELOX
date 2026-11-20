package com.velox.payment.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Loyalty points: balance = sum of rows (earned positive, spent negative). */
@Repository
public class LoyaltyRepository {

    private final JdbcTemplate db;

    public LoyaltyRepository(JdbcTemplate db) {
        this.db = db;
    }

    public int balance(long userId) {
        Integer n = db.queryForObject("SELECT COALESCE(SUM(points), 0) FROM loyalty_points WHERE user_id = ?", Integer.class, userId);
        return n == null ? 0 : n;
    }

    /** Earned once per order, even if the event is delivered twice. */
    public void earnOnce(long userId, int points, long orderId) {
        db.update("INSERT INTO loyalty_points (user_id, points, order_id) SELECT ?, ?, ? WHERE NOT EXISTS "
                + "(SELECT 1 FROM loyalty_points WHERE order_id = ? AND points > 0)", userId, points, orderId, orderId);
    }

    public void add(long userId, int points, Long orderId) {
        db.update("INSERT INTO loyalty_points (user_id, points, order_id) VALUES (?,?,?)", userId, points, orderId);
    }
}
