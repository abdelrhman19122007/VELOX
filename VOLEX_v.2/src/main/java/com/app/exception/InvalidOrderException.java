package com.app.exception;

// استثناء مخصص للتعامل مع اخطاء الطلبات غير الصالحه
public class InvalidOrderException extends Exception {
    // ينشئ الاستثناء ويمرر رساله الخطاء للكلاس الاب
    public InvalidOrderException(String message) {
        super(message);
    }
}
