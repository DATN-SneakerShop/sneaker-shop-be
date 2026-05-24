package com.sneakershop.backend.dto.order.returning;

import com.sneakershop.backend.entity.order.enums.ReturnRequestStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReturnRefundHistoryResponse {
    private ReturnRequestStatus oldStatus;
    private ReturnRequestStatus newStatus;
    private String note;
    private String createdBy;
    private LocalDateTime createdAt;
}
