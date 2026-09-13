package com.app.discount;

public class SeasonalDiscount implements DiscountStrategy {
    @Override
    public double applyDiscount(double amount) {
        return amount * 0.85;
    }
}
