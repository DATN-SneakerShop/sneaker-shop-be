package com.sneakershop.backend.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "nguoi_dung")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_dang_nhap", unique = true, nullable = false)
    private String username;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "mat_khau_hash", nullable = false)
    private String passwordHash;

    @Column(name = "ho_ten")
    private String fullName;

    @Column(name = "trang_thai")
    private String status = "ACTIVE";

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "loai_dang_nhap")
    private String loaiDangNhap = "LOCAL";

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "nguoi_dung_vai_tro",
            joinColumns = @JoinColumn(name = "nguoi_dung_id"),
            inverseJoinColumns = @JoinColumn(name = "vai_tro_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Column(name = "tao_luc")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "performedBy", cascade = CascadeType.PERSIST)
    @JsonIgnore
    private Set<AuditLog> auditLogs = new HashSet<>();
}