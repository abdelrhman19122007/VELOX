package com.app.service;

import com.app.enums.ProductType;
import com.app.model.order.Order;
import com.app.model.product.Product;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Personalized offers ("ads") derived from what the customer buys most.
 * Pure logic, no framework dependency: given order history it returns
 * short offer lines for the console and the API.
 */
public final class OfferService {

    private OfferService() {
    }

    public static List<String> personalizedOffers(List<Order> history) {
        List<String> offers = new ArrayList<>();
        if (history == null || history.isEmpty()) {
            offers.add("New here? Use FIRSTORDER for 35% OFF your first order.");
            offers.add("Use VELOX10 for 15% OFF, or FREESHIP for free delivery.");
            return offers;
        }

        Map<ProductType, Integer> byType = new EnumMap<>(ProductType.class);
        Map<String, Integer> byProduct = new HashMap<>();
        int totalUnits = 0;
        for (Order o : history) {
            if (o == null || o.isReturned()) {
                continue;
            }
            for (Product p : o.getProducts()) {
                if (p == null) {
                    continue;
                }
                totalUnits++;
                byType.merge(p.getType(), 1, Integer::sum);
                byProduct.merge(p.getName(), 1, Integer::sum);
            }
        }
        if (totalUnits == 0) {
            offers.add("Use VELOX10 for 15% OFF your next order.");
            return offers;
        }

        ProductType top = byType.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
        if (top != null) {
            int n = byType.get(top);
            offers.add("Top category for you: " + label(top) + " (" + n + "/" + totalUnits + " items)"
                    + " -> " + categoryOffer(top));
        }

        byProduct.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(3)
                .forEach(e -> offers.add("You buy '" + e.getKey() + "' x" + e.getValue()
                        + " - reorder it now with VELOX10 for 15% OFF."));

        // Cross-sell the least-explored shelf
        for (ProductType t : ProductType.values()) {
            if (!byType.containsKey(t)) {
                offers.add("Try something new: " + label(t) + " -> " + categoryOffer(t));
                break;
            }
        }
        return offers;
    }

    public static String topCategoryName(List<Order> history) {
        if (history == null) {
            return "-";
        }
        Map<ProductType, Integer> byType = new EnumMap<>(ProductType.class);
        for (Order o : history) {
            if (o == null || o.isReturned()) {
                continue;
            }
            for (Product p : o.getProducts()) {
                if (p != null) {
                    byType.merge(p.getType(), 1, Integer::sum);
                }
            }
        }
        return byType.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(e -> label(e.getKey()))
                .orElse("-");
    }

    private static String label(ProductType t) {
        return switch (t) {
            case FOOD -> "Food & Restaurants";
            case CLOTHES -> "Fashion & Clothes";
            case TECH -> "Tech & Electronics";
        };
    }

    private static String categoryOffer(ProductType t) {
        return switch (t) {
            case FOOD -> "FREESHIP weekend on restaurant orders.";
            case CLOTHES -> "VELOX10 extra 15% OFF fashion this week.";
            case TECH -> "Free gift-packaging on electronics with code FREESHIP.";
        };
    }
}
