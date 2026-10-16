package com.velox.payment.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** SQL for {@code promo_codes}. */
@Repository
public class PromoRepository {

    public record Promo(String code, String kind, BigDecimal value, BigDecimal minOrder, Long userId, Integer maxUses,
                        int usedCount, Instant expiresAt) {
    }

    private static final RowMapper<Promo> PROMO = (rs, n) -> {
        long user = rs.getLong("user_id");
        Long userId = rs.wasNull() ? null : user;
        int max = rs.getInt("max_uses");
        Integer maxUses = rs.wasNull() ? null : max;
        Timestamp exp = rs.getTimestamp("expires_at");
        return new Promo(rs.getString("code"), rs.getString("kind"), rs.getBigDecimal("value"), rs.getBigDecimal("min_order"),
                userId, maxUses, rs.getInt("used_count"), exp == null ? null : exp.toInstant());
    };

    private final JdbcTemplate db;

    public PromoRepository(JdbcTemplate db) {
        this.db = db;
    }

    public Optional<Promo> find(String code) {
        return db.query("SELECT * FROM promo_codes WHERE code = ?", PROMO, code).stream().findFirst();
    }

    public List<Promo> all() {
        return db.query("SELECT * FROM promo_codes ORDER BY created_at DESC", PROMO);
    }

    /** Counts one use, only while uses are left (two customers can't both take the last use). */
    public boolean use(String code) {
        return db.update("UPDATE promo_codes SET used_count = used_count + 1 WHERE code = ? AND (max_uses IS NULL OR used_count < max_uses)",
                code) > 0;
    }

    /** Has this customer already used the code on a non-cancelled order? (one use per customer) */
    public boolean usedBy(String code, long userId) {
        Integer n = db.queryForObject("SELECT COUNT(*) FROM orders WHERE promo = ? AND customer_id = ? "
                + "AND status NOT IN ('CANCELLED', 'REJECTED')", Integer.class, code, userId);
        return n != null && n > 0;
    }

    public void insert(Promo p) {
        db.update("INSERT INTO promo_codes (code, kind, value, min_order, user_id, max_uses, expires_at) VALUES (?,?,?,?,?,?,?)",
                p.code(), p.kind(), p.value(), p.minOrder(), p.userId(), p.maxUses(),
                p.expiresAt() == null ? null : Timestamp.from(p.expiresAt()));
    }
}
