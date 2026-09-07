package com.app.util;

/**
 * فئة مخصصة (Custom Exception) للتعامل مع الأخطاء والمشاكل المتعلقة بعمليات التوصيل.
 * ترث من فئة الاستثناءات الأساسية في جافا (Exception).
 */
public class DeliveryException extends Exception {

    /**
     * مُنشئ (Constructor) لاستقبال رسالة الخطأ وتمريرها.
     * 
     * @param message رسالة الخطأ التفصيلية التي توضح سبب المشكلة في التوصيل
     */
    public DeliveryException(String message) {
        super(message); // تمرير رسالة الخطأ إلى الفئة الأب (Exception)
    }
}
