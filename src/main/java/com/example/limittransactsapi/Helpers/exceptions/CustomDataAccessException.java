package com.example.limittransactsapi.Helpers.exceptions;

// Класс исключения для ошибок доступа данных
public class CustomDataAccessException extends RuntimeException {
    public CustomDataAccessException(String message) {
        super(message);
    }

    public CustomDataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
