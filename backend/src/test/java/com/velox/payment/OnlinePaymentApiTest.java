package com.velox.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.velox.payment.service.RefundListener;

/** Vodafone Cash and Fawry from order to paid, plus the attacks on them (replay, brute force, IDOR, expiry). */
class OnlinePaymentApiTest extends PaymentApiTestBase {

    @Autowired
    RefundListener refunds;

    @Test
    void vodafoneCashIsPaidWithTheSmsCode() {
        String[] store = storeWithProduct(100);
        String customer = newCustomer(uniqueEmail(), "secret-123");
        JsonNode o = place(customer, order(store[1], "VODAFONE_CASH"));
        assertEquals("PENDING_PAYMENT", o.get("status").asText());
        assertEquals("PENDING", o.get("paymentStatus").asText());

        JsonNode pay = payment(customer, o.get("code").asText());
        assertEquals(115.0, pay.get("amount").asDouble(), "100 + 15 delivery");
        assertEquals(VF_PHONE, pay.get("walletPhone").asText());
        String sms = pay.get("sandboxCode").asText();
        assertTrue(sms.matches("\\d{6}"));

        String wrong = sms.equals("000000") ? "111111" : "000000";
        ResponseEntity<JsonNode> bad = post("/api/orders/" + o.get("code").asText() + "/payment/confirm", Map.of("code", wrong), customer);
        assertEquals("WRONG_CODE", bad.getBody().get("code").asText());

        assertEquals(HttpStatus.NO_CONTENT,
                post("/api/orders/" + o.get("code").asText() + "/payment/confirm", Map.of("code", sms), customer).getStatusCode());
        JsonNode after = orderOf(customer, o.get("code").asText());
        assertEquals("PLACED", after.get("status").asText());
        assertEquals("PAID", after.get("paymentStatus").asText());
        assertFalse(payment(customer, o.get("code").asText()).has("sandboxCode"), "no code once paid");

        ResponseEntity<JsonNode> replay = post("/api/orders/" + o.get("code").asText() + "/payment/confirm", Map.of("code", sms), customer);
        assertEquals("PAYMENT_CLOSED", replay.getBody().get("code").asText(), "a payment can't be confirmed twice");
    }

    @Test
    void vodafoneCashNeedsAWalletNumber() {
        String[] store = storeWithProduct(50);
        String customer = newCustomer(uniqueEmail(), "secret-123");
        Map<String, Object> o = order(store[1], "VODAFONE_CASH");
        o.remove("walletPhone");
        assertEquals("INVALID_WALLET", place(customer, o).get("code").asText());
        o.put("walletPhone", "01112345678");
        assertEquals(HttpStatus.BAD_REQUEST, post("/api/orders", o, customer).getStatusCode(), "Vodafone numbers only");
    }

    @Test
    void fiveWrongCodesClosesThePayment() {
        String[] store = storeWithProduct(50);
        String customer = newCustomer(uniqueEmail(), "secret-123");
        String code = place(customer, order(store[1], "VODAFONE_CASH")).get("code").asText();
        String sms = payment(customer, code).get("sandboxCode").asText();
        String wrong = sms.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            assertEquals("WRONG_CODE", post("/api/orders/" + code + "/payment/confirm", Map.of("code", wrong), customer)
                    .getBody().get("code").asText());
        }
        assertEquals("TOO_MANY_ATTEMPTS", post("/api/orders/" + code + "/payment/confirm", Map.of("code", sms), customer)
                .getBody().get("code").asText(), "even the right code is refused after 5 tries");
        assertEquals(HttpStatus.BAD_REQUEST,
                post("/api/orders/" + code + "/payment/confirm", Map.of("code", "12ab"), customer).getStatusCode());
    }

    @Test
    void fawryReferenceIsPaidOnce() {
        String[] store = storeWithProduct(40);
        String customer = newCustomer(uniqueEmail(), "secret-123");
        String code = place(customer, order(store[1], "FAWRY")).get("code").asText();
        JsonNode pay = payment(customer, code);
        assertTrue(pay.get("reference").asText().matches("\\d{9}"));
        assertEquals("INVALID_REQUEST", post("/api/orders/" + code + "/payment/confirm", Map.of("code", "123456"), customer)
                .getBody().get("code").asText(), "Fawry has no SMS code");

        assertEquals(HttpStatus.NO_CONTENT, post("/api/orders/" + code + "/payment/sandbox-pay", null, customer).getStatusCode());
        assertEquals("PLACED", orderOf(customer, code).get("status").asText());
        assertEquals("PAYMENT_CLOSED", post("/api/orders/" + code + "/payment/sandbox-pay", null, customer)
                .getBody().get("code").asText());
    }

    @Test
    void onlyTheOwnerSeesOrPaysAnOrder() {
        String[] store = storeWithProduct(40);
        String owner = newCustomer(uniqueEmail(), "secret-123");
        String other = newCustomer(uniqueEmail(), "secret-123");
        String code = place(owner, order(store[1], "VODAFONE_CASH")).get("code").asText();

        ResponseEntity<JsonNode> peek = get("/api/orders/" + code + "/payment", other);
        assertEquals(HttpStatus.NOT_FOUND, peek.getStatusCode());
        assertEquals("ORDER_NOT_FOUND", peek.getBody().get("code").asText(), "same answer as a code that doesn't exist");
        assertEquals(HttpStatus.NOT_FOUND, post("/api/orders/" + code + "/payment/sandbox-pay", null, other).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, get("/api/orders/" + code + "/payment", null).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, get("/api/orders/" + code + "/payment", login(store[2], "store-pass-123")).getStatusCode());
    }

    @Test
    void cashOrdersHaveNoOnlinePayment() {
        String[] store = storeWithProduct(40);
        String customer = newCustomer(uniqueEmail(), "secret-123");
        String code = place(customer, order(store[1], "CASH")).get("code").asText();
        assertEquals("NO_PAYMENT", get("/api/orders/" + code + "/payment", customer).getBody().get("code").asText());
    }

    @Test
    void unpaidOrdersAreCancelledWhenTimeRunsOut() {
        String[] store = storeWithProduct(40);
        String customer = newCustomer(uniqueEmail(), "secret-123");
        String code = place(customer, order(store[1], "FAWRY")).get("code").asText();
        db.update("UPDATE payments SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 minute' WHERE order_id = ?", idOf(code));

        assertEquals("PAYMENT_EXPIRED", post("/api/orders/" + code + "/payment/sandbox-pay", null, customer)
                .getBody().get("code").asText());
        refunds.expireUnpaid();
        JsonNode o = orderOf(customer, code);
        assertEquals("CANCELLED", o.get("status").asText());
        assertEquals("EXPIRED", o.get("paymentStatus").asText());
        assertEquals("EXPIRED", payment(customer, code).get("status").asText());
    }

    @Test
    void cancellingAnUnpaidOrderClosesItsPayment() {
        String[] store = storeWithProduct(40);
        String customer = newCustomer(uniqueEmail(), "secret-123");
        String code = place(customer, order(store[1], "FAWRY")).get("code").asText();
        assertEquals(HttpStatus.OK, post("/api/orders/" + code + "/cancel", null, customer).getStatusCode());
        assertEquals("EXPIRED", orderOf(customer, code).get("paymentStatus").asText());
        assertEquals("PAYMENT_CLOSED", post("/api/orders/" + code + "/payment/sandbox-pay", null, customer)
                .getBody().get("code").asText());
        assertEquals(0.0, get("/api/me/wallet", customer).getBody().get("balance").asDouble(), "nothing was paid, nothing refunded");
    }
}
