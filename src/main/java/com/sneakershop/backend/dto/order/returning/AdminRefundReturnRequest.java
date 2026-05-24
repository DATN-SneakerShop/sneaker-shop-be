package com.sneakershop.backend.dto.order.returning;

import com.sneakershop.backend.entity.order.enums.RefundMethod;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminRefundReturnRequest {
    private RefundMethod refundMethod;
    private BigDecimal refundAmount;
    private String transactionCode;
    private String adminNote;
}
