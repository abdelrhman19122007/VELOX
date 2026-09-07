package com.app.util;

/**
 * فئة عامة (Generic Class) تستخدم لتغليف البيانات المرجعة من العمليات مع رسالة توضيحية.
 * تساعد في توحيد شكل الاستجابة (Response) في التطبيق.
 * 
 * @param <T> نوع البيانات المراد إرجاعها داخل الاستجابة
 */
public class Response<T> {
    
    private final T data;       // البيانات أو النتيجة المراد إرجاعها
    private final String message; // رسالة توضيحية لحالة العملية (نجاح أو فشل)

    /**
     * مُنشئ (Constructor) لتهيئة الاستجابة بالبيانات والرسالة.
     * 
     * @param data البيانات
     * @param message الرسالة التوضيحية
     */
    public Response(T data, String message) {
        this.data = data;
        this.message = message;
    }

    /**
     * الحصول على البيانات المخزنة في الاستجابة.
     * @return البيانات من النوع T
     */
    public T getData() { 
        return data; 
    }

    /**
     * الحصول على الرسالة التوضيحية المرتبطة بالاستجابة.
     * @return نص الرسالة
     */
    public String getMessage() { 
        return message; 
    }
}
