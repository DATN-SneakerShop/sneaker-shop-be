package com.sneakershop.backend.dto.order.admin;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AdminDeliveryFailedRequest {
    @NotBlank(message = "Lý do giao hàng không thành công không được để trống.")
    private String reason;
}
