package com.app.payment;

// فئة تمثل طريقة الدفع بالبطاقة الائتمانية، وتلتزم بتطبيق دوال واجهة PaymentMethod
public class CreditCardPayment implements PaymentMethod {
    
    // رقم البطاقة الائتمانية (مُعرف كـ final لضمان عدم تعديله بعد إنشاء الكائن)
    private final String cardNumber;
    
    // مؤشر لتتبع حالة الدفع (هل تمت العملية بنجاح أم لا)
    private boolean paid;
    
    // متغير لتخزين الرصيد المتاح في البطاقة
    private double balance;

    // المُنشئ (Constructor): لتهيئة الكائن برقم البطاقة والرصيد المبدئي
    public CreditCardPayment(String cardNumber ,double balance) {
        this.cardNumber = cardNumber;
        this.balance = balance;
    }

    // تطبيق دالة الدفع لمعالجة المعاملة المالية وخصم الرصيد
    @Override
    public void pay(double amount) {
        // التحقق مما إذا كان رصيد البطاقة يغطي المبلغ المطلوب دفعه
        if(balance >= amount){
            // تحديث حالة الدفع إلى "تم بنجاح"
            paid = true;
            
            // خصم المبلغ المطلوب فعلياً من رصيد البطاقة
            balance -= amount;
            
            // إخفاء رقم البطاقة (Masking) وعرض آخر 4 أرقام فقط لأسباب أمنية
            String maskedCard = cardNumber.length() >= 4 
                ? "**** " + cardNumber.substring(cardNumber.length() - 4) 
                : "****";
                
            // طباعة رسالة تأكيد الدفع مع عرض المبلغ ورقم البطاقة المخفي
            System.out.println(">>> Paid " + amount + " EGP via Credit Card (" + maskedCard + ")");
        } else {
            // تحديث حالة الدفع إلى "فشل" لعدم كفاية الرصيد
            paid = false;
            
            // طباعة رسالة تنبيه تفيد بفشل عملية الدفع
            System.out.println("\n Payment Failed: Insufficient Credit Card Limit/Budget!");
        }
    }

    // دالة تُرجع نوع طريقة الدفع المُستخدمة كنص
    @Override
    public String getPaymentType() { 
        return "Credit Card"; 
    }

    // دالة تُرجع حالة عملية الدفع الحالية
    @Override
    public boolean getPaymentStatus() { 
        return paid; 
    }
}
