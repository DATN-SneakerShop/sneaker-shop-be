package com.sneakershop.backend.dto.login;

import lombok.Data;

import java.util.List;

@Data
public class CurrentAccountResponse {
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private List<String> roles;
    private CurrentCustomerResponse customerInfo;
}