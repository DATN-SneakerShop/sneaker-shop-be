package com.sneakershop.backend.entity.product;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(
        name = "product_variant",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "sku"),
                @UniqueConstraint(
                        columnNames = {"product_id", "size", "sizeType", "colorway"}
                )
        }
)
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(name = "size")
    private String size;

    // ❗ KHÔNG snake_case
    @Column(name = "sizeType")
    private String sizeType;

    @Column(name = "colorway")
    private String colorway;

    private int stock;
    private String status;
    @Column(nullable = false)
    private BigDecimal price;

    @Column
    private BigDecimal salePrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}

