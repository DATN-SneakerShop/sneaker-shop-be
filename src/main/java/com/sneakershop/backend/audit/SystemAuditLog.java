package com.sneakershop.backend.audit;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_audit_log")
@Data
public class SystemAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String username; // Tên user thao tác (lưu text để không bị lỗi khóa ngoại)

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 50)
    private String module;

    @Column(length = 50)
    private String action;

    @Column(length = 50)
    private String entityName;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(length = 20)
    private String status; // SUCCESS hoặc FAILED

    @Column(columnDefinition = "TEXT")
    private String errorMessage; // Lưu nguyên nhân lỗi nếu có

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}