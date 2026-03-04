package com.sneakershop.backend.dto.order;

import com.sneakershop.backend.entity.order.enums.OrderStatus;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UpdateOrderStatusRequest {
    @NotNull
    private OrderStatus status;
}
