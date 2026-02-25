package com.sneakershop.backend.entity.promotion;

import com.sneakershop.backend.entity.product.ProductVariant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Where(clause = "deleted = false")
@Table(
        name = "promotion",
        indexes = {
                @Index(name = "idx_promotion_time", columnList = "start_time, end_time"),
                @Index(name = "idx_promotion_active", columnList = "active"),
                @Index(name = "idx_promotion_priority", columnList = "priority")
        }
)
@Getter
@Setter
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên khuyến mãi
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    // Loại giảm: PERCENT / AMOUNT
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    // Giá trị giảm
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    // Thời gian bắt đầu
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    // Thời gian kết thúc
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // Bật / tắt
    @Column(nullable = false)
    private Boolean active = true;

    // Độ ưu tiên
    @Column(nullable = false)
    private Integer priority = 0;

    @Column(name = "customer_group")
    private String customerGroup;

    @Column(nullable = false)
    private Boolean deleted = false;

    // Variant áp dụng
    @ManyToMany
    @JoinTable(
            name = "promotion_variant",
            joinColumns = @JoinColumn(name = "promotion_id"),
            inverseJoinColumns = @JoinColumn(name = "variant_id")
    )
    private Set<ProductVariant> variants = new HashSet<>();
}
