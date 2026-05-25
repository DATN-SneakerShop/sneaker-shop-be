package com.sneakershop.backend.entity.order;

import com.sneakershop.backend.entity.order.enums.ReturnConditionStatus;
import com.sneakershop.backend.entity.order.enums.ReturnDispositionType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_inventory_dispositions", indexes = {
        @Index(name = "idx_return_disposition_request", columnList = "return_request_id"),
        @Index(name = "idx_return_disposition_item", columnList = "return_item_id"),
        @Index(name = "idx_return_disposition_variant", columnList = "variant_id"),
        @Index(name = "idx_return_disposition_type", columnList = "disposition_type")
})
@Data
@ToString(exclude = {"returnRequest"})
@EqualsAndHashCode(exclude = {"returnRequest"})
public class ReturnInventoryDisposition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    /**
     * Lưu ID dạng scalar để tránh lỗi DevTools/ClassLoader khi Hibernate scan entity trong lúc IntelliJ đang rebuild.
     * Quan hệ nghiệp vụ vẫn được đảm bảo bằng FK ở database/script SQL, nhưng entity này không bắt buộc load
     * ReturnRequestItem/ReturnRequestItemInspection/ProductVariant khi khởi động.
     */
    @Column(name = "return_item_id", nullable = false)
    private Long returnItemId;

    @Column(name = "inspection_id")
    private Long inspectionId;

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_status", nullable = false, length = 30)
    private ReturnConditionStatus conditionStatus = ReturnConditionStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposition_type", nullable = false, length = 40)
    private ReturnDispositionType dispositionType = ReturnDispositionType.NOT_RESELLABLE_HOLD;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(name = "restock_quantity", nullable = false)
    private Integer restockQuantity = 0;

    @Column(name = "non_resellable_quantity", nullable = false)
    private Integer nonResellableQuantity = 0;

    @Column(name = "responsibility", length = 40)
    private String responsibility;

    @Column(name = "warehouse_location", length = 120)
    private String warehouseLocation;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
