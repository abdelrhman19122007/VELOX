package com.app.util;

public class Response<T> {
    private final T data;
    private final String message;

    public Response(T data, String message) {
        this.data = data;
        this.message = message;
    }

    public T getData() { return data; }
    public String getMessage() { return message; }
}
