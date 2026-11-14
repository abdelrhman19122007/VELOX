package com.velox.catalog.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** SQL for {@code favorites} — always scoped to the signed-in user. */
@Repository
public class FavoriteRepository {

    private final JdbcTemplate db;

    public FavoriteRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<String> findByUser(long userId) {
        return db.queryForList("SELECT fav_key FROM favorites WHERE user_id = ? ORDER BY fav_key", String.class, userId);
    }

    /** Adds the key if missing, removes it if present. */
    public void toggle(long userId, String key) {
        if (db.update("DELETE FROM favorites WHERE user_id = ? AND fav_key = ?", userId, key) == 0) {
            db.update("INSERT INTO favorites (user_id, fav_key) VALUES (?, ?)", userId, key);
        }
    }
}
