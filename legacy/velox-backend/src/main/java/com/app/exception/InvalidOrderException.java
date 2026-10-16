package com.app.exception;

// 
public class InvalidOrderException extends Exception {
    // كونسرتكر بياخد رسالة الخطأ ويبعتها للكلاس الأب (Exception)
    public InvalidOrderException(String message) {
        super(message);
    }
}
