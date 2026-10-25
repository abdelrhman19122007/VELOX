package com.velox.operation.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Reviews of delivered orders and customer complaints. */
@Repository
public class FeedbackRepository {

    public record Review(String orderCode, int storeRating, Integer driverRating, String comment, Instant at) {
    }

    public record Complaint(String code, String orderCode, long customerId, String details, String status,
                            String compensationCode, String adminNote, Instant at) {
    }

    private static final String COMPLAINT_SELECT = "SELECT c.code, o.code, o.customer_id, c.details, c.status, c.compensation_code, "
            + "c.admin_note, c.created_at FROM complaints c JOIN orders o ON o.id = c.order_id ";
    private static final RowMapper<Complaint> COMPLAINT = (rs, i) -> new Complaint(rs.getString(1), rs.getString(2), rs.getLong(3),
            rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getTimestamp(8).toInstant());

    private final JdbcTemplate db;

    public FeedbackRepository(JdbcTemplate db) {
        this.db = db;
    }

    /** One review per order: false if it was already reviewed. */
    public boolean insertReview(long orderId, int storeRating, Integer driverRating, String comment) {
        return db.update("INSERT INTO reviews (order_id, store_rating, driver_rating, comment) VALUES (?,?,?,?) "
                + "ON CONFLICT (order_id) DO NOTHING", orderId, storeRating, driverRating, comment) > 0;
    }

    public List<Review> storeReviews(String storeId) {
        return db.query("SELECT o.code, r.store_rating, r.driver_rating, r.comment, r.created_at FROM reviews r "
                        + "JOIN orders o ON o.id = r.order_id WHERE o.store_id = ? ORDER BY r.created_at DESC LIMIT 100",
                (rs, i) -> new Review(rs.getString(1), rs.getInt(2), (Integer) rs.getObject(3, Integer.class), rs.getString(4),
                        rs.getTimestamp(5).toInstant()), storeId);
    }

    public boolean hasOpenComplaint(long orderId) {
        return Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS (SELECT 1 FROM complaints WHERE order_id = ? AND status = 'OPEN')",
                Boolean.class, orderId));
    }

    public void insertComplaint(String code, long orderId, String details) {
        db.update("INSERT INTO complaints (code, order_id, details) VALUES (?,?,?)", code, orderId, details);
    }

    public Optional<Complaint> findComplaint(String code) {
        return db.query(COMPLAINT_SELECT + "WHERE c.code = ?", COMPLAINT, code).stream().findFirst();
    }

    public List<Complaint> complaintsOf(long customerId) {
        return db.query(COMPLAINT_SELECT + "WHERE o.customer_id = ? ORDER BY c.id DESC", COMPLAINT, customerId);
    }

    public List<Complaint> complaints(String status) {
        return status == null
                ? db.query(COMPLAINT_SELECT + "ORDER BY c.id DESC LIMIT 200", COMPLAINT)
                : db.query(COMPLAINT_SELECT + "WHERE c.status = ? ORDER BY c.id DESC LIMIT 200", COMPLAINT, status);
    }

    public int openComplaints() {
        Integer n = db.queryForObject("SELECT COUNT(*) FROM complaints WHERE status = 'OPEN'", Integer.class);
        return n == null ? 0 : n;
    }

    /** Only an open complaint can be closed (closing twice would hand out two compensations). */
    public boolean close(String code, String note, String compensationCode) {
        return db.update("UPDATE complaints SET status = 'CLOSED', admin_note = ?, compensation_code = ?, closed_at = CURRENT_TIMESTAMP "
                + "WHERE code = ? AND status = 'OPEN'", note, compensationCode, code) > 0;
    }
}
