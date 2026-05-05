package com.sneakershop.backend.entity.order;

import com.sneakershop.backend.entity.order.enums.PaymentMethod;
import com.sneakershop.backend.entity.order.enums.TransactionStatus;
import com.sneakershop.backend.entity.order.enums.TransactionType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transaction", indexes = {
        @Index(name = "idx_payment_txn_order_created", columnList = "order_id, created_at"),
        @Index(name = "idx_payment_txn_status_created", columnList = "status, created_at"),
        @Index(name = "idx_payment_txn_provider_ref", columnList = "provider, provider_transaction_id")
})
@Data
@ToString(exclude = {"order"})
@EqualsAndHashCode(exclude = {"order"})
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType = TransactionType.PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "request_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestAmount = BigDecimal.ZERO;

    @Column(name = "actual_amount", precision = 15, scale = 2)
    private BigDecimal actualAmount;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode = "VND";

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "provider_transaction_id", length = 100)
    private String providerTransactionId;

    @Column(name = "provider_response_code", length = 50)
    private String providerResponseCode;

    @Column(name = "provider_message", length = 255)
    private String providerMessage;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.requestAmount == null) this.requestAmount = BigDecimal.ZERO;
        if (this.currencyCode == null || this.currencyCode.trim().isEmpty()) this.currencyCode = "VND";
    }
}
