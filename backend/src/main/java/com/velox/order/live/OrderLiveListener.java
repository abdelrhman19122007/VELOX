package com.velox.order.live;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.velox.order.domain.OrderStatus;
import com.velox.order.repository.OrderRepository;
import com.velox.order.service.OrderViews;
import com.velox.order.spi.OrderChanged;

/**
 * Sends the fresh order to everyone following it (customer, store staff, driver) once the change is committed —
 * never before, so nobody sees a change that was rolled back. Unpaid orders stay hidden from the store.
 */
@Component
public class OrderLiveListener {

    private final OrderRepository orders;
    private final OrderViews views;
    private final EventHub hub;

    public OrderLiveListener(OrderRepository orders, OrderViews views, EventHub hub) {
        this.orders = orders;
        this.views = views;
        this.hub = hub;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onChange(OrderChanged change) {
        orders.findById(change.orderId()).ifPresent(order -> {
            var to = change.to() == OrderStatus.PENDING_PAYMENT
                    ? java.util.List.of(order.customerId())
                    : orders.participants(order.id());
            hub.sendTo(to, "order", views.of(order));
        });
    }
}
