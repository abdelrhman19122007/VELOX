package com.velox.operation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** A driver's shift: online or not, the vehicle, and the last known location. */
@Repository
public class DriverRepository {

    public record DriverState(boolean online, String vehicle, Double lat, Double lng) {

        /** Large items (gas cylinders, cartons) need a tuk-tuk or a car. */
        public boolean carriesLarge() {
            return "TUKTUK".equals(vehicle) || "CAR".equals(vehicle);
        }
    }

    private final JdbcTemplate db;

    public DriverRepository(JdbcTemplate db) {
        this.db = db;
    }

    public Optional<DriverState> find(long userId) {
        return db.query("SELECT online, vehicle, lat, lng FROM driver_status WHERE user_id = ?",
                (rs, i) -> new DriverState(rs.getBoolean(1), rs.getString(2), (Double) rs.getObject(3, Double.class),
                        (Double) rs.getObject(4, Double.class)), userId).stream().findFirst();
    }

    public void setStatus(long userId, boolean online, String vehicle) {
        db.update("INSERT INTO driver_status (user_id, online, vehicle) VALUES (?,?,COALESCE(?, 'MOTORBIKE')) "
                + "ON CONFLICT (user_id) DO UPDATE SET online = EXCLUDED.online, "
                + "vehicle = COALESCE(?, driver_status.vehicle), updated_at = CURRENT_TIMESTAMP", userId, online, vehicle, vehicle);
    }

    public boolean setLocation(long userId, double lat, double lng) {
        return db.update("UPDATE driver_status SET lat = ?, lng = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?",
                lat, lng, userId) > 0;
    }

    /** Online drivers whose account isn't blocked — the ones who get offers. */
    public List<Long> online() {
        return db.queryForList("SELECT d.user_id FROM driver_status d JOIN users u ON u.id = d.user_id "
                + "WHERE d.online AND NOT u.blocked AND u.role = 'DRIVER'", Long.class);
    }
}
