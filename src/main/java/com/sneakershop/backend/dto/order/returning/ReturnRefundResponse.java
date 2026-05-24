package com.sneakershop.backend.dto.order.returning;

import com.sneakershop.backend.entity.order.enums.RefundMethod;
import com.sneakershop.backend.entity.order.enums.ReturnRequestStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReturnRefundResponse {
    private Long id;
    private String code;
    private Long orderId;
    private String orderCode;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private ReturnRequestStatus status;
    private String reason;
    private String customerNote;
    private String adminNote;
    private String rejectReason;
    private BigDecimal refundAmount;
    private RefundMethod refundMethod;
    private String refundTransactionCode;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime refundedAt;
    private LocalDateTime completedAt;
    private List<ReturnRefundItemResponse> items;
    private List<ReturnRefundHistoryResponse> histories;
}
