package com.sneakershop.backend.entity.customer;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_point_history")
@Data
public class CustomerPointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "old_point")
    private Integer oldPoint;

    @Column(name = "new_point")
    private Integer newPoint;

    @Column(name = "reason")
    private String reason;

    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;

    @Transient
    private String customerName;

    @PrePersist
    protected void onCreate() {
        this.changedAt = LocalDateTime.now();
    }
}
