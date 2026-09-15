package com.app.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Feature 1 (multi-store single checkout): pure pricing rules.
 * First store ships at the governorate fee; every extra store adds
 * a flat consolidation fee (one combined courier run).
 */
public final class MultiStorePricing {

    /** Flat fee added per store beyond the first one. */
    public static final double EXTRA_FEE_PER_STORE = 10.0;

    private MultiStorePricing() {
    }

    public record StoreLine(long storeId, String storeName, double lineTotal) {
    }

    public record StoreGroup(long storeId, String storeName, double subtotal, int lines) {
    }

    /** Extra delivery fee for a cart spanning {@code storeCount} stores. */
    public static double extraFee(int storeCount) {
        if (storeCount <= 1) {
            return 0.0;
        }
        return EXTRA_FEE_PER_STORE * (storeCount - 1);
    }

    /** Groups priced lines by store, preserving first-seen order. */
    public static List<StoreGroup> groupByStore(List<StoreLine> lines) {
        Map<Long, double[]> sum = new LinkedHashMap<>();
        Map<Long, String> names = new LinkedHashMap<>();
        if (lines != null) {
            for (StoreLine l : lines) {
                names.putIfAbsent(l.storeId(), l.storeName());
                double[] s = sum.computeIfAbsent(l.storeId(), k -> new double[2]);
                s[0] += l.lineTotal();
                s[1] += 1;
            }
        }
        List<StoreGroup> out = new ArrayList<>();
        for (Map.Entry<Long, double[]> e : sum.entrySet()) {
            out.add(new StoreGroup(e.getKey(), names.get(e.getKey()), e.getValue()[0], (int) e.getValue()[1]));
        }
        return out;
    }
}
