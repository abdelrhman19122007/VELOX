package com.velox.order.service;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.velox.common.error.ApiException;
import com.velox.order.domain.OrderStatus;
import com.velox.order.repository.OrderRepository;

/**
 * Orders nobody moves on are closed instead of waiting forever (FR-ORD-10): a store that doesn't answer a new
 * order, or an accepted order no driver takes. Paid orders are refunded by the payments module (it hears the
 * CANCELLED change). Runs every minute.
 */
@Component
public class StaleOrderCloser {

    private final OrderRepository orders;
    private final OrderLifecycle lifecycle;
    private final Clock clock;
    private final Duration storeAnswer;
    private final Duration driverWait;

    public StaleOrderCloser(OrderRepository orders, OrderLifecycle lifecycle, Clock clock,
                            @Value("${velox.orders.store-answer-minutes:15}") long storeAnswerMinutes,
                            @Value("${velox.orders.driver-minutes:40}") long driverMinutes) {
        this.orders = orders;
        this.lifecycle = lifecycle;
        this.clock = clock;
        this.storeAnswer = Duration.ofMinutes(storeAnswerMinutes);
        this.driverWait = Duration.ofMinutes(driverMinutes);
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void closeStale() {
        close(OrderStatus.PLACED, storeAnswer, false, "The store didn't answer");
        close(OrderStatus.ACCEPTED, driverWait, true, "No driver was available");
        close(OrderStatus.READY, driverWait, true, "No driver was available");
    }

    private void close(OrderStatus status, Duration after, boolean withoutDriver, String reason) {
        for (long id : orders.stale(status, clock.instant().minus(after), withoutDriver)) {
            try {
                lifecycle.move(id, OrderStatus.CANCELLED, null, reason);
            } catch (ApiException e) {
                // moved on meanwhile
            }
        }
    }
}
