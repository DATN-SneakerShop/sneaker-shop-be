package com.sneakershop.backend.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "nhat_ky_audit")
@Data
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module", length = 50)
    private String module;

    @Column(name = "hanh_dong", length = 50)
    private String action;

    @Column(name = "doi_tuong", length = 50)
    private String entityName;

    @Column(name = "doi_tuong_id")
    private Long entityId;

    @Column(name = "tom_tat", columnDefinition = "TEXT") // Cho phép lưu nội dung dài
    private String summary;

    @Column(name = "ip", length = 45)
    private String ipAddress;

    @Column(name = "tao_luc", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_thuc_hien_id", foreignKey = @ForeignKey(name = "FK_AUDIT_USER"))
    private User performedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}