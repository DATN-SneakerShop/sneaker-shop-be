package com.sneakershop.backend.controller.customer;

import com.sneakershop.backend.audit.SystemAuditLogService;
import com.sneakershop.backend.exception.OutOfStockException;
import com.sneakershop.backend.exception.PaymentException;
import com.sneakershop.backend.exception.VoucherInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final SystemAuditLogService auditLogService;
    private final HttpServletRequest request;


    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<String> handleOutOfStock(OutOfStockException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(VoucherInvalidException.class)
    public ResponseEntity<String> handleVoucherInvalid(VoucherInvalidException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<String> handlePaymentException(PaymentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        System.err.println("❌ Lỗi Logic: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    // 🔥 YÊU CẦU 3 & 5: CẢNH BÁO XÂM NHẬP TRÁI PHÉP VÀ SAI QUYỀN MODULE
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDeniedException(AccessDeniedException e, Principal principal) {
        String username = principal != null ? principal.getName() : "GUEST/UNAUTHORIZED";
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();

        auditLogService.logManual(
                username, ip, "SECURITY", "UNAUTHORIZED_ACCESS", "System",
                "Cảnh báo: Cố tình truy cập trái phép hoặc thao tác vượt quyền vào module: " + uri,
                "FAILED", e.getMessage(), "DANGER"
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cảnh báo: Bạn không có quyền thực hiện thao tác này!");
    }

    // 🔥 YÊU CẦU 2: GHI LOG TẤT CẢ LỖI HỆ THỐNG (LỖI 500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception e, Principal principal) {
        String username = principal != null ? principal.getName() : "SYSTEM";
        String ip = request.getRemoteAddr();

        auditLogService.logManual(
                username, ip, "SYSTEM", "INTERNAL_ERROR", "System",
                "Phát hiện lỗi hệ thống nghiêm trọng tại " + request.getRequestURI(),
                "FAILED", e.getMessage(), "ERROR"
        );

        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống: " + e.getMessage());
    }
}