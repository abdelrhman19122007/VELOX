package com.velox.order.domain;

import java.math.BigDecimal;
import java.time.Instant;

/** An order as stored in {@code orders}. Money is BigDecimal — never double — so totals are exact. */
public record Order(long id, String code, OrderKind kind, long customerId, String storeId, Long driverId,
                    OrderStatus status, String zone, String phone, String address, String landmark, Double lat,
                    Double lng, String errandDetails, BigDecimal subtotal, BigDecimal discount, BigDecimal deliveryFee,
                    BigDecimal total, BigDecimal cashChangeFor, PaymentMethod paymentMethod, PaymentStatus paymentStatus,
                    String promo, String rejectReason, Instant scheduledFor, Instant createdAt, Instant updatedAt) {
}
