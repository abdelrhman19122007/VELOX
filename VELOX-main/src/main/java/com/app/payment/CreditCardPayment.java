package com.app.payment;

public class CreditCardPayment implements PaymentMethod {
    private final String cardNumber;
    private boolean paid;
    private double balance;
    public CreditCardPayment(String cardNumber ,double balance) {
        this.cardNumber = cardNumber;
        this.balance=balance;
    }

    @Override
    public void pay(double amount) {
        if(balance>= amount){
        paid = true;
        balance-=amount;
        String maskedCard = cardNumber.length() >= 4
                ? "**** " + cardNumber.substring(cardNumber.length() - 4)
                : "****";
        System.out.println(">>> Paid " + amount + " EGP via Credit Card (" + maskedCard + ")");
         }else{paid = false;
         System.out.println("\n Payment Failed: Insufficient Credit Card Limit/Budget!");
        }}

    @Override
    public String getPaymentType() { return "Credit Card"; }

    @Override
    public boolean getPaymentStatus() { return paid; }
}
