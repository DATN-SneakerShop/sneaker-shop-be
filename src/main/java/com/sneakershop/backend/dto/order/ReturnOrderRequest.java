package com.sneakershop.backend.dto.order;

import com.sneakershop.backend.entity.order.enums.ReturnStatus;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ReturnOrderRequest {
    @NotNull
    private ReturnStatus returnStatus;

    private String returnNote;

    @Valid
    private List<ReturnItemRequest> items;

    // nếu muốn override tổng tiền hoàn (không bắt buộc)
    private BigDecimal returnedAmountOverride;
}
