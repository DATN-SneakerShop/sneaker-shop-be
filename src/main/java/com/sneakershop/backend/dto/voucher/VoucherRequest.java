package com.sneakershop.backend.dto.voucher;

import lombok.Data;

import javax.persistence.Column;
import java.time.LocalDateTime;

@Data
public class VoucherRequest {

    private String name;
    private String type;

    private Long value;
    private Long maxDiscount;
    private Long minOrderValue;

    private Integer quantity;
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