package com.sneakershop.backend.entity.order;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.login.User;
import com.sneakershop.backend.entity.order.enums.*;
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
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_code", columnList = "order_code"),
                @Index(name = "idx_orders_created_at", columnList = "created_at"),
                @Index(name = "idx_orders_status_created", columnList = "order_status, created_at"),
                @Index(name = "idx_orders_channel_created", columnList = "channel, created_at"),
                @Index(name = "idx_orders_customer_created", columnList = "khach_hang_id, created_at"),
                @Index(name = "idx_orders_created_by", columnList = "created_by, created_at"),
                @Index(name = "idx_orders_return_status", columnList = "return_status, created_at")
        }
)
@Where(clause = "deleted = false") // xóa = soft delete cho đồ án (không mất dữ liệu báo cáo)
@Data
@ToString(exclude = {"items"})
@EqualsAndHashCode(exclude = {"items"})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Search/in/export/PDF
    @Column(name = "order_code", nullable = false, unique = true, length = 40)
    private String orderCode;

    // chọn khách khi tạo đơn (nullable nếu khách lẻ)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khach_hang_id")
    private Customer customer;

    // đơn theo nhân viên bán
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // Kênh bán: online/offline
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private SalesChannel channel = SalesChannel.OFFLINE;

    // Trạng thái đơn: mới/đang xử lý/đang giao/hoàn tất/hủy
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    private OrderStatus orderStatus = OrderStatus.NEW;

    // Trạng thái thanh toán
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    // Phương thức thanh toán (đơn giản cho đồ án)
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    // Trạng thái hoàn trả (để report “đơn hoàn trả”, “cập nhật trạng thái trả hàng”)
    @Enumerated(EnumType.STRING)
    @Column(name = "return_status", nullable = false, length = 30)
    private ReturnStatus returnStatus = ReturnStatus.NONE;

    // ===== Snapshot tiền (phục vụ báo cáo) =====
    @Column(name="subtotal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name="discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name="shipping_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    // Tổng trước hoàn trả
    @Column(name="total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // Tổng giá trị hoàn trả (để cập nhật doanh thu sau hoàn trả)
    @Column(name="returned_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal returnedAmount = BigDecimal.ZERO;

    // Tổng cuối cùng sau hoàn trả (net revenue)
    @Column(name="final_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal finalAmount = BigDecimal.ZERO;

    @Column(name="currency_code", nullable = false, length = 10)
    private String currencyCode = "VND";

    // ===== Promo phục vụ “in hóa đơn kèm khuyến mãi” (đơn giản cho đồ án) =====
    @Column(name="promotion_code", length = 50)
    private String promotionCode;

    @Column(name="promotion_discount_amount", precision = 15, scale = 2)
    private BigDecimal promotionDiscountAmount = BigDecimal.ZERO;

    // ===== Shipping (đơn giản) =====
    @Column(name="shipping_carrier", length = 50)
    private String shippingCarrier;

    @Column(name="tracking_code", length = 100)
    private String trackingCode;

    // ===== Notes =====
    @Column(name="note", columnDefinition = "TEXT")
    private String note;

    @Column(name="return_note", columnDefinition = "TEXT")
    private String returnNote;

    // ===== Timeline phục vụ lọc theo thời gian / báo cáo ngày/tuần/tháng =====
    @Column(name="created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @Column(name="shipped_at")
    private LocalDateTime shippedAt;

    @Column(name="completed_at")
    private LocalDateTime completedAt;

    @Column(name="cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name="returned_at")
    private LocalDateTime returnedAt;

    // Hủy đơn
    @Column(name="cancel_reason", length=255)
    private String cancelReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="cancelled_by")
    private User cancelledBy;

    // Email xác nhận đơn (phục vụ “tự động email xác nhận”)
    @Column(name="email_sent", nullable = false)
    private Boolean emailSent = false;

    @Column(name="email_sent_at")
    private LocalDateTime emailSentAt;

    // Soft delete để có “xóa” mà vẫn giữ dữ liệu báo cáo
    @Column(name="deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name="deleted_at")
    private LocalDateTime deletedAt;

    // ===== Items =====
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;

        // default final = total (khi chưa hoàn)
        if (this.finalAmount == null || this.finalAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.finalAmount = this.totalAmount;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private com.sneakershop.backend.entity.voucher.Voucher voucher;

    @Column(name = "voucher_code", length = 50)
    private String voucherCode;
}