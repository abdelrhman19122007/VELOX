package com.app.util;

import com.app.model.order.Order;
import com.app.model.product.Product;
import com.app.service.Delivery;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * فئة مسؤولة عن توليد وإنشاء الإيصالات (Receipts) الخاصة بالطلبات.
 * تقوم بتنسيق تفاصيل المنتجات، الأسعار، رسوم التوصيل، التغليف، وسياسة الإرجاع في شكل نصي منظم.
 */
public class ReceiptGenerator {

    /**
     * الدالة الأساسية لتوليد نص الإيصال بالكامل بناءً على بيانات الطلب والرسوم.
     * 
     * @param order كائن الطلب الذي يحتوي على المنتجات والبيانات
     * @param deliveryFee رسوم التوصيل
     * @param packagingFee رسوم التغليف
     * @param packagingType نوع التغليف المستخدم
     * @return نص منسق يمثل الإيصال الرسمي للعميل
     */
    public static String generateReceipt(Order order, double deliveryFee , double packagingFee, String packagingType) {
            double rawTotal = order.calculateRawTotal();
            double finalTotal = order.calculateFinalTotal();
            double discountAmount = rawTotal - finalTotal;
            double grandTotal = finalTotal + deliveryFee + packagingFee;

        StringBuilder builder = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm / dd:MM:YYYY");
        
   
        builder.append("\n==========================================\n");
        builder.append("             OFFICIAL RECEIPT              \n");
        builder.append("==========================================\n");
        builder.append("Order ID  : ").append(order.getOrderId()).append(" ||  Date  : ").append(java.time.LocalDateTime.now().format(formatter)).append("\n");
        builder.append("Delivery  : City: ").append(order.getCity()).append(" | Phone: ").append(order.getPhone()).append("\n");
        builder.append("------------------------------------------\n");
        
        // حلقة تكرارية لطباعة اسم وسعر كل منتج في الطلب داخل الإيصال
        for (Product product : order.getProducts()) {
           builder.append(String.format(Locale.US, "%-30s | %.2f EGP\n", product.getName(), product.getPrice()));
        }

     
        builder.append("-----------------------------------------------------------------------------\n");
        builder.append(String.format(Locale.US, " Subtotal        : %.2f EGP%n", rawTotal));
        builder.append(String.format(Locale.US, " Discount        : -%.2f EGP%n", discountAmount));
        builder.append(String.format(Locale.US, " Delivery Fee    : %.2f EGP%n", deliveryFee));
        builder.append(String.format(Locale.US, "Packaging Fee  : %.2f EGP\n", packagingFee));
        builder.append(String.format(Locale.US, " FINAL TOTAL     : %.2f EGP%n", grandTotal));
        builder.append("============================================================================-\n");
        
       // تفاصيل التغليف
        builder.append("PACKAGING DETAILS:\n");
        builder.append("Type: ").append(packagingType).append("\n");
        builder.append("Total Items Packed: ").append(order.getProducts().size()).append(" pcs\n");
        builder.append("==========================================\n");

        // سياسة الإرجاع
        builder.append("RETURN POLICY:\n");
        builder.append("- Products can be returned within 14 days.\n");
        builder.append("- Official receipt is required for returns.\n");
        builder.append("==========================================\n\n");

        return builder.toString();
    }
    
    /**
     * دالة مساعدة (Overloaded Method) لتوليد الإيصال في حال تم تمرير كائن خدمة التوصيل (Delivery) مباشرة،
     * حيث تقوم بحساب قيمة التوصيل أولاً ثم استدعاء الدالة الأساسية.
     */
    public static String generateReceipt(Order order, Delivery delivery, double packagingFee, String packagingType) {
        double fee = (delivery != null) ? delivery.calculatePrice() : 0.0;
        return generateReceipt(order, fee, packagingFee, packagingType);
    }
}
