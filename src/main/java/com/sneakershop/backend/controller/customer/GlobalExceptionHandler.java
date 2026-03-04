package com.sneakershop.backend.controller.customer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ✅ BỘ LỌC LỖI TẬP TRUNG
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        // 1. Chỉ in 1 dòng thông báo ra Console IDE cho sạch (không văng stack trace)
        System.err.println("❌ Lỗi Logic: " + e.getMessage());

        // 2. Trả về mã lỗi 400 (Bad Request) kèm đúng cái câu String tin nhắn
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(e.getMessage());
    }
}