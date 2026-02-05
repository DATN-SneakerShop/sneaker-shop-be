package com.sneakershop.backend.entity.login;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "nguoi_dung")
@Data
@ToString(exclude = {"roles", "auditLogs"})
@EqualsAndHashCode(exclude = {"roles", "auditLogs"})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_dang_nhap", unique = true, length = 100)
    private String username;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "mat_khau_hash", nullable = false)
    @JsonIgnore
    private String passwordHash;

    @Column(name = "ho_ten", length = 100)
    private String fullName;

    @Column(name = "trang_thai", length = 20)
    private String status = "ACTIVE";

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "loai_dang_nhap", length = 20)
    private String loaiDangNhap = "LOCAL";

    // Trường mới cho OTP
    @Column(name = "otp_code", length = 6)
    private String otpCode;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "nguoi_dung_vai_tro",
            joinColumns = @JoinColumn(name = "nguoi_dung_id"),
            inverseJoinColumns = @JoinColumn(name = "vai_tro_id")
    )
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "performedBy", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<AuditLog> auditLogs = new HashSet<>();
}