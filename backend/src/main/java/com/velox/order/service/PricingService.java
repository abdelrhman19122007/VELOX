package com.velox.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.velox.catalog.domain.Product;
import com.velox.catalog.domain.ProductSize;
import com.velox.catalog.repository.ProductRepository;
import com.velox.catalog.service.CatalogService;
import com.velox.common.error.ApiException;
import com.velox.order.domain.DeliveryZone;
import com.velox.order.domain.OrderItem;
import com.velox.order.domain.OrderKind;
import com.velox.order.dto.OrderDtos.CartLine;
import com.velox.order.dto.OrderDtos.Quote;
import com.velox.order.repository.ZoneRepository;
import com.velox.order.spi.DiscountPolicy;
import com.velox.order.spi.DiscountPolicy.Discount;
import com.velox.security.AuthUser;

/**
 * Prices an order on the server from the database (FR-ORD-03, OWASP A04): whatever prices a client sends are
 * ignored. One store per order; every product must exist, be available, and the store must be taking orders.
 */
@Service
public class PricingService {

    /** Bulky items (gas cylinders, cartons…) go by tuk-tuk. */
    static final BigDecimal LARGE_ITEM_SURCHARGE = new BigDecimal("10.00");
    /** Store preparation time added to the zone's travel time. */
    static final int PREPARATION_MINUTES = 15;

    /** The priced order, ready to save. */
    public record Priced(String storeId, List<OrderItem> items, DeliveryZone zone, BigDecimal subtotal,
                         BigDecimal deliveryFee, BigDecimal discount, BigDecimal total, int etaMinutes) {
        public Quote toQuote() {
            return new Quote(subtotal, deliveryFee, discount, total, etaMinutes);
        }
    }

    private final ProductRepository products;
    private final CatalogService catalog;
    private final ZoneRepository zones;
    private final Optional<DiscountPolicy> discounts;

    public PricingService(ProductRepository products, CatalogService catalog, ZoneRepository zones,
                          Optional<DiscountPolicy> discounts) {
        this.products = products;
        this.catalog = catalog;
        this.zones = zones;
        this.discounts = discounts;
    }

    public Priced price(AuthUser customer, OrderKind kind, String zoneCode, List<CartLine> lines, String promo) {
        DeliveryZone zone = zones.findActive(zoneCode)
                .orElseThrow(() -> ApiException.badRequest("UNKNOWN_ZONE", "We don't deliver to that area yet"));
        if (kind == OrderKind.ERRAND) {
            return withDiscount(customer, null, List.of(), zone, BigDecimal.ZERO, zone.fee(), promo);
        }
        if (lines == null || lines.isEmpty()) {
            throw ApiException.badRequest("EMPTY_CART", "Add something to the cart");
        }
        Map<String, Integer> qty = new LinkedHashMap<>();
        Map<String, String> notes = new LinkedHashMap<>();
        for (CartLine line : lines) {
            qty.merge(line.productId(), line.qty(), Integer::sum);
            if (line.note() != null && !line.note().isBlank()) {
                notes.put(line.productId(), line.note().trim());
            }
        }
        String storeId = null;
        boolean bulky = false;
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (Map.Entry<String, Integer> e : qty.entrySet()) {
            Product p = products.findById(e.getKey())
                    .orElseThrow(() -> ApiException.badRequest("UNKNOWN_PRODUCT", "A product is no longer on the menu"));
            if (storeId != null && !storeId.equals(p.storeId())) {
                throw ApiException.badRequest("MIXED_STORES", "One order, one store");
            }
            storeId = p.storeId();
            if (!p.available()) {
                throw new ApiException(HttpStatus.CONFLICT, "UNAVAILABLE", "Something in the cart is sold out",
                        Map.of("productId", p.id()));
            }
            if (e.getValue() > 99) {
                throw ApiException.badRequest("INVALID_QUANTITY", "Too many of one item");
            }
            bulky |= p.size() == ProductSize.LARGE;
            OrderItem item = new OrderItem(p.id(), p.name(), p.nameAr(), p.price(), e.getValue(), notes.get(p.id()));
            items.add(item);
            subtotal = subtotal.add(item.lineTotal());
        }
        if (!catalog.isOrderable(storeId)) {
            throw new ApiException(HttpStatus.CONFLICT, "STORE_CLOSED", "The store isn't taking orders right now");
        }
        BigDecimal fee = bulky ? zone.fee().add(LARGE_ITEM_SURCHARGE) : zone.fee();
        return withDiscount(customer, storeId, items, zone, subtotal, fee, promo);
    }

    private Priced withDiscount(AuthUser customer, String storeId, List<OrderItem> items, DeliveryZone zone,
                                BigDecimal subtotal, BigDecimal fee, String promo) {
        Discount d = Discount.NONE;
        if (promo != null && !promo.isBlank()) {
            d = discounts.orElseThrow(() -> ApiException.badRequest("INVALID_PROMO", "Promo codes aren't available"))
                    .apply(customer, promo.trim().toUpperCase(), subtotal, fee);
        }
        BigDecimal discount = money(d.total().min(subtotal.add(fee)));
        BigDecimal total = money(subtotal.add(fee).subtract(discount));
        return new Priced(storeId, items, zone, money(subtotal), money(fee), discount, total,
                zone.etaMinutes() + PREPARATION_MINUTES);
    }

    static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
