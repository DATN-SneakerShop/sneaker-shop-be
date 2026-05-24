package com.sneakershop.backend.entity.inventory;

import com.sneakershop.backend.entity.product.ProductVariant;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "stock_transaction")
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(nullable = false, length = 60)
    private String type;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "before_stock", nullable = false)
    private int beforeStock;

    @Column(name = "after_stock", nullable = false)
    private int afterStock;

    @Column(name = "before_reserved_quantity", nullable = false)
    private int beforeReservedQuantity;

    @Column(name = "after_reserved_quantity", nullable = false)
    private int afterReservedQuantity;

    @Column(name = "reference_type", length = 60)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
