package com.sneakershop.backend.entity.customer;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_audit_log")
@Data
public class CustomerAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;          // CREATE, UPDATE, DELETE, VIP, POINT

    private String entityName;      // "Khách hàng"

    @Column(columnDefinition = "TEXT")
    private String summary;         // mô tả chi tiết

    private String performedBy;     // admin

    private String ipAddress;

    private LocalDateTime timestamp;

    private Long customerId;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}

