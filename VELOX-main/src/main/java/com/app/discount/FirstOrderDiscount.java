package com.app.discount;

public class FirstOrderDiscount implements DiscountStrategy {
    @Override
    public double applyDiscount(double amount) {
        return amount * 0.65;
    }
}
