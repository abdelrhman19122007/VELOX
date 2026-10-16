package com.velox.operation.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.velox.notification.PushService;
import com.velox.operation.repository.DriverRepository;
import com.velox.order.domain.Order;
import com.velox.order.domain.OrderKind;
import com.velox.order.domain.OrderStatus;
import com.velox.order.live.EventHub;
import com.velox.order.repository.OrderRepository;
import com.velox.order.service.OrderViews;
import com.velox.order.spi.OrderChanged;

/**
 * Tells online drivers about new work the moment it exists (live + push): a store order the store accepted, or a
 * new errand. When a driver takes it, the others are told it's gone.
 */
@Component
public class OfferListener {

    private final OrderRepository orders;
    private final OrderViews views;
    private final DriverRepository drivers;
    private final EventHub hub;
    private final PushService push;

    public OfferListener(OrderRepository orders, OrderViews views, DriverRepository drivers, EventHub hub, PushService push) {
        this.orders = orders;
        this.views = views;
        this.drivers = drivers;
        this.hub = hub;
        this.push = push;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onChange(OrderChanged change) {
        Order o = orders.findById(change.orderId()).orElse(null);
        if (o == null || o.driverId() != null) {
            return;
        }
        boolean newWork = (o.kind() == OrderKind.STORE && change.to() == OrderStatus.ACCEPTED)
                || (o.kind() == OrderKind.ERRAND && change.to() == OrderStatus.PLACED);
        if (!newWork) {
            return;
        }
        List<Long> online = drivers.online();
        hub.sendTo(online, "offer", views.of(o));
        push.send(online, new PushService.Message("طلب جديد للتوصيل · " + o.code(), "التوصيل " + o.deliveryFee() + " ج.م — " + o.zone(),
                "New delivery · " + o.code(), "Fee EGP " + o.deliveryFee() + " — " + o.zone(), o.code()));
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onTaken(DriverService.OfferTaken taken) {
        hub.sendTo(drivers.online(), "offer-taken", Map.of("code", taken.orderCode()));
    }
}
