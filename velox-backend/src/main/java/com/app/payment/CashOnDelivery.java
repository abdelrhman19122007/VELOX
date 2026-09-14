package com.app.payment;

public class CashOnDelivery implements PaymentMethod {
    private double balance;
    private boolean paid = false;

    public CashOnDelivery(double balance) {
        if (balance < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        this.balance = balance;
    }
    @Override
    public void pay(double amount) {
        if (amount <= 0) {
            paid = false;
            System.out.println("\n Cash Payment Failed: Invalid amount!");
            return;
        }
        if (balance >= amount) {
            paid = true;
            System.out.println(">>> Cash Payment: " + amount + " EGP will be collected upon delivery.");
        } else {
            paid = false;
            System.out.println("\n Cash Payment Failed: Insufficient Budget!");
        }
    }

    @Override
    public String getPaymentType() { return "Cash on Delivery"; }

    @Override
    public boolean getPaymentStatus() { return paid; }
}
