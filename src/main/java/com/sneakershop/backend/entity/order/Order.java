package com.sneakershop.backend.entity.order;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.login.User;
import com.sneakershop.backend.entity.order.enums.*;
import com.sneakershop.backend.entity.voucher.Voucher;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_code", columnList = "order_code"),
        @Index(name = "idx_orders_created_at", columnList = "created_at"),
        @Index(name = "idx_orders_status_created", columnList = "order_status, created_at"),
        @Index(name = "idx_orders_channel_created", columnList = "channel, created_at"),
        @Index(name = "idx_orders_customer_created", columnList = "khach_hang_id, created_at"),
        @Index(name = "idx_orders_created_by", columnList = "created_by, created_at"),
        @Index(name = "idx_orders_return_status", columnList = "return_status, created_at"),
        @Index(name = "idx_orders_payment_status", columnList = "payment_status, created_at"),
        @Index(name = "idx_orders_shipping_status", columnList = "shipping_status, created_at"),
        @Index(name = "idx_orders_lookup_code", columnList = "lookup_code")
})
@Where(clause = "deleted = false")
@Data
@ToString(exclude = {"items", "customer", "createdBy", "cancelledBy", "voucher", "cart"})
@EqualsAndHashCode(exclude = {"items", "customer", "createdBy", "cancelledBy", "voucher", "cart"})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true, length = 40)
    private String orderCode;

    @Column(name = "lookup_code", unique = true, length = 64)
    private String lookupCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private SalesChannel channel = SalesChannel.OFFLINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    private OrderStatus orderStatus = OrderStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_status", length = 30)
    private ShippingStatus shippingStatus = ShippingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_status", nullable = false, length = 30)
    private ReturnStatus returnStatus = ReturnStatus.NONE;

    @Column(name = "guest_order")
    private Boolean guestOrder = false;

    @Column(name = "orderer_name", length = 150)
    private String ordererName;

    @Column(name = "orderer_email", length = 150)
    private String ordererEmail;

    @Column(name = "orderer_phone", length = 20)
    private String ordererPhone;

    @Column(name = "receiver_name", length = 150)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    @Column(name = "address_label", length = 100)
    private String addressLabel;

    @Column(name = "shipping_province", length = 120)
    private String shippingProvince;

    @Column(name = "shipping_district", length = 120)
    private String shippingDistrict;

    @Column(name = "shipping_ward", length = 120)
    private String shippingWard;

    @Column(name = "shipping_detail_address", length = 255)
    private String shippingDetailAddress;

    @Column(name = "shipping_address_line", length = 500)
    private String shippingAddressLine;

    @Column(name = "subtotal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "promotion_discount_amount", precision = 15, scale = 2)
    private BigDecimal promotionDiscountAmount = BigDecimal.ZERO;

    @Column(name = "voucher_discount_amount", precision = 15, scale = 2)
    private BigDecimal voucherDiscountAmount = BigDecimal.ZERO;

    @Column(name = "shipping_discount_amount", precision = 15, scale = 2)
    private BigDecimal shippingDiscountAmount = BigDecimal.ZERO;

    @Column(name = "manual_discount_amount", precision = 15, scale = 2)
    private BigDecimal manualDiscountAmount = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "returned_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal returnedAmount = BigDecimal.ZERO;

    @Column(name = "final_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal finalAmount = BigDecimal.ZERO;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode = "VND";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    @Column(name = "voucher_name_snapshot", length = 255)
    private String voucherNameSnapshot;

    @Column(name = "voucher_type_snapshot", length = 20)
    private String voucherTypeSnapshot;

    @Column(name = "voucher_value_snapshot")
    private Long voucherValueSnapshot;

    @Column(name = "free_ship_voucher_code", length = 50)
    private String freeShipVoucherCode;

    @Column(name = "promotion_code", length = 50)
    private String promotionCode;

    @Column(name = "applied_promotion_summary", length = 255)
    private String appliedPromotionSummary;

    @Column(name = "shipping_carrier", length = 50)
    private String shippingCarrier;

    @Column(name = "tracking_code", length = 100)
    private String trackingCode;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "return_note", columnDefinition = "TEXT")
    private String returnNote;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "delivery_fail_reason", length = 255)
    private String deliveryFailReason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "delivery_failed_at")
    private LocalDateTime deliveryFailedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "email_sent", nullable = false)
    private Boolean emailSent = false;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;



    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.finalAmount == null || this.finalAmount.compareTo(BigDecimal.ZERO) == 0) this.finalAmount = this.totalAmount;
        if (this.lookupCode == null || this.lookupCode.trim().isEmpty()) this.lookupCode = "LOOKUP-" + System.currentTimeMillis();
        rebuildShippingAddressLine();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        rebuildShippingAddressLine();
    }

    public void rebuildShippingAddressLine() {
        StringBuilder sb = new StringBuilder();
        if (shippingDetailAddress != null && !shippingDetailAddress.trim().isEmpty()) sb.append(shippingDetailAddress.trim());
        if (shippingWard != null && !shippingWard.trim().isEmpty()) { if (sb.length() > 0) sb.append(", "); sb.append(shippingWard.trim()); }
        if (shippingDistrict != null && !shippingDistrict.trim().isEmpty()) { if (sb.length() > 0) sb.append(", "); sb.append(shippingDistrict.trim()); }
        if (shippingProvince != null && !shippingProvince.trim().isEmpty()) { if (sb.length() > 0) sb.append(", "); sb.append(shippingProvince.trim()); }
        this.shippingAddressLine = sb.length() == 0 ? null : sb.toString();
    }
}
