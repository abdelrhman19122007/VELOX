package com.velox.catalog.service;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.velox.catalog.domain.Store;
import com.velox.catalog.dto.CatalogDtos.ProductView;
import com.velox.catalog.dto.CatalogDtos.SearchResult;
import com.velox.catalog.dto.CatalogDtos.Section;
import com.velox.catalog.dto.CatalogDtos.StoreDetail;
import com.velox.catalog.dto.CatalogDtos.StoreSummary;
import com.velox.catalog.repository.ProductRepository;
import com.velox.catalog.repository.StoreRepository;
import com.velox.catalog.repository.StoreRepository.RatedStore;
import com.velox.common.error.ApiException;

/** Public catalogue: stores, menus and search (FR-CAT-01…05). Opening hours follow Samannoud's clock. */
@Service
public class CatalogService {

    public static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");

    private final StoreRepository stores;
    private final ProductRepository products;
    private final Clock clock;

    public CatalogService(StoreRepository stores, ProductRepository products, Clock clock) {
        this.stores = stores;
        this.products = products;
        this.clock = clock;
    }

    public List<StoreSummary> listStores(boolean openOnly) {
        LocalTime now = nowInCairo();
        return stores.findAllRated().stream()
                .map(r -> StoreSummary.of(r, r.store().isOrderableAt(now)))
                .filter(s -> !openOnly || s.openNow())
                .toList();
    }

    public StoreDetail store(String id) {
        RatedStore rated = stores.findRated(id).orElseThrow(() -> ApiException.notFound("UNKNOWN_STORE", "No such store"));
        Store s = rated.store();
        Map<String, List<ProductView>> bySection = new LinkedHashMap<>();
        products.findByStore(id).forEach(p ->
                bySection.computeIfAbsent(p.section(), k -> new java.util.ArrayList<>()).add(ProductView.of(p)));
        List<Section> sections = bySection.entrySet().stream().map(e -> new Section(e.getKey(), e.getValue())).toList();
        return new StoreDetail(StoreSummary.of(rated, s.isOrderableAt(nowInCairo())), s.phone(), s.address(), sections);
    }

    /** Search stores and products by Arabic or English name. Short or empty queries return nothing. */
    public SearchResult search(String query) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            return new SearchResult(List.of(), List.of());
        }
        if (q.length() > 50) {
            q = q.substring(0, 50);
        }
        String pattern = "%" + escapeLike(q) + "%";
        LocalTime now = nowInCairo();
        return new SearchResult(
                stores.searchByName(pattern).stream().map(r -> StoreSummary.of(r, r.store().isOrderableAt(now))).toList(),
                products.searchByName(pattern).stream().map(ProductView::of).toList());
    }

    /** Is the store taking orders now? Used by the orders module before accepting a cart. */
    public boolean isOrderable(String storeId) {
        return stores.findById(storeId).map(s -> s.isOrderableAt(nowInCairo())).orElse(false);
    }

    LocalTime nowInCairo() {
        return LocalTime.now(clock.withZone(CAIRO));
    }

    /** % and _ in what people type are literal characters, not wildcards (the search can't be abused). */
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
