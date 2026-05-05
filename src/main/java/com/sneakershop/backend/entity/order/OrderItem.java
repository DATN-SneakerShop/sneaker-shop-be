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
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_items_order", columnList = "order_id"),
        @Index(name = "idx_order_items_variant", columnList = "variant_id"),
        @Index(name = "idx_order_items_returned", columnList = "returned_quantity"),
        @Index(name = "idx_order_items_variant_snapshot", columnList = "variant_id_snapshot"),
        @Index(name = "idx_order_items_product_snapshot", columnList = "product_id_snapshot")
})
@Data
@ToString(exclude = {"order", "variant"})
@EqualsAndHashCode(exclude = {"order", "variant"})
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    @JsonIgnore
    private ProductVariant variant;

    @Transient
    public Long getVariantId() { return variant != null ? variant.getId() : null; }

    @Column(name = "product_id_snapshot")
    private Long productIdSnapshot;

    @Column(name = "variant_id_snapshot")
    private Long variantIdSnapshot;

    @Column(name = "sku_snapshot", length = 80)
    private String skuSnapshot;

    @Column(name = "product_name_snapshot", length = 255)
    private String productNameSnapshot;

    @Column(name = "color_snapshot", length = 100)
    private String colorSnapshot;

    @Column(name = "size_snapshot", length = 50)
    private String sizeSnapshot;

    @Column(name = "material_snapshot", length = 100)
    private String materialSnapshot;

    @Column(name = "sole_snapshot", length = 100)
    private String soleSnapshot;

    @Column(name = "image_url_snapshot", length = 500)
    private String imageUrlSnapshot;

    @Column(name = "base_unit_price", precision = 15, scale = 2)
    private BigDecimal baseUnitPrice = BigDecimal.ZERO;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "promotion_discount_amount", precision = 15, scale = 2)
    private BigDecimal promotionDiscountAmount = BigDecimal.ZERO;

    @Column(name = "line_discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineDiscountAmount = BigDecimal.ZERO;

    @Column(name = "line_total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotalAmount = BigDecimal.ZERO;

    @Column(name = "returned_quantity", nullable = false)
    private Integer returnedQuantity = 0;

    @Column(name = "return_note", columnDefinition = "TEXT")
    private String returnNote;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;
}
