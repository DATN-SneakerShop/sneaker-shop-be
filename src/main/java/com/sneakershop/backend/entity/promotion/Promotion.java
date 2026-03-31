package com.sneakershop.backend.entity.promotion;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(name = "customer_group")
    private String customerGroup;

    @Column(nullable = false)
    private Boolean deleted = false;

    // Thay thế @ManyToMany cũ bằng @OneToMany trỏ tới bảng chi tiết
    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PromotionDetail> promotionDetails = new ArrayList<>();
}