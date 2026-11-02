package com.velox.auth.repository;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.velox.auth.domain.User;
import com.velox.security.Role;

/** All SQL for {@code users}. Every query is parameterised (OWASP A03) — never build SQL from user input. */
@Repository
public class UserRepository {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final RowMapper<User> USER = (rs, n) -> new User(rs.getLong("id"), rs.getString("public_id"),
            rs.getString("email"), rs.getString("name"), rs.getString("phone"), Role.valueOf(rs.getString("role")),
            rs.getString("store_id"), rs.getString("password_hash"), rs.getInt("token_version"), rs.getBoolean("blocked"),
            rs.getTimestamp("created_at").toInstant());

    private final JdbcTemplate db;

    public UserRepository(JdbcTemplate db) {
        this.db = db;
    }

    public Optional<User> findByEmail(String email) {
        return db.query("SELECT * FROM users WHERE email = ?", USER, email).stream().findFirst();
    }

    public Optional<User> findByPublicId(String publicId) {
        return db.query("SELECT * FROM users WHERE public_id = ?", USER, publicId).stream().findFirst();
    }

    public Optional<User> findActiveByPublicId(String publicId) {
        return db.query("SELECT * FROM users WHERE public_id = ? AND NOT blocked", USER, publicId).stream().findFirst();
    }

    public boolean emailExists(String email) {
        Integer n = db.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
        return n != null && n > 0;
    }

    public boolean anyAdmin() {
        Integer n = db.queryForObject("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'", Integer.class);
        return n != null && n > 0;
    }

    /** Creates the account and returns it. The public id is random, so ids can't be guessed (IDOR). */
    public User insert(String email, String name, String phone, Role role, String storeId, String passwordHash) {
        String publicId = newPublicId();
        db.update("INSERT INTO users (public_id, email, name, phone, role, store_id, password_hash) VALUES (?,?,?,?,?,?,?)",
                publicId, email, name, phone, role.name(), storeId, passwordHash);
        return findByPublicId(publicId).orElseThrow();
    }

    public void updatePassword(long userId, String passwordHash) {
        db.update("UPDATE users SET password_hash = ?, token_version = token_version + 1 WHERE id = ?", passwordHash, userId);
    }

    /** Ends every session of the account (tokens carry the old version). */
    public void bumpTokenVersion(long userId) {
        db.update("UPDATE users SET token_version = token_version + 1 WHERE id = ?", userId);
    }

    /**
     * "Delete my account": the personal data goes (name, email, phone, picture, password, addresses, devices,
     * favorites, notifications). The row stays, anonymous and blocked, because past orders must still add up.
     */
    public void anonymize(long userId) {
        db.update("UPDATE users SET email = 'deleted-' || public_id || '@deleted.velox', name = 'Deleted user', phone = NULL, "
                + "picture = NULL, password_hash = NULL, blocked = TRUE, token_version = token_version + 1 WHERE id = ?", userId);
        db.update("DELETE FROM addresses WHERE user_id = ?", userId);
        db.update("DELETE FROM push_devices WHERE user_id = ?", userId);
        db.update("DELETE FROM favorites WHERE user_id = ?", userId);
        db.update("DELETE FROM notifications WHERE user_id = ?", userId);
    }

    public boolean setBlocked(String publicId, boolean blocked) {
        return db.update("UPDATE users SET blocked = ?, token_version = token_version + 1 WHERE public_id = ?",
                blocked, publicId) > 0;
    }

    public List<User> findByRole(Role role, int limit) {
        if (role == null) {
            return db.query("SELECT * FROM users ORDER BY id DESC LIMIT ?", USER, limit);
        }
        return db.query("SELECT * FROM users WHERE role = ? ORDER BY id DESC LIMIT ?", USER, role.name(), limit);
    }

    private static String newPublicId() {
        byte[] b = new byte[9];
        RANDOM.nextBytes(b);
        return "U" + Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
