package com.sneakershop.backend.entity.login;

import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vai_tro")
@Data
@NoArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma", unique = true, nullable = false, length = 30)
    private String code;

    @Column(name = "ten", nullable = false, length = 50)
    private String name;

    @Column(name = "tao_luc", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Role(String code, String name) {
        this.code = code;
        this.name = name;
    }
}