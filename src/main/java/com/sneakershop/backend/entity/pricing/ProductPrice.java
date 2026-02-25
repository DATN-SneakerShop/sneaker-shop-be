package com.sneakershop.backend.entity.pricing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sneakershop.backend.entity.product.ProductVariant;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(
        name = "product_price",
        indexes = {
                @Index(name = "idx_price_variant", columnList = "variant_id"),
                @Index(name = "idx_price_active", columnList = "end_date")
        }
)
public class ProductPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK → product_variant.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    // FK → currency.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Currency currency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    // ✅ PHẢI nullable (giá đang active)
    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;
}
