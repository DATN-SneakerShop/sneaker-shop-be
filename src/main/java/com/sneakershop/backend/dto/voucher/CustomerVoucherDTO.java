package com.sneakershop.backend.dto.voucher;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CustomerVoucherDTO {
    private Long id;
    private String ten;
    private String email;
    private LocalDate ngaySinh;
    private String loaiKhach;
    private Long totalOrders;
    private BigDecimal totalSpent;
    private LocalDateTime createdAt; // Đổi thành LocalDateTime để khớp Entity Order

    public CustomerVoucherDTO(Long id, String ten, String email, LocalDate ngaySinh,
                              String loaiKhach, Long totalOrders, Object totalSpent,
                              LocalDateTime createdAt) { // Tham số cuối là LocalDateTime
        this.id = id;
        this.ten = ten;
        this.email = email;
        this.ngaySinh = ngaySinh;
        this.loaiKhach = loaiKhach;
        this.totalOrders = (totalOrders != null) ? totalOrders : 0L;
        this.createdAt = createdAt;

        // Xử lý totalSpent an toàn từ kết quả SUM của Hibernate
        if (totalSpent instanceof BigDecimal) {
            this.totalSpent = (BigDecimal) totalSpent;
        } else if (totalSpent instanceof Number) {
            this.totalSpent = new BigDecimal(((Number) totalSpent).toString());
        } else {
            this.totalSpent = BigDecimal.ZERO;
        }
    }
}