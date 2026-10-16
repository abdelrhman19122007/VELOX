package com.velox.payment.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sandbox only: the Vodafone Cash confirmation code kept in memory so the app can show it in place of the SMS.
 * The database only ever holds its hash. With a real gateway nothing is remembered here.
 */
final class SandboxCodes {

    private static final Map<Long, String> CODES = new ConcurrentHashMap<>();

    private SandboxCodes() {
    }

    static void remember(long orderId, String code) {
        if (code != null) {
            CODES.put(orderId, code);
        }
    }

    static String peek(long orderId) {
        return CODES.get(orderId);
    }

    static void forget(long orderId) {
        CODES.remove(orderId);
    }
}
