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

    // nullable: khách lẻ
    private Long customerId;

    // nullable -> default OFFLINE
    private SalesChannel channel = SalesChannel.OFFLINE;

    // nullable -> default COD
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    // nếu true -> set paymentStatus = PAID (phù hợp đơn offline thu tiền tại quầy)
    private Boolean paidNow = false;

    private BigDecimal shippingFee = BigDecimal.ZERO;

    private String shippingCarrier;
    private String trackingCode;

    private String note;

    private String currencyCode = "VND";

    @Valid
    @NotEmpty
    private List<CreateOrderItemRequest> items;
}
