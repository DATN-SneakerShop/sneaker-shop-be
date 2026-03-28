package com.sneakershop.backend.entity.voucher;

import com.sneakershop.backend.entity.customer.Customer;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "voucher_usage",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"voucher_id", "customer_id"})
        }
)
@Data
public class VoucherUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔥 LAZY để tránh load dư
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "so_tien_giam", nullable = false)
    private Double discountAmount;

    @Column(name = "su_dung_luc", nullable = false)
    private LocalDateTime usedAt;
}