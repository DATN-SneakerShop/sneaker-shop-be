package com.sneakershop.backend.dto.voucher;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VoucherResponse {

    private Long id;
    private String code;
    private String name;
    private String type;
    private Long value;
    private Long maxDiscount;
    private Long minOrderValue;
    private Integer quantity;
    private Integer usedCount;
    private String status;
    private Boolean isPublic;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean applyBirthdayMonth = false;
    private Integer limitCustomerDays;
    private Long minCustomerSpent;
    private Integer maxDaysSinceLastOrder;
    private Boolean isFirstOrderOnly = false;
}