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
    private String username;

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
    private String errorMessage;

    // 🔥 MỚI THÊM: Phân loại mức độ nghiêm trọng (INFO, WARNING, ERROR, DANGER)
    @Column(length = 20)
    private String logLevel = "INFO";

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}