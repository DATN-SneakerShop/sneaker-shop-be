package com.sneakershop.backend.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSpendingDTO {
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String phone;
    private String rankName;
    private Integer point;
    private BigDecimal totalSpent;
    private Long orderCount;
    private LocalDateTime lastOrderAt;
}
