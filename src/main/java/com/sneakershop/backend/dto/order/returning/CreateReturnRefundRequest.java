package com.sneakershop.backend.dto.order.returning;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class CreateReturnRefundRequest {
    @NotNull
    private Long orderId;

    private String reason;
    private String customerNote;
    private String adminNote;

    @Valid
    @NotEmpty
    private List<CreateReturnRefundItemRequest> items;
}
