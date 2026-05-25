package com.sneakershop.backend.dto.order.returning;

import com.sneakershop.backend.entity.order.enums.ReturnConditionStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReturnRefundItemResponse {
    private Long id;
    private Long orderItemId;
    private Long variantId;
    private String productName;
    private String sku;
    private String color;
    private String size;
    private Integer boughtQuantity;
    private Integer previouslyReturnedQuantity;
    private Integer quantity;
    private Integer receivedQuantity;
    private Integer restockQuantity;
    private BigDecimal unitPrice;
    private BigDecimal refundAmount;
    private ReturnConditionStatus conditionStatus;
    private String note;
    private List<ReturnRefundInspectionResponse> inspections;
}
