package com.sneakershop.backend.entity.pricing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sneakershop.backend.entity.product.ProductVariant;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "variant_price_group")
@Data
public class VariantPriceGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔥 FIX LỖI VÒNG LẶP: Thêm @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "variant_id", nullable = false)
    @JsonIgnore
    private ProductVariant variant;

    // VIP hoặc NORMAL
    @Column(name = "loai_khach", nullable = false)
    private String loaiKhach;

    // Giá dành cho nhóm đó
    @Column(nullable = false)
    private BigDecimal price;
}