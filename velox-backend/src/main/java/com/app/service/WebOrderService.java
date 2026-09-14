package com.app.service;

import com.app.dao.WebOrderDAO;
import com.app.enums.Governorate;
import com.app.enums.OrderStatus;
import com.app.enums.Size;
import com.app.model.order.Order;
import com.app.model.product.ClothingItem;
import com.app.model.product.ElectronicsItem;
import com.app.model.product.FoodItem;
import com.app.model.product.Product;
import com.app.util.OrderRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates storefront checkouts: transactional DB placement plus a
 * file-account copy so web orders appear in loyalty/offers/history too.
 */
public class WebOrderService {

    private final WebOrderDAO dao = new WebOrderDAO();
    private final com.app.dao.UserDAO users = new com.app.dao.UserDAO();

    public Map<String, Object> placeOrder(String email, List<Map<String, Object>> rawItems, String governorateRaw) {
        return placeOrder(email, rawItems, governorateRaw, null);
    }

    /**
     * @param paymentMethod optional "WALLET": debited from remaining_budget
     *                      (refunded automatically if placement fails).
     */
    public Map<String, Object> placeOrder(String email, List<Map<String, Object>> rawItems,
                                          String governorateRaw, String paymentMethod) {
        CustomerAccountService.Profile profile = CustomerAccountService.profileOf(email);
        if (profile == null) {
            throw new IllegalArgumentException("Account not found. Please register first.");
        }
        Governorate gov = resolveGovernorate(governorateRaw, profile.governorate);

        List<int[]> items = new ArrayList<>();
        if (rawItems == null || rawItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty.");
        }
        for (Map<String, Object> it : rawItems) {
            Object pid = it.get("product_id") != null ? it.get("product_id") : it.get("productId");
            Object qty = it.get("quantity") != null ? it.get("quantity") : it.get("qty");
            if (pid == null || qty == null) {
                throw new IllegalArgumentException("Each item needs product_id and quantity.");
            }
            items.add(new int[]{toInt(pid, "product_id"), toInt(qty, "quantity")});
        }

        int userId = dao.ensureUser(email, profile.name,
                CustomerAccountService.passwordHashOf(CustomerAccountService.keyForEmail(email)),
                profile.phone, gov.name());
        String shippingAddress = gov.name() + ", " + profile.phone;
        boolean wallet = paymentMethod != null && paymentMethod.trim().equalsIgnoreCase("WALLET");
        double walletCharged = 0;
        WebOrderDAO.Quote quote = null;
        if (wallet) {
            quote = dao.quote(items);
            walletCharged = quote.subtotal() + gov.getShippingPrice();
            if (!users.debit(userId, walletCharged)) {
                throw new IllegalArgumentException(
                        "Insufficient wallet balance (need " + String.format(java.util.Locale.US, "%.2f", walletCharged) + " EGP).");
            }
        }
        WebOrderDAO.PlacedOrder placed;
        try {
            placed = dao.placeOrder(userId, shippingAddress, gov.name(),
                    gov.getShippingPrice(), items);
        } catch (RuntimeException e) {
            if (wallet && walletCharged > 0) {
                users.topUp(userId, walletCharged); // refund on placement failure
            }
            throw e;
        }

        // File-account copy (PENDING: counts toward loyalty once delivered/paid).
        Order order = new Order(placed.orderCode(), email, gov.name(), profile.phone);
        order.setCustomerNameForDelivery(profile.name);
        for (WebOrderDAO.OrderLine l : placed.lines()) {
            Map<String, Object> v = dao.productView(l.productId());
            Product p = toProduct(v, l);
            if (p != null) {
                for (int i = 0; i < l.quantity(); i++) {
                    order.getProducts().add(p);
                }
            }
        }
        CustomerAccountService.recordCompletedOrder(order);

        // Global copy too: console return flow + OrderService history read this file.
        List<Order> all = OrderRepository.loadOrders();
        all.removeIf(o -> o.getOrderId().equalsIgnoreCase(order.getOrderId()));
        all.add(order);
        OrderRepository.saveOrders(all);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("orderId", placed.orderId());
        out.put("orderCode", placed.orderCode());
        out.put("subtotal", placed.subtotal());
        out.put("deliveryFee", placed.deliveryFee());
        out.put("total", placed.total());
        out.put("status", placed.status());
        out.put("paymentMethod", wallet ? "WALLET" : "CASH_ON_DELIVERY");
        if (wallet) {
            out.put("walletCharged", walletCharged);
        }
        return out;
    }

