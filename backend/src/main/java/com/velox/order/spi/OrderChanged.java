package com.velox.order.spi;

import com.velox.order.domain.OrderStatus;

/**
 * Published after an order is created or changes status (Observer). Live updates, push notifications, loyalty
 * points and refunds listen to it — the orders module doesn't need to know about any of them.
 * {@code from} is null for a new order.
 */
public record OrderChanged(long orderId, OrderStatus from, OrderStatus to) {
}
