package com.velox.order.domain;

/** COD = cash on delivery; the others follow an online payment. */
public enum PaymentStatus {
    COD, PENDING, PAID, REFUNDED, EXPIRED
}
