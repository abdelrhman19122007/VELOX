package com.velox.catalog.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.velox.catalog.domain.OpeningHours;
import com.velox.catalog.domain.Store;
import com.velox.catalog.domain.StoreType;

/** SQL for {@code stores}. Parameterised queries only (OWASP A03). */
@Repository
public class StoreRepository {

    /** A store plus its rating from customer reviews. */
    public record RatedStore(Store store, Double rating, int ratingCount) {
    }

    private static final String RATED = "SELECT s.*, r.avg_rating, COALESCE(r.cnt, 0) AS rating_count FROM stores s "
            + "LEFT JOIN (SELECT o.store_id, AVG(v.store_rating) AS avg_rating, COUNT(*) AS cnt FROM reviews v "
            + "JOIN orders o ON o.id = v.order_id GROUP BY o.store_id) r ON r.store_id = s.id ";

    static final RowMapper<Store> STORE = StoreRepository::map;
    private static final RowMapper<RatedStore> RATED_STORE = (rs, n) -> {
        Number avg = (Number) rs.getObject("avg_rating");
        return new RatedStore(map(rs, n), avg == null ? null : Math.round(avg.doubleValue() * 10) / 10.0,
                rs.getInt("rating_count"));
    };

    private final JdbcTemplate db;

    public StoreRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<RatedStore> findAllRated() {
        return db.query(RATED + "ORDER BY s.sort_order, s.id", RATED_STORE);
    }

    public Optional<RatedStore> findRated(String id) {
        return db.query(RATED + "WHERE s.id = ?", RATED_STORE, id).stream().findFirst();
    }

    public Optional<Store> findById(String id) {
        return db.query("SELECT * FROM stores WHERE id = ?", STORE, id).stream().findFirst();
    }

    /** Stores whose name matches (Arabic or English). {@code pattern} is an escaped ILIKE pattern. */
    public List<RatedStore> searchByName(String pattern) {
        return db.query(RATED + "WHERE s.name ILIKE ? ESCAPE '\\' OR s.name_ar ILIKE ? ESCAPE '\\' "
                + "ORDER BY s.sort_order LIMIT 30", RATED_STORE, pattern, pattern);
    }

    public void insert(Store s) {
        Integer sort = db.queryForObject("SELECT COALESCE(MAX(sort_order), 0) + 1 FROM stores", Integer.class);
        db.update("INSERT INTO stores (id, type, cuisine, name, name_ar, tagline, tagline_ar, phone, address, photo_url, "
                        + "opens, closes, accepting, lat, lng, commission, sort_order) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                s.id(), s.type().name(), s.cuisine(), s.name(), s.nameAr(), s.tagline(), s.taglineAr(), s.phone(),
                s.address(), s.photoUrl(), time(s.hours().opens()), time(s.hours().closes()), s.accepting(), s.lat(),
                s.lng(), s.commission(), sort);
    }

    public void update(Store s) {
        db.update("UPDATE stores SET cuisine = ?, name = ?, name_ar = ?, tagline = ?, tagline_ar = ?, phone = ?, address = ?, "
                        + "photo_url = ?, opens = ?, closes = ?, accepting = ?, lat = ?, lng = ?, commission = ? WHERE id = ?",
                s.cuisine(), s.name(), s.nameAr(), s.tagline(), s.taglineAr(), s.phone(), s.address(), s.photoUrl(),
                time(s.hours().opens()), time(s.hours().closes()), s.accepting(), s.lat(), s.lng(), s.commission(), s.id());
    }

    public void setAccepting(String storeId, boolean accepting) {
        db.update("UPDATE stores SET accepting = ? WHERE id = ?", accepting, storeId);
    }

    /** Next free id for a type: R7 after R6. */
    public String nextId(StoreType type) {
        return IdSequence.next(db, "stores", type.storePrefix());
    }

    private static Store map(ResultSet rs, int n) throws SQLException {
        Time opens = rs.getTime("opens");
        Time closes = rs.getTime("closes");
        return new Store(rs.getString("id"), StoreType.valueOf(rs.getString("type")), rs.getString("cuisine"),
                rs.getString("name"), rs.getString("name_ar"), rs.getString("tagline"), rs.getString("tagline_ar"),
                rs.getString("phone"), rs.getString("address"), rs.getString("photo_url"),
                new OpeningHours(opens == null ? null : opens.toLocalTime(), closes == null ? null : closes.toLocalTime()),
                rs.getBoolean("accepting"), (Double) rs.getObject("lat", Double.class),
                (Double) rs.getObject("lng", Double.class), rs.getBigDecimal("commission"));
    }

    private static Time time(java.time.LocalTime t) {
        return t == null ? null : Time.valueOf(t);
    }

    static BigDecimal money(BigDecimal v) {
        return v == null ? null : v.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
