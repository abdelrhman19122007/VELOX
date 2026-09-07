package com.app.payment;

public class CashOnDelivery implements PaymentMethod {
    private double balance;
    private boolean paid = false;

    public CashOnDelivery(double balance) {
        this.balance = balance;
    }
    @Override
    public void pay(double amount) {
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
