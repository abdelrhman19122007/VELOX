package com.app.payment;

// فئة تمثل طريقة الدفع عبر المحفظة الإلكترونية، وتلتزم بتطبيق دوال واجهة PaymentMethod
public class WalletPayment implements PaymentMethod {
    
    // متغير لتخزين رقم المحفظة الإلكترونية
    private String walletNumber;
    
    // متغير لتخزين الرصيد المتاح داخل المحفظة
    private double balance;
    
    // مؤشر لتتبع حالة الدفع، وقيمته الافتراضية false (لم تتم عملية الدفع بعد)
    private boolean isPaid = false;

    // المُنشئ (Constructor): لتهيئة كائن المحفظة برقمها والرصيد المبدئي
    public WalletPayment(String walletNumber, double balance) {
        this.balance = balance;
        this.walletNumber = walletNumber;
    }

    // دالة إضافية للحصول على الرصيد الحالي للمحفظة
    public double getBalance() {
        return balance;
    }

    // تطبيق دالة الدفع لمعالجة المعاملة المالية عبر المحفظة وخصم المبلغ
    @Override
    public void pay(double amount) {
        // التحقق مما إذا كان رصيد المحفظة يغطي المبلغ المطلوب دفعه
        if (balance >= amount) {
            // خصم المبلغ المطلوب من رصيد المحفظة
            balance -= amount;
            
            // تحديث حالة الدفع إلى "تم بنجاح"
            isPaid = true;
            
            // طباعة رسالة نجاح العملية مع تفاصيل المبلغ المدفوع بالجنيه المصري والرصيد المتبقي
            System.out.println("\n Payment Successful via Wallet!");
            System.out.printf(java.util.Locale.US,"--> Paid: %.2f EGP\n", amount);
            System.out.printf(java.util.Locale.US,"--> Remaining Balance: %.2f EGP\n", balance);
        } else {
            // تأكيد فشل عملية الدفع لعدم كفاية الرصيد
            isPaid = false;
            
            // طباعة رسالة تنبيه توضح المبلغ المطلوب مقارنة بالرصيد المتوفر في المحفظة
            System.out.println("\n Payment Failed: Insufficient Wallet Balance!");
            System.out.printf(java.util.Locale.US,"--> Required: %.2f EGP | Your Balance: %.2f EGP\n", amount, balance);
        }
    }

    // دالة تُرجع نوع وسيلة الدفع كنص
    @Override
    public String getPaymentType() {
        return "Wallet";
    }

    // دالة تُرجع حالة عملية الدفع الحالية
    @Override
    public boolean getPaymentStatus() {
        return isPaid;
    }
}
