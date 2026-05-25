package com.sneakershop.backend.dto.order.returning;

import com.sneakershop.backend.entity.order.enums.ReturnConditionStatus;
import com.sneakershop.backend.entity.order.enums.ReturnDispositionType;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class AdminReceiveReturnInspectionRequest {
    @NotNull
    private ReturnConditionStatus conditionStatus;

    @NotNull
    @Min(0)
    private Integer quantity;

    @NotNull
    @Min(0)
    private Integer restockQuantity;

    @NotNull
    @Min(0)
    private Integer refundQuantity;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal refundRate;

    private String responsibility;
    private ReturnDispositionType dispositionType;
    private String warehouseLocation;
    private String note;
}
