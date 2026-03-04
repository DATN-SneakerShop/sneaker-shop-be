package com.sneakershop.backend.dto.order;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class ReturnOrderRequest {

    private String note;

    @Valid
    @NotEmpty
    private List<ReturnOrderItemRequest> items;
}
