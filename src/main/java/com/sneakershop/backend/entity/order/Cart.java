package com.sneakershop.backend.entity.order;

import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.order.enums.CartStatus;
import com.sneakershop.backend.entity.order.enums.SalesChannel;
import com.sneakershop.backend.entity.voucher.Voucher;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cart", indexes = {
        @Index(name = "idx_cart_customer_status", columnList = "customer_id, status"),
        @Index(name = "idx_cart_session_status", columnList = "session_key, status"),
        @Index(name = "idx_cart_updated_at", columnList = "updated_at")
})
@Where(clause = "deleted = false")
@Data
@ToString(exclude = {"items", "customer", "voucher", "freeShipVoucher"})
@EqualsAndHashCode(exclude = {"items", "customer", "voucher", "freeShipVoucher"})
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "session_key", length = 100)
    private String sessionKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private SalesChannel channel = SalesChannel.ONLINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CartStatus status = CartStatus.ACTIVE;

    @Column(name = "guest_email", length = 150)
    private String guestEmail;

    @Column(name = "guest_phone", length = 20)
    private String guestPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "free_ship_voucher_id")
    private Voucher freeShipVoucher;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
