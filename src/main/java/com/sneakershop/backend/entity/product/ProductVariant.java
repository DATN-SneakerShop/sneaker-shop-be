package com.sneakershop.backend.entity.product;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Getter @Setter
@Table(
        name = "product_variant",
        uniqueConstraints = @UniqueConstraint(columnNames = "sku")
)
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sku;

    private String size;
    private String colorway;

    private BigDecimal price;
    private BigDecimal salePrice;

    private int stock;

    private String status;    // IN_STOCK / OUT_OF_STOCK

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}

