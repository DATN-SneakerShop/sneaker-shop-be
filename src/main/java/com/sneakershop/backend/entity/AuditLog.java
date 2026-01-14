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

    @Column(name = "module")
    private String module;

    @Column(name = "hanh_dong")
    private String action;

    @Column(name = "doi_tuong")
    private String entityName;

    @Column(name = "doi_tuong_id")
    private Long entityId;

    @Column(name = "tom_tat")
    private String summary;

    @Column(name = "ip")
    private String ipAddress;

    @Column(name = "tao_luc")
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "nguoi_thuc_hien_id") // Khớp đúng Database của bạn
    private User performedBy;
}