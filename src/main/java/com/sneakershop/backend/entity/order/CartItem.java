package com.sneakershop.backend.entity.order;

import com.sneakershop.backend.entity.product.ProductVariant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_cart_variant", columnNames = {"cart_id", "variant_id"}),
        indexes = {
                @Index(name = "idx_cart_items_cart", columnList = "cart_id"),
                @Index(name = "idx_cart_items_variant", columnList = "variant_id")
        })
@Data
@ToString(exclude = {"cart", "variant"})
@EqualsAndHashCode(exclude = {"cart", "variant"})
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "selected", nullable = false)
    private Boolean selected = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.quantity == null || this.quantity < 1) this.quantity = 1;
        if (this.selected == null) this.selected = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.quantity == null || this.quantity < 1) this.quantity = 1;
        if (this.selected == null) this.selected = true;
    }
}
