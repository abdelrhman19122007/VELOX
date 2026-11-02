package com.velox.order.domain;

import java.math.BigDecimal;

/** A receipt line. Name and price are copied from the menu at order time, so receipts never change later. */
public record OrderItem(String productId, String name, String nameAr, BigDecimal unitPrice, int qty, String note) {

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(qty));
    }
}
