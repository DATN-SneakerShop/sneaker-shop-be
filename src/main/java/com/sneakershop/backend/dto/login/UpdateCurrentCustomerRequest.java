package com.sneakershop.backend.dto.login;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateCurrentCustomerRequest {
    private String fullName;
    private String phone;
    private LocalDate ngaySinh;
}