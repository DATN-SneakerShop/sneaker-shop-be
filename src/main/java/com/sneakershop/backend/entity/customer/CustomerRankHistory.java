package com.sneakershop.backend.entity.customer;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_rank_history")
@Data
public class CustomerRankHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "old_rank", length = 20)
    private String oldRank;

    @Column(name = "new_rank", nullable = false, length = 20)
    private String newRank;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        this.changedAt = LocalDateTime.now();
    }
}