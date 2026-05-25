package com.sneakershop.backend.dto.order.returning;

import com.sneakershop.backend.entity.order.enums.ReturnConditionStatus;
import com.sneakershop.backend.entity.order.enums.ReturnDispositionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReturnInventoryDispositionResponse {
    private Long id;
    private Long returnItemId;
    private Long inspectionId;
    private Long variantId;
    private ReturnConditionStatus conditionStatus;
    private ReturnDispositionType dispositionType;
    private Integer quantity;
    private Integer restockQuantity;
    private Integer nonResellableQuantity;
    private String responsibility;
    private String warehouseLocation;
    private String note;
    private String createdBy;
    private LocalDateTime createdAt;
}