    /**
     * Rebuilds the file copies (per-user + global) for a DB order,
     * e.g. after cache cleanup. Returns false when the order is unknown.
     */
    @SuppressWarnings("unchecked")
    public boolean resyncFileCopy(String codeOrId) {
        Map<String, Object> data = dao.invoiceData(codeOrId);
        if (data == null) {
            return false;
        }
        String code = String.valueOf(data.getOrDefault("orderCode", codeOrId));
        String email = String.valueOf(data.getOrDefault("email", ""));
        String shipping = String.valueOf(data.getOrDefault("shipping", ""));
        String city = shipping.contains(",") ? shipping.split(",")[0].trim() : shipping;
        String phone = "";
        Order order = new Order(code, email, city.isEmpty() ? "CAIRO" : city, phone);
        order.setCustomerNameForDelivery(String.valueOf(data.getOrDefault("customer", "")));
        // Rebuild items from DB lines (name/qty/unit known; type/size defaulted).
        Object rawLines = data.get("lines");
        if (rawLines instanceof List<?> list2) {
            for (Object o : list2) {
                if (o instanceof Map<?, ?> line) {
                    int qty = 1;
                    try {
                        qty = Integer.parseInt(String.valueOf(line.get("quantity")).trim());
                    } catch (Exception ignored) {
                    }
                    double unit = toDouble(line.get("unit"), 0);
                    String nm = String.valueOf(line.get("name"));
                    com.app.model.product.Product p =
                            new com.app.model.product.FoodItem("0", nm, unit, "", com.app.enums.Size.MEDIUM);
                    for (int i = 0; i < qty; i++) {
                        order.getProducts().add(p);
                    }
                }
            }
        }
        try {
            OrderStatus st = OrderStatus.valueOf(String.valueOf(data.getOrDefault("status", "PENDING")));
            order.setStatus(st);
            if (st == OrderStatus.RETURNED) {
                order.setReturned(true);
            }
        } catch (IllegalArgumentException ignored) {
        }
        CustomerAccountService.recordCompletedOrder(order);
        List<Order> all = OrderRepository.loadOrders();
        all.removeIf(x -> x.getOrderId().equalsIgnoreCase(order.getOrderId()));
        all.add(order);
        OrderRepository.saveOrders(all);
        return true;
    }

    private static double toDouble(Object v, double fallback) {
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(v).trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Governorate resolveGovernorate(String raw, String profileGov) {
        if (raw != null && !raw.isBlank()) {
            try {
                return Governorate.fromCodeOrName(raw.trim());
            } catch (IllegalArgumentException e) {
                return Governorate.fromNameLenient(raw.trim());
            }
        }
        if (profileGov != null && !profileGov.isBlank()) {
            try {
                return Governorate.fromName(profileGov);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Governorate.CAIRO;
    }

    private static int toInt(Object v, String field) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + field + ": " + v + ".");
        }
    }

    private static Product toProduct(Map<String, Object> v, WebOrderDAO.OrderLine l) {
        if (v == null || v.isEmpty()) {
            return null;
        }
        String id = String.valueOf(l.productId());
        String name = String.valueOf(v.getOrDefault("name", "Item"));
        String desc = String.valueOf(v.getOrDefault("description", ""));
        Size size;
        try {
            size = Size.valueOf(String.valueOf(v.getOrDefault("size", "MEDIUM")).trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            size = Size.MEDIUM;
        }
        String type = String.valueOf(v.getOrDefault("product_type", "FOOD")).trim().toUpperCase();
        return switch (type) {
            case "CLOTHES" -> new ClothingItem(id, name, l.unitPrice(), desc, size);
            case "TECH" -> new ElectronicsItem(id, name, l.unitPrice(), desc, size);
            default -> new FoodItem(id, name, l.unitPrice(), desc, size);
        };
    }
}
