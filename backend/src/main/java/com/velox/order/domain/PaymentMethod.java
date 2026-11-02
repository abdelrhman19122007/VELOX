package com.velox.order.domain;

/** How the customer pays. Online methods are handled by the payments module (see {@code PaymentStarter}). */
public enum PaymentMethod {
    CASH, VODAFONE_CASH, FAWRY, WALLET;

    public boolean isOnline() {
        return this != CASH;
    }
}
