package com.sneakershop.backend.dto.order.returning;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class AdminReceiveReturnRequest {
    private String adminNote;

    @Valid
    @NotEmpty
    private List<AdminReceiveReturnItemRequest> items;
}
