
package com.app.payment;

// فئة تمثل طريقة الدفع بالبطاقة الائتمانية، وتلتزم بتطبيق دوال واجهة PaymentMethod
public class CreditCardPayment implements PaymentMethod {
    
    // رقم البطاقة الائتمانية (مُعرف كـ final لضمان عدم تعديله بعد إنشاء الكائن)
    private final String cardNumber;
    // مؤشر لتتبع حالة الدفع (تم بنجاح أم لا)
    private boolean paid;
    // متغير لتخزين الرصيد المتاح في البطاقة
    private double balance;

    // المُنشئ (Constructor): لتهيئة الكائن برقم البطاقة والرصيد المبدئي
    public CreditCardPayment(String cardNumber, double balance) {
        if (!isValidCardNumber(cardNumber)) {
            throw new IllegalArgumentException("Invalid credit card number: must be 16 digits and pass Luhn check");
        }
        if (balance < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        this.cardNumber = cardNumber.replaceAll("\\s|-", "");
        this.balance = balance;
    }

    public static boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return false;
        }
        String digits = cardNumber.replaceAll("\\s|-", "");
        if (!digits.matches("\\d{16}")) {
            return false;
        }
        // Luhn check
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    // تطبيق دالة الدفع لمعالجة المعاملة وخصم الرصيد
    @Override
    public void pay(double amount) {
        // التحقق مما إذا كان رصيد البطاقة يغطي المبلغ المطلوب
        if(balance >= amount) {
            // تحديث حالة الدفع إلى "تم بنجاح"
            paid = true;
            // خصم المبلغ المطلوب من رصيد البطاقة
            balance -= amount;
            
            // إخفاء رقم البطاقة (Masking) وعرض آخر 4 أرقام فقط لأسباب أمنية
            String maskedCard = cardNumber.length() >= 4
                ? "**** " + cardNumber.substring(cardNumber.length() - 4)
                : "****";
                
            // طباعة رسالة تأكيد الدفع مع عرض المبلغ ورقم البطاقة المخفي
            System.out.println(">>> Paid " + amount + " EGP via Credit Card (" + maskedCard + ")");
        } else {
            // تحديث حالة الدفع إلى "فشل"
            paid = false;
            // طباعة رسالة تنبيه لعدم كفاية رصيد البطاقة
            System.out.println("\n Payment Failed: Insufficient Credit Card Limit/Budget!");
        }
    }

    // دالة تُرجع نوع طريقة الدفع المُستخدمة
    @Override
    public String getPaymentType() { 
        return "Credit Card"; 
    }

    // دالة تُرجع حالة الدفع الحالية
    @Override
    public boolean getPaymentStatus() { 
        return paid; 
    }
}
