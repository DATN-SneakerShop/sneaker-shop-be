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
    private Long voucherId;
    @NotNull
    private SalesChannel channel;

    @NotNull
    private PaymentMethod paymentMethod;

    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private String note;
    private Long freeShipVoucherId;

    // Admin tạo đơn ONLINE: dùng các field đã có sẵn trên Order entity, không đổi schema/entity.
    private String ordererName;
    private String ordererEmail;
    private String ordererPhone;

    private String receiverName;
    private String receiverPhone;
    private String addressLabel;
    private String shippingProvince;
    private String shippingDistrict;
    private String shippingWard;
    private String shippingDetailAddress;
    private String shippingAddressLine;

    @Valid
    @NotEmpty
    private List<OrderItemCreateRequest> items;
}
