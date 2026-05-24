package com.sneakershop.backend.dto.inventory;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockTransactionDTO {
    private Long id;
    private Long variantId;
    private String sku;
    private String type;
    private int quantity;
    private int beforeStock;
    private int afterStock;
    private int beforeReservedQuantity;
    private int afterReservedQuantity;
    private int beforeAvailableStock;
    private int afterAvailableStock;
    private String referenceType;
    private Long referenceId;
    private String note;
    private LocalDateTime createdAt;
}
