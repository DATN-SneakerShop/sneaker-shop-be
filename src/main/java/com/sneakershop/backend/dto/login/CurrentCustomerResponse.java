package com.sneakershop.backend.dto.login;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CurrentCustomerResponse {
    private Long id;
    private String ten;
    private String email;
    private String phone;
    private LocalDate ngaySinh;
    private Integer diemTichLuy;
    private String loaiKhach;
    private String status;
    private String ghiChu;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}