package com.sneakershop.backend.entity.customer;

import lombok.Data;
import javax.persistence.*;

@Entity
@Table(name = "customer_rank_config")
@Data
public class CustomerRank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_hang", nullable = false, unique = true)
    private String name; // NORMAL, LOYALTY, VIP

    @Column(name = "diem_toi_thieu", nullable = false)
    private Integer minPoints; // Số điểm tối thiểu để đạt hạng này (vd: 5000)

    @Column(name = "phan_tram_giam_gia")
    private Integer discountPercent = 0; // % giảm giá mặc định (vd: 5%)

    @Column(name = "mo_ta")
    private String description;
}