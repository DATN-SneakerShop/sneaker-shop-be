package com.sneakershop.backend.dto.order.storefront;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class StorefrontOrderDetailResponse {
    private Long id;
    private String orderCode;
    private String lookupCode;

    private String orderStatus;
    private String paymentStatus;
    private String shippingStatus;
    private String returnStatus;
    private String paymentMethod;

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

    private String shippingCarrier;
    private String trackingCode;

    private String note;
    private String cancelReason;
    private String returnNote;
    private String deliveryFailReason;

    private BigDecimal subtotalAmount;
    private BigDecimal promotionDiscountAmount;
    private BigDecimal voucherDiscountAmount;
    private BigDecimal shippingDiscountAmount;
    private BigDecimal manualDiscountAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private BigDecimal returnedAmount;
    private BigDecimal finalAmount;

    private String voucherCode;
    private String voucherNameSnapshot;
    private String voucherTypeSnapshot;
    private Long voucherValueSnapshot;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime returnedAt;

    private List<StorefrontOrderItemResponse> items = new ArrayList<>();
}