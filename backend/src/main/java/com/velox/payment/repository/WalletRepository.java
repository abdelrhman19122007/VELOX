package com.velox.payment.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** In-app wallet: the balance is always the sum of its transactions (never a stored number that can drift). */
@Repository
public class WalletRepository {

    public record Transaction(BigDecimal amount, String reason, Long orderId, Instant at) {
    }

    private final JdbcTemplate db;

    public WalletRepository(JdbcTemplate db) {
        this.db = db;
    }

    public BigDecimal balance(long userId) {
        BigDecimal b = db.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM wallet_transactions WHERE user_id = ?",
                BigDecimal.class, userId);
        return b == null ? BigDecimal.ZERO : b;
    }

    /** Locks the account's wallet rows for this transaction, so two payments can't spend the same money. */
    public BigDecimal balanceForUpdate(long userId) {
        db.queryForList("SELECT id FROM users WHERE id = ? FOR UPDATE", Long.class, userId);
        return balance(userId);
    }

    public void add(long userId, BigDecimal amount, String reason, Long orderId) {
        db.update("INSERT INTO wallet_transactions (user_id, amount, reason, order_id) VALUES (?,?,?,?)", userId, amount, reason, orderId);
    }

    public List<Transaction> history(long userId) {
        return db.query("SELECT amount, reason, order_id, created_at FROM wallet_transactions WHERE user_id = ? ORDER BY id DESC LIMIT 50",
                (rs, n) -> {
                    long order = rs.getLong("order_id");
                    return new Transaction(rs.getBigDecimal("amount"), rs.getString("reason"), rs.wasNull() ? null : order,
                            rs.getTimestamp("created_at").toInstant());
                }, userId);
    }
}
