package com.velox.catalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.velox.catalog.domain.Product;
import com.velox.catalog.domain.ProductSize;
import com.velox.catalog.domain.StoreType;

/** SQL for {@code products}. Parameterised queries only (OWASP A03). */
@Repository
public class ProductRepository {

    private static final RowMapper<Product> PRODUCT = (rs, n) -> new Product(rs.getString("id"), rs.getString("store_id"),
            rs.getString("name"), rs.getString("name_ar"), rs.getString("description"), rs.getBigDecimal("price"),
            ProductSize.valueOf(rs.getString("size")), rs.getString("section"), rs.getString("photo_url"),
            rs.getBoolean("available"));

    private final JdbcTemplate db;

    public ProductRepository(JdbcTemplate db) {
        this.db = db;
    }

    public List<Product> findByStore(String storeId) {
        return db.query("SELECT * FROM products WHERE store_id = ? ORDER BY sort_order, id", PRODUCT, storeId);
    }

    public Optional<Product> findById(String id) {
        return db.query("SELECT * FROM products WHERE id = ?", PRODUCT, id).stream().findFirst();
    }

    /** Products whose name matches. {@code pattern} is an escaped ILIKE pattern. */
    public List<Product> searchByName(String pattern) {
        return db.query("SELECT * FROM products WHERE name ILIKE ? ESCAPE '\\' OR name_ar ILIKE ? ESCAPE '\\' "
                + "ORDER BY available DESC, id LIMIT 50", PRODUCT, pattern, pattern);
    }

    public void insert(Product p) {
        Integer sort = db.queryForObject("SELECT COALESCE(MAX(sort_order), 0) + 1 FROM products WHERE store_id = ?",
                Integer.class, p.storeId());
        db.update("INSERT INTO products (id, store_id, name, name_ar, description, price, size, section, photo_url, available, "
                        + "sort_order) VALUES (?,?,?,?,?,?,?,?,?,?,?)", p.id(), p.storeId(), p.name(), p.nameAr(), p.description(),
                StoreRepository.money(p.price()), p.size().name(), p.section(), p.photoUrl(), p.available(), sort);
    }

    public void update(Product p) {
        db.update("UPDATE products SET name = ?, name_ar = ?, description = ?, price = ?, size = ?, section = ?, photo_url = ?, "
                        + "available = ? WHERE id = ?", p.name(), p.nameAr(), p.description(), StoreRepository.money(p.price()),
                p.size().name(), p.section(), p.photoUrl(), p.available(), p.id());
    }

    public String nextId(StoreType type) {
        return IdSequence.next(db, "products", type.productPrefix());
    }
}
