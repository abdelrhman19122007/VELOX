package com.velox.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.velox.order.domain.DeliveryZone;

/** SQL for {@code delivery_zones}. */
@Repository
public class ZoneRepository {

    private static final RowMapper<DeliveryZone> ZONE = (rs, n) -> new DeliveryZone(rs.getString("code"),
            rs.getString("name_ar"), rs.getString("name_en"), rs.getString("kind"), rs.getBigDecimal("fee"),
            rs.getInt("eta_min"), rs.getBoolean("active"), rs.getInt("sort_order"));

    private final JdbcTemplate db;

    public ZoneRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<DeliveryZone> findActive() {
        return db.query("SELECT * FROM delivery_zones WHERE active ORDER BY sort_order, code", ZONE);
    }

    public List<DeliveryZone> findAll() {
        return db.query("SELECT * FROM delivery_zones ORDER BY sort_order, code", ZONE);
    }

    public Optional<DeliveryZone> findActive(String code) {
        return db.query("SELECT * FROM delivery_zones WHERE code = ? AND active", ZONE, code).stream().findFirst();
    }

    public boolean exists(String code) {
        Integer n = db.queryForObject("SELECT COUNT(*) FROM delivery_zones WHERE code = ?", Integer.class, code);
        return n != null && n > 0;
    }

    public void insert(DeliveryZone z) {
        db.update("INSERT INTO delivery_zones (code, name_ar, name_en, kind, fee, eta_min, active, sort_order) VALUES (?,?,?,?,?,?,?,?)",
                z.code(), z.nameAr(), z.nameEn(), z.kind(), z.fee(), z.etaMinutes(), z.active(), z.sortOrder());
    }

    public void update(DeliveryZone z) {
        db.update("UPDATE delivery_zones SET name_ar = ?, name_en = ?, kind = ?, fee = ?, eta_min = ?, active = ?, sort_order = ? "
                + "WHERE code = ?", z.nameAr(), z.nameEn(), z.kind(), z.fee(), z.etaMinutes(), z.active(), z.sortOrder(), z.code());
    }
}
