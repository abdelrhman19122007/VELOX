package com.app.payment;

public class WalletPayment implements PaymentMethod {
    private String walletNumber;
    private double balance;
    private boolean isPaid = false;

    public WalletPayment(String walletNumber,double balance) {
        this.balance = balance;
     this.walletNumber = walletNumber;
    }

    public double getBalance() {
        return balance;
    }

    @Override
    public void pay(double amount) {
        if (balance >= amount) {
            balance -= amount;
            isPaid = true;
            System.out.println("\n Payment Successful via Wallet!");
            System.out.printf(java.util.Locale.US,"--> Paid: %.2f EGP\n", amount);
            System.out.printf(java.util.Locale.US,"--> Remaining Balance: %.2f EGP\n", balance);
        } else {
            isPaid = false;
            System.out.println("\n Payment Failed: Insufficient Wallet Balance!");
            System.out.printf(java.util.Locale.US,"--> Required: %.2f EGP | Your Balance: %.2f EGP\n", amount, balance);
        }
    }

    @Override
    public String getPaymentType() {
        return "Wallet";
    }

    @Override
    public boolean getPaymentStatus() {
        return isPaid;
    }
}