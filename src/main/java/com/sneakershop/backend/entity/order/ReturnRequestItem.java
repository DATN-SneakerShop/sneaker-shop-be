package com.sneakershop.backend.entity.order;

import com.sneakershop.backend.entity.order.enums.ReturnConditionStatus;
import com.sneakershop.backend.entity.product.ProductVariant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "return_request_items", indexes = {
        @Index(name = "idx_return_item_request", columnList = "return_request_id"),
        @Index(name = "idx_return_item_order_item", columnList = "order_item_id"),
        @Index(name = "idx_return_item_variant", columnList = "variant_id")
})
@Data
@ToString(exclude = {"returnRequest", "orderItem", "variant", "inspections"})
@EqualsAndHashCode(exclude = {"returnRequest", "orderItem", "variant", "inspections"})
public class ReturnRequestItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_request_id", nullable = false)
    private ReturnRequest returnRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(name = "received_quantity", nullable = false)
    private Integer receivedQuantity = 0;

    @Column(name = "restock_quantity", nullable = false)
    private Integer restockQuantity = 0;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "refund_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_status", length = 30)
    private ReturnConditionStatus conditionStatus;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "returnItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReturnRequestItemInspection> inspections = new ArrayList<>();
}
