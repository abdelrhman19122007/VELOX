package com.velox.auth.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.velox.auth.domain.Address;

/** SQL for {@code addresses}. Every statement filters by user_id, so one account can't touch another's addresses. */
@Repository
public class AddressRepository {

    private static final RowMapper<Address> ADDRESS = (rs, n) -> new Address(rs.getLong("id"), rs.getString("label"),
            rs.getString("zone"), rs.getString("address"), rs.getString("landmark"), rs.getString("phone"),
            (Double) rs.getObject("lat", Double.class), (Double) rs.getObject("lng", Double.class));

    private final JdbcTemplate db;

    public AddressRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<Address> findByUser(long userId) {
        return db.query("SELECT * FROM addresses WHERE user_id = ? ORDER BY id", ADDRESS, userId);
    }

    public int countByUser(long userId) {
        Integer n = db.queryForObject("SELECT COUNT(*) FROM addresses WHERE user_id = ?", Integer.class, userId);
        return n == null ? 0 : n;
    }

    public void insert(long userId, Address a) {
        db.update("INSERT INTO addresses (user_id, label, zone, address, landmark, phone, lat, lng) VALUES (?,?,?,?,?,?,?,?)",
                userId, a.label(), a.zone(), a.address(), a.landmark(), a.phone(), a.lat(), a.lng());
    }

    public void delete(long userId, long addressId) {
        db.update("DELETE FROM addresses WHERE id = ? AND user_id = ?", addressId, userId);
    }

    public boolean zoneExists(String zone) {
        Integer n = db.queryForObject("SELECT COUNT(*) FROM delivery_zones WHERE code = ? AND active", Integer.class, zone);
        return n != null && n > 0;
    }
}
