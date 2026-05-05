package com.sneakershop.backend.dto.order.admin;

import com.sneakershop.backend.entity.order.enums.OrderStatus;
import com.sneakershop.backend.entity.order.enums.PaymentMethod;
import com.sneakershop.backend.entity.order.enums.PaymentStatus;
import com.sneakershop.backend.entity.order.enums.ReturnStatus;
import com.sneakershop.backend.entity.order.enums.SalesChannel;
import com.sneakershop.backend.entity.order.enums.ShippingStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminOrderDetailDTO {
    private Long id;
    private String orderCode;
    private String lookupCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private ShippingStatus shippingStatus;
    private ReturnStatus returnStatus;
    private PaymentMethod paymentMethod;
    private SalesChannel channel;

    private Long customerId;
    private Long createdById;
    private Boolean guestOrder;

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

    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal promotionDiscountAmount;
    private BigDecimal voucherDiscountAmount;
    private BigDecimal shippingDiscountAmount;
    private BigDecimal manualDiscountAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private BigDecimal finalAmount;
    private BigDecimal returnedAmount;

    private BigDecimal receivedAmount;
    private BigDecimal paymentActualAmount;
    private LocalDateTime lastTransferReceivedAt;
    private LocalDateTime paymentReceivedAt;

    private String paymentCode;
    private String bankCode;
    private String bankName;
    private String bankAccountNo;
    private String bankAccountName;
    private String qrImageUrl;

    private String voucherCode;
    private String voucherNameSnapshot;
    private String voucherTypeSnapshot;
    private Long voucherValueSnapshot;
    private String promotionCode;
    private String appliedPromotionSummary;

    private String shippingCarrier;
    private String trackingCode;
    private String deliveryFailReason;

    private String note;
    private String cancelReason;
    private String returnNote;

    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime deliveryFailedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime returnedAt;

    private Boolean emailSent;
    private LocalDateTime emailSentAt;

    private List<AdminOrderPaymentHistoryDTO> paymentHistory;
    private List<AdminOrderItemDTO> items;
}
