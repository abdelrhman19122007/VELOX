package com.app.payment;

// واجهة أساسية (Interface) تحدد القواعد والدوال المشتركة لكل طرق الدفع في النظام
public interface PaymentMethod {
    
    // دالة لتنفيذ عملية الدفع بمبلغ محدد ويجب تنفيذها في كل فئة فرعية
    void pay(double amount);
    
    // دالة تُرجع نوع وسيلة الدفع كنص (مثل Cash on Delivery أو Credit Card)
    String getPaymentType();
    
    // دالة تُرجع حالة عملية الدفع (هل تمت بنجاح أم فشلت)
    boolean getPaymentStatus();
}
