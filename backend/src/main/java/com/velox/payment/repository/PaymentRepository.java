package com.velox.payment.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.velox.order.domain.PaymentMethod;
import com.velox.payment.domain.Payment;

/** SQL for {@code payments}: every charge and refund is a row — the full money trail. */
@Repository
public class PaymentRepository {

    private static final RowMapper<Payment> PAYMENT = (rs, n) -> {
        Timestamp expires = rs.getTimestamp("expires_at");
        Timestamp paid = rs.getTimestamp("paid_at");
        return new Payment(rs.getLong("id"), rs.getLong("order_id"), PaymentMethod.valueOf(rs.getString("method")),
                rs.getString("kind"), rs.getBigDecimal("amount"), rs.getString("status"), rs.getString("reference"),
                rs.getString("wallet_phone"), rs.getString("code_hash"), rs.getInt("attempts"),
                expires == null ? null : expires.toInstant(), paid == null ? null : paid.toInstant());
    };

    private final JdbcTemplate db;

    public PaymentRepository(JdbcTemplate db) {
        this.db = db;
    }

    public void insertCharge(long orderId, PaymentMethod method, BigDecimal amount, String status, String reference,
                             String walletPhone, String codeHash, Instant expiresAt) {
        db.update("INSERT INTO payments (order_id, method, kind, amount, status, reference, wallet_phone, code_hash, expires_at, paid_at) "
                        + "VALUES (?,?,'CHARGE',?,?,?,?,?,?,?)", orderId, method.name(), amount, status, reference, walletPhone,
                codeHash, expiresAt == null ? null : Timestamp.from(expiresAt),
                "PAID".equals(status) ? Timestamp.from(Instant.now()) : null);
    }

    public void insertRefund(long orderId, PaymentMethod method, BigDecimal amount) {
        db.update("INSERT INTO payments (order_id, method, kind, amount, status, paid_at) VALUES (?,?,'REFUND',?,'REFUNDED',CURRENT_TIMESTAMP)",
                orderId, method.name(), amount);
    }

    /** The open charge of an order (the one waiting to be paid), if any. */
    public Optional<Payment> pendingCharge(long orderId) {
        return db.query("SELECT * FROM payments WHERE order_id = ? AND kind = 'CHARGE' AND status = 'PENDING' ORDER BY id DESC LIMIT 1",
                PAYMENT, orderId).stream().findFirst();
    }

    public Optional<Payment> latestCharge(long orderId) {
        return db.query("SELECT * FROM payments WHERE order_id = ? AND kind = 'CHARGE' ORDER BY id DESC LIMIT 1",
                PAYMENT, orderId).stream().findFirst();
    }

    public void countWrongCode(long paymentId) {
        db.update("UPDATE payments SET attempts = attempts + 1 WHERE id = ?", paymentId);
    }

    /** Only a still-pending charge can be marked paid — a reference can't be "paid" twice (replay). */
    public boolean markPaid(long paymentId) {
        return db.update("UPDATE payments SET status = 'PAID', paid_at = CURRENT_TIMESTAMP, code_hash = NULL "
                + "WHERE id = ? AND status = 'PENDING'", paymentId) > 0;
    }

    public void markExpired(long orderId) {
        db.update("UPDATE payments SET status = 'EXPIRED' WHERE order_id = ? AND kind = 'CHARGE' AND status = 'PENDING'", orderId);
    }

    /** Order ids whose pending charge ran out of time. */
    public List<Long> expiredPendingOrders(Instant now) {
        return db.queryForList("SELECT order_id FROM payments WHERE kind = 'CHARGE' AND status = 'PENDING' AND expires_at < ?",
                Long.class, Timestamp.from(now));
    }
}
