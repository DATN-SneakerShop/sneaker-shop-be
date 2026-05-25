package com.sneakershop.backend.entity.order;

import com.sneakershop.backend.entity.order.enums.ReturnConditionStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "return_request_item_inspections", indexes = {
        @Index(name = "idx_return_inspection_item", columnList = "return_item_id")
})
@Data
@ToString(exclude = {"returnItem"})
@EqualsAndHashCode(exclude = {"returnItem"})
public class ReturnRequestItemInspection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_item_id", nullable = false)
    private ReturnRequestItem returnItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_status", nullable = false, length = 30)
    private ReturnConditionStatus conditionStatus = ReturnConditionStatus.NEW;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(name = "restock_quantity", nullable = false)
    private Integer restockQuantity = 0;

    @Column(name = "refund_quantity", nullable = false)
    private Integer refundQuantity = 0;

    @Column(name = "refund_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal refundRate = BigDecimal.ZERO;

    @Column(name = "refund_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Column(name = "responsibility", length = 40)
    private String responsibility;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
