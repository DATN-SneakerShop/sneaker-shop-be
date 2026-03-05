package com.sneakershop.backend.entity.order;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sneakershop.backend.entity.product.ProductVariant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "order_items",
        indexes = {
                @Index(name="idx_order_items_order", columnList="order_id"),
                @Index(name="idx_order_items_variant", columnList="variant_id"),
                @Index(name="idx_order_items_returned", columnList="returned_quantity")
        }
)
@Data
@ToString(exclude = {"order", "variant"})
@EqualsAndHashCode(exclude = {"order", "variant"})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="order_id", nullable=false)
    @JsonBackReference
    private Order order;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="variant_id", nullable=false)
    @JsonIgnore
    private ProductVariant variant;

    /**
     * Giúp FE lấy được variantId khi API đang trả entity trực tiếp,
     * mà không cần serialize toàn bộ ProductVariant (tránh nặng JSON / lỗi lazy proxy).
     */
    @Transient
    public Long getVariantId() {
        return variant != null ? variant.getId() : null;
    }

    // Snapshot để in hóa đơn/PDF không bị lệch khi đổi tên/SKU sau này
    @Column(name="sku_snapshot", length=80)
    private String skuSnapshot;

    @Column(name="product_name_snapshot", length=255)
    private String productNameSnapshot;

    @Column(name="unit_price", nullable=false, precision=15, scale=2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name="quantity", nullable=false)
    private Integer quantity = 1;

    @Column(name="line_discount_amount", nullable=false, precision=15, scale=2)
    private BigDecimal lineDiscountAmount = BigDecimal.ZERO;

    @Column(name="line_total_amount", nullable=false, precision=15, scale=2)
    private BigDecimal lineTotalAmount = BigDecimal.ZERO;

    // ===== Return tracking theo sản phẩm (đủ cho đồ án, không cần tách bảng return) =====
    @Column(name="returned_quantity", nullable=false)
    private Integer returnedQuantity = 0;

    @Column(name="return_note", columnDefinition = "TEXT")
    private String returnNote;

    @Column(name="returned_at")
    private LocalDateTime returnedAt;
}