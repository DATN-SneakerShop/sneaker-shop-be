package com.sneakershop.backend.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InactiveCustomerDTO {

    private Long customerId;
    private String customerName;
    private long daysSinceLastOrder;

}