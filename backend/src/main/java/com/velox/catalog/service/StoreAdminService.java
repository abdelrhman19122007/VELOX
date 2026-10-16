package com.velox.catalog.service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.velox.auth.repository.UserRepository;
import com.velox.catalog.domain.OpeningHours;
import com.velox.catalog.domain.Product;
import com.velox.catalog.domain.ProductSize;
import com.velox.catalog.domain.Store;
import com.velox.catalog.dto.CatalogDtos.ProductRequest;
import com.velox.catalog.dto.CatalogDtos.StoreRequest;
import com.velox.catalog.dto.CatalogDtos.StoreUpdateRequest;
import com.velox.catalog.repository.ProductRepository;
import com.velox.catalog.repository.StoreRepository;
import com.velox.common.error.ApiException;
import com.velox.security.AuditLog;
import com.velox.security.AuthUser;
import com.velox.security.PasswordHasher;
import com.velox.security.Role;

/** Admin: add stores (with their sign-in) and products, edit them (FR-CAT-07). Every change is audited. */
@Service
public class StoreAdminService {

    private static final BigDecimal DEFAULT_COMMISSION = new BigDecimal("10.00");

    private final StoreRepository stores;
    private final ProductRepository products;
    private final UserRepository users;
    private final PasswordHasher hasher;
    private final AuditLog audit;

    public StoreAdminService(StoreRepository stores, ProductRepository products, UserRepository users,
                             PasswordHasher hasher, AuditLog audit) {
        this.stores = stores;
        this.products = products;
        this.users = users;
        this.hasher = hasher;
        this.audit = audit;
    }

    /** Creates the store and a RESTAURANT account for its staff, in one transaction. */
    @Transactional
    public String createStore(AuthUser admin, StoreRequest r) {
        admin.require(Role.ADMIN);
        String email = r.accountEmail().trim().toLowerCase(Locale.ROOT);
        if (users.emailExists(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "This email already has an account");
        }
        String id = stores.nextId(r.type());
        stores.insert(new Store(id, r.type(), r.cuisine(), r.name().trim(), r.nameAr().trim(), blank(r.tagline()),
                blank(r.taglineAr()), blank(r.phone()), blank(r.address()), blank(r.photoUrl()), hours(r.opens(), r.closes()),
                true, r.lat(), r.lng(), r.commission() == null ? DEFAULT_COMMISSION : r.commission()));
        users.insert(email, r.name().trim(), blank(r.phone()), Role.RESTAURANT, id, hasher.hash(r.accountPassword()));
        audit.record(admin, "STORE_CREATE", "store", id, r.name());
        return id;
    }

    @Transactional
    public void updateStore(AuthUser admin, String id, StoreUpdateRequest r) {
        admin.require(Role.ADMIN);
        Store s = stores.findById(id).orElseThrow(() -> ApiException.notFound("UNKNOWN_STORE", "No such store"));
        OpeningHours hours = r.opens() != null || r.closes() != null ? hours(r.opens(), r.closes()) : s.hours();
        stores.update(new Store(id, s.type(), or(r.cuisine(), s.cuisine()), or(trim(r.name()), s.name()),
                or(trim(r.nameAr()), s.nameAr()), s.tagline(), r.taglineAr() == null ? s.taglineAr() : blank(r.taglineAr()),
                s.phone(), s.address(), r.photoUrl() == null ? s.photoUrl() : blank(r.photoUrl()), hours,
                r.accepting() == null ? s.accepting() : r.accepting(), s.lat(), s.lng(), or(r.commission(), s.commission())));
        audit.record(admin, "STORE_UPDATE", "store", id, null);
    }

    @Transactional
    public String addProduct(AuthUser admin, String storeId, ProductRequest r) {
        admin.require(Role.ADMIN);
        Store s = stores.findById(storeId).orElseThrow(() -> ApiException.notFound("UNKNOWN_STORE", "No such store"));
        String id = products.nextId(s.type());
        products.insert(new Product(id, storeId, r.name().trim(), r.nameAr().trim(), blank(r.description()), r.price(),
                r.size() == null ? ProductSize.MEDIUM : r.size(), r.section(), blank(r.photoUrl()), true));
        audit.record(admin, "PRODUCT_CREATE", "product", id, storeId);
        return id;
    }

    @Transactional
    public void updateProduct(AuthUser admin, String productId, ProductRequest r) {
        admin.require(Role.ADMIN);
        Product p = products.findById(productId).orElseThrow(() -> ApiException.notFound("UNKNOWN_PRODUCT", "No such product"));
        products.update(new Product(p.id(), p.storeId(), r.name().trim(), r.nameAr().trim(), blank(r.description()), r.price(),
                r.size() == null ? p.size() : r.size(), r.section(), blank(r.photoUrl()), p.available()));
        audit.record(admin, "PRODUCT_UPDATE", "product", productId, null);
    }

    /** Both times or neither; "no hours" = always open. */
    static OpeningHours hours(String opens, String closes) {
        boolean o = opens != null && !opens.isBlank();
        boolean c = closes != null && !closes.isBlank();
        if (o != c) {
            throw ApiException.badRequest("INVALID_HOURS", "Give both the opening and the closing time");
        }
        return o ? new OpeningHours(LocalTime.parse(opens), LocalTime.parse(closes)) : OpeningHours.ALWAYS;
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static <T> T or(T value, T fallback) {
        return value == null ? fallback : value;
    }
}
