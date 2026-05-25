package com.sneakershop.backend.dto.order.returning;

import com.sneakershop.backend.entity.order.enums.ReturnConditionStatus;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class AdminReceiveReturnItemRequest {
    @NotNull
    private Long returnItemId;

    @NotNull
    @Min(0)
    private Integer receivedQuantity;

    @NotNull
    @Min(0)
    private Integer restockQuantity;

    private ReturnConditionStatus conditionStatus;
    private String note;

    @Valid
    private List<AdminReceiveReturnInspectionRequest> inspections;
}
