package com.sneakershop.backend.dto.order;

import com.sneakershop.backend.entity.order.enums.PaymentMethod;
import com.sneakershop.backend.entity.order.enums.SalesChannel;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderRequest {
    private Long customerId;   // null = khách lẻ
    private Long createdById;  // optional (nếu chưa lấy từ security)

    @NotNull
    private SalesChannel channel;

    @NotNull
    private PaymentMethod paymentMethod;

    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private String note;

    @Valid
    @NotEmpty
    private List<OrderItemCreateRequest> items;
}
