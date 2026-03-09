package com.sneakershop.backend.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CustomerHistoryDTO {

    private Long id;

    private String orderCode;

    private String customerName;

    private BigDecimal orderAmount;

    private LocalDateTime createdAt;

}