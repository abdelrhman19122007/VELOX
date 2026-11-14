package com.velox.catalog.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.velox.catalog.domain.Product;
import com.velox.catalog.dto.CatalogDtos.ProductUpdateRequest;
import com.velox.catalog.dto.CatalogDtos.StoreDetail;
import com.velox.catalog.repository.ProductRepository;
import com.velox.catalog.repository.StoreRepository;
import com.velox.common.error.ApiException;
import com.velox.security.AuthUser;
import com.velox.security.Role;

/**
 * What store staff change themselves: the "accepting orders" switch, product availability and prices
 * (FR-CAT-05, -06). A store account can only touch its own store — checked here, not trusted from the client.
 */
@Service
public class RestaurantCatalogService {

    private final StoreRepository stores;
    private final ProductRepository products;
    private final CatalogService catalog;

    public RestaurantCatalogService(StoreRepository stores, ProductRepository products, CatalogService catalog) {
        this.stores = stores;
        this.products = products;
        this.catalog = catalog;
    }

    public StoreDetail myStore(AuthUser me) {
        return catalog.store(storeOf(me));
    }

    public void setAccepting(AuthUser me, boolean accepting) {
        stores.setAccepting(storeOf(me), accepting);
    }

    @Transactional
    public void updateProduct(AuthUser me, String productId, ProductUpdateRequest r) {
        String storeId = storeOf(me);
        Product p = products.findById(productId)
                .filter(x -> x.storeId().equals(storeId))   // another store's product looks like "not found" (no IDOR)
                .orElseThrow(() -> ApiException.notFound("UNKNOWN_PRODUCT", "Not one of your products"));
        products.update(new Product(p.id(), p.storeId(), p.name(), p.nameAr(), p.description(),
                r.price() == null ? p.price() : r.price(), p.size(), p.section(), p.photoUrl(),
                r.available() == null ? p.available() : r.available(), p.descriptionEn()));
    }

    private static String storeOf(AuthUser me) {
        me.require(Role.RESTAURANT);
        if (me.storeId() == null) {
            throw ApiException.forbidden();
        }
        return me.storeId();
    }
}
