package com.sneakershop.backend.controller.customer;

import com.sneakershop.backend.audit.SystemAuditLogService;
import com.sneakershop.backend.exception.ErrorResponse;
import com.sneakershop.backend.exception.OutOfStockException;
import com.sneakershop.backend.exception.PaymentException;
import com.sneakershop.backend.exception.ValidationException;
import com.sneakershop.backend.exception.VoucherInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final SystemAuditLogService auditLogService;
    private final HttpServletRequest request;

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException e) {
        if (e.getField() != null && !e.getField().isBlank()) {
            return ResponseEntity.badRequest().body(ErrorResponse.field(e.getField(), e.getMessage()));
        }
        return ResponseEntity.badRequest().body(ErrorResponse.of(e.getMessage()));
    }

    @ExceptionHandler({OutOfStockException.class, VoucherInvalidException.class, PaymentException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBusinessException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(safeBusinessMessage(e.getMessage())));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException e) {
        HttpStatus status = e.getStatus();
        String message = e.getReason() == null ? "Dữ liệu không hợp lệ." : e.getReason();
        return ResponseEntity.status(status).body(ErrorResponse.of(safeBusinessMessage(message)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ErrorResponse response = ErrorResponse.of(fieldErrors.isEmpty() ? "Dữ liệu không hợp lệ." : "Dữ liệu không hợp lệ.");
        response.setFieldErrors(fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("Dữ liệu không hợp lệ."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String raw = String.valueOf(e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage()).toLowerCase();
        String message = "Dữ liệu đã tồn tại hoặc không hợp lệ.";
        if (raw.contains("sku") && raw.contains("product_variant")) message = "Mã SKU biến thể đã được sử dụng.";
        else if (raw.contains("sku") && raw.contains("product")) message = "Mã sản phẩm đã được sử dụng.";
        else if (raw.contains("email")) message = "Email này đã được đăng ký.";
        else if (raw.contains("username")) message = "Tên đăng nhập đã được sử dụng.";
        else if (raw.contains("phone") || raw.contains("so_dien_thoai")) message = "Số điện thoại đã được sử dụng.";
        else if (raw.contains("voucher") && raw.contains("code")) message = "Mã voucher đã tồn tại.";
        else if (raw.contains("name")) message = "Tên đã tồn tại.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(message));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        System.err.println("❌ Lỗi nghiệp vụ: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(safeBusinessMessage(e.getMessage())));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e, Principal principal) {
        String username = principal != null ? principal.getName() : "GUEST/UNAUTHORIZED";
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        auditLogService.logManual(username, ip, "SECURITY", "UNAUTHORIZED_ACCESS", "System",
                "Cảnh báo: Cố tình truy cập trái phép hoặc thao tác vượt quyền vào module: " + uri,
                "FAILED", e.getMessage(), "DANGER");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.of("Cảnh báo: Bạn không có quyền thực hiện thao tác này!"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e, Principal principal) {
        String username = principal != null ? principal.getName() : "SYSTEM";
        String ip = request.getRemoteAddr();
        auditLogService.logManual(username, ip, "SYSTEM", "INTERNAL_ERROR", "System",
                "Phát hiện lỗi hệ thống nghiêm trọng tại " + request.getRequestURI(),
                "FAILED", e.getClass().getSimpleName(), "ERROR");
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("Hệ thống đang gặp lỗi. Vui lòng thử lại sau hoặc liên hệ quản trị viên."));
    }

    private String safeBusinessMessage(String message) {
        if (message == null || message.isBlank()) return "Dữ liệu không hợp lệ.";
        String lower = message.toLowerCase();
        if (lower.contains("sqlexception") || lower.contains("constraint") || lower.contains("nullpointerexception")
                || lower.contains("could not execute") || lower.contains("hibernate")) {
            return "Dữ liệu không hợp lệ.";
        }
        return message;
    }
}
