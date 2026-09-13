package com.app.service;

import com.app.model.order.Order;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * Loyalty program: every {@value #ORDERS_PER_REWARD} successful orders
 * earns the customer one FREE delivery on a future order.
 * Progress is derived live from the account's orders (returns excluded
 * automatically), while consumed rewards are persisted in
 * {@code loyalty.properties} so a reward can't be reused.
 */
public final class LoyaltyService {

    public static final int ORDERS_PER_REWARD = 5;

    private LoyaltyService() {
    }

    public static int successfulCount(String userKey) {
        List<Order> orders = CustomerAccountService.loadUserOrders(userKey);
        return CustomerAccountService.countSuccessful(orders);
    }

    public static int rewardsEarned(int successful) {
        return successful / ORDERS_PER_REWARD;
    }

    public static int rewardsConsumed(String userKey) {
        Path f = CustomerAccountService.userDir(userKey).resolve("loyalty.properties");
        if (!Files.exists(f)) {
            return 0;
        }
        try (var in = Files.newInputStream(f)) {
            Properties p = new Properties();
            p.load(in);
            return Integer.parseInt(p.getProperty("consumed", "0").trim());
        } catch (IOException | NumberFormatException e) {
            return 0;
        }
    }

    public static boolean isRewardAvailable(String userKey) {
        return rewardsEarned(successfulCount(userKey)) > rewardsConsumed(userKey);
    }

    /** Progress inside the current 5-order cycle, e.g. 3/5. */
    public static int progressInCycle(String userKey) {
        return successfulCount(userKey) % ORDERS_PER_REWARD;
    }

    public static void consumeReward(String userKey) {
        int consumed = rewardsConsumed(userKey) + 1;
        try {
            Files.createDirectories(CustomerAccountService.userDir(userKey));
            Properties p = new Properties();
            p.setProperty("consumed", String.valueOf(consumed));
            try (var out = Files.newOutputStream(
                    CustomerAccountService.userDir(userKey).resolve("loyalty.properties"))) {
                p.store(out, "VELOX loyalty rewards consumed");
            }
        } catch (IOException e) {
            System.err.println("[Loyalty] consume failed: " + e.getMessage());
        }
    }

    public static String progressMessage(String userKey) {
        int ok = successfulCount(userKey);
        if (isRewardAvailable(userKey)) {
            return "Loyalty: you earned FREE delivery! (" + ok + " successful orders)";
        }
        int need = ORDERS_PER_REWARD - (ok % ORDERS_PER_REWARD);
        return "Loyalty: " + (ok % ORDERS_PER_REWARD) + "/" + ORDERS_PER_REWARD
                + " - " + need + " more successful order(s) for FREE delivery (total: " + ok + ")";
    }
}
