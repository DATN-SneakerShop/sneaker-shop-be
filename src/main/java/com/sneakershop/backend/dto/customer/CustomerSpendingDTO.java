package com.sneakershop.backend.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CustomerSpendingDTO {

    private Long customerId;

    private String customerName;

    private BigDecimal totalSpent;

}
