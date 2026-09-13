package com.app.payment;

public interface PaymentMethod {
    void pay(double amount);
    String getPaymentType();
    boolean getPaymentStatus();
}
