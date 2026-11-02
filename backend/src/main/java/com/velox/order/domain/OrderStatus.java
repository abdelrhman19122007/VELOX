package com.velox.order.domain;

import java.util.Map;
import java.util.Set;

import com.velox.security.Role;

/**
 * The order lifecycle as a state machine (State pattern): each state lists the states it may move to and which
 * kind of account may make that move. No other code decides transitions — see {@code OrderLifecycle}.
 * {@code null} actor = the system (expiry, timeouts, payment confirmation).
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PLACED,
    ACCEPTED,
    READY,
    PICKED_UP,
    DELIVERED,
    REJECTED,
    CANCELLED,
    RETURNED;

    /** Who may make each move: target state → roles (SYSTEM = scheduled jobs / payment callbacks). */
    private static final Map<OrderStatus, Map<OrderStatus, Set<Actor>>> RULES = Map.of(
            PENDING_PAYMENT, Map.of(
                    PLACED, Set.of(Actor.SYSTEM),
                    CANCELLED, Set.of(Actor.CUSTOMER, Actor.SYSTEM, Actor.ADMIN)),
            PLACED, Map.of(
                    ACCEPTED, Set.of(Actor.RESTAURANT, Actor.SYSTEM),   // SYSTEM: an errand taken by a driver
                    REJECTED, Set.of(Actor.RESTAURANT),
                    CANCELLED, Set.of(Actor.CUSTOMER, Actor.SYSTEM, Actor.ADMIN)),
            ACCEPTED, Map.of(
                    READY, Set.of(Actor.RESTAURANT, Actor.SYSTEM),      // SYSTEM: the errand driver has bought it
                    CANCELLED, Set.of(Actor.SYSTEM, Actor.ADMIN)),
            READY, Map.of(
                    PICKED_UP, Set.of(Actor.DRIVER),
                    CANCELLED, Set.of(Actor.SYSTEM, Actor.ADMIN)),
            PICKED_UP, Map.of(
                    DELIVERED, Set.of(Actor.DRIVER),
                    CANCELLED, Set.of(Actor.ADMIN)),
            DELIVERED, Map.of(
                    RETURNED, Set.of(Actor.CUSTOMER)));

    public enum Actor {
        CUSTOMER, RESTAURANT, DRIVER, ADMIN, SYSTEM;

        public static Actor of(Role role) {
            return role == null ? SYSTEM : Actor.valueOf(role.name());
        }
    }

    public boolean canMoveTo(OrderStatus next, Actor actor) {
        return RULES.getOrDefault(this, Map.of()).getOrDefault(next, Set.of()).contains(actor);
    }

    /** Still being worked on (not finished, failed or waiting for payment). */
    public boolean isActive() {
        return this == PLACED || this == ACCEPTED || this == READY || this == PICKED_UP;
    }

    public boolean isFinal() {
        return !RULES.containsKey(this);
    }
}
