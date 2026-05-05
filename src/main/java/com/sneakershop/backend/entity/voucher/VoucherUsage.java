package com.sneakershop.backend.entity.voucher;

import com.sneakershop.backend.entity.customer.Customer;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "voucher_usage", indexes = {
        @Index(name = "idx_voucher_usage_customer", columnList = "voucher_id, customer_id"),
        @Index(name = "idx_voucher_usage_guest_email", columnList = "voucher_id, guest_email"),
        @Index(name = "idx_voucher_usage_guest_phone", columnList = "voucher_id, guest_phone"),
        @Index(name = "idx_voucher_usage_used_at", columnList = "su_dung_luc")
})
@Data
public class VoucherUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "guest_email", length = 150)
    private String guestEmail;

    @Column(name = "guest_phone", length = 20)
    private String guestPhone;

    @Column(name = "usage_type", length = 20)
    private String usageType;

    @Column(name = "voucher_code_snapshot", length = 50)
    private String voucherCodeSnapshot;

    @Column(name = "so_tien_giam", nullable = false)
    private Double discountAmount;

    @Column(name = "su_dung_luc", nullable = false)
    private LocalDateTime usedAt;
}
