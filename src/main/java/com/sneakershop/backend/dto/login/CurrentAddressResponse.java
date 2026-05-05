package com.sneakershop.backend.dto.login;

import lombok.Data;

@Data
public class CurrentAddressResponse {
    private Long id;
    private String label;
    private String recipientName;
    private String phone;
    private String province;
    private String district;
    private String ward;
    private String detailAddress;
    private Integer isDefault;
}