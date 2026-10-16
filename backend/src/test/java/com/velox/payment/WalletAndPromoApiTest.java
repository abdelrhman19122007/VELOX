package com.velox.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.velox.order.domain.OrderStatus;
import com.velox.order.spi.OrderChanged;
import com.velox.payment.service.WalletService;

/** Refunds to the wallet, paying from it, loyalty points and promo codes. */
class WalletAndPromoApiTest extends PaymentApiTestBase {

    @Autowired
    WalletService walletService;

    @Test
    void cancellingAPaidOrderRefundsTheWallet() {
        String[] store = storeWithProduct(100);
        String customer = newCustomer(uniqueEmail(), "secret-123");
        String code = place(customer, order(store[1], "FAWRY")).get("code").asText();
        post("/api/orders/" + code + "/payment/sandbox-pay", null, customer);

        assertEquals(HttpStatus.OK, post("/api/orders/" + code + "/cancel", null, customer).getStatusCode());
        assertEquals("REFUNDED", orderOf(customer, code).get("paymentStatus").asText());
        JsonNode wallet = get("/api/me/wallet", customer).getBody();
        assertEquals(115.0, wallet.get("balance").asDouble());
        assertEquals("REFUND", wallet.get("history").get(0).get("reason").asText());
    }

    @Test
    void walletPaysOnlyWhatItHas() {
        String[] store = storeWithProduct(100);
        String customer = newCustomer(uniqueEmail(), "secret-123");
        assertEquals("INSUFFICIENT_BALANCE", place(customer, order(store[1], "WALLET")).get("code").asText());

        assertEquals(200.0, post("/api/me/wallet/sandbox-top-up", Map.of("amount", 200), customer).getBody()
                .get("balance").asDouble());
        JsonNode o = place(customer, order(store[1], "WALLET"));
        assertEquals("PLACED", o.get("status").asText(), "paid at once");
        assertEquals("PAID", o.get("paymentStatus").asText());
        assertEquals(85.0, get("/api/me/wallet", customer).getBody().get("balance").asDouble());
    }

    @Test
    void topUpLimitsAndRoles() {
        String customer = newCustomer(uniqueEmail(), "secret-123");
        assertEquals(HttpStatus.BAD_REQUEST, post("/api/me/wallet/sandbox-top-up", Map.of("amount", 5000), customer).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, post("/api/me/wallet/sandbox-top-up", Map.of("amount", -50), customer).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, get("/api/me/wallet", login(ADMIN_EMAIL, ADMIN_PASSWORD)).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, get("/api/me/wallet", null).getStatusCode());
    }

    @Test
    void deliveredOrdersEarnPointsOnce() {
        String[] store = storeWithProduct(100);
        String email = uniqueEmail();
        String customer = newCustomer(email, "secret-123");
        String code = place(customer, order(store[1], "CASH")).get("code").asText();
        OrderChanged delivered = new OrderChanged(idOf(code), OrderStatus.PICKED_UP, OrderStatus.DELIVERED);
        walletService.onDelivered(delivered);
        walletService.onDelivered(delivered);
        assertEquals(11, get("/api/me/wallet", customer).getBody().get("points").asInt(), "115 EGP → 11 points, once");
    }

    @Test
    void percentPromoOncePerCustomer() {
        String[] store = storeWithProduct(100);
        String customer = newCustomer(uniqueEmail(), "secret-123");
        Map<String, Object> o = order(store[1], "CASH");
        o.put("promo", "VELOX10");
        JsonNode placed = place(customer, o);
        assertEquals(10.0, placed.get("discount").asDouble());
        assertEquals(105.0, placed.get("total").asDouble());
        assertEquals("INVALID_PROMO", place(customer, o).get("code").asText(), "one use per customer");
    }

    @Test
    void freeDeliveryAndMinimumOrder() {
        String customer = newCustomer(uniqueEmail(), "secret-123");
        Map<String, Object> big = order(storeWithProduct(100)[1], "CASH");
        big.put("promo", "FREESHIP");
        assertEquals(100.0, place(customer, big).get("total").asDouble(), "delivery is free");

        Map<String, Object> small = order(storeWithProduct(20)[1], "CASH");
        small.put("promo", "WELCOME");
        assertEquals("INVALID_PROMO", place(customer, small).get("code").asText(), "under the 60 EGP minimum");
        small.put("promo", "NO_SUCH_CODE");
        assertEquals("INVALID_PROMO", place(customer, small).get("code").asText(), "same answer — codes can't be guessed");
    }

    @Test
    void adminPromoWithUseLimit() {
        String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String code = "ONCE" + System.nanoTime() % 100000;
        Map<String, Object> promo = Map.of("code", code, "kind", "FIXED", "value", 30, "maxUses", 1);
        String customer = newCustomer(uniqueEmail(), "secret-123");
        assertEquals(HttpStatus.FORBIDDEN, post("/api/admin/promos", promo, customer).getStatusCode());
        assertEquals(HttpStatus.CREATED, post("/api/admin/promos", promo, admin).getStatusCode());
        assertEquals(HttpStatus.CONFLICT, post("/api/admin/promos", promo, admin).getStatusCode(), "codes are unique");
        assertEquals("INVALID_PROMO", post("/api/admin/promos", Map.of("code", "HALFPLUS", "kind", "PERCENT", "value", 150), admin)
                .getBody().get("code").asText());

        String product = storeWithProduct(100)[1];
        Map<String, Object> o = order(product, "CASH");
        o.put("promo", code);
        assertEquals(new BigDecimal("85.00"), place(customer, o).get("total").decimalValue().setScale(2));
        String second = newCustomer(uniqueEmail(), "secret-123");
        assertEquals("INVALID_PROMO", place(second, o).get("code").asText(), "max uses reached");
    }
}
