package com.sneakershop.backend.entity.order;

import lombok.Data;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transaction_event", indexes = {
        @Index(name = "idx_pt_event_order_created", columnList = "order_id, created_at"),
        @Index(name = "idx_pt_event_payment_created", columnList = "payment_transaction_id, created_at"),
        @Index(name = "idx_pt_event_provider_tx", columnList = "provider, provider_transaction_id", unique = true)
})
@Where(clause = "deleted = false")
@Data
public class PaymentTransactionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_transaction_id")
    private PaymentTransaction paymentTransaction;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_transaction_id", nullable = false, length = 100)
    private String providerTransactionId;

    @Column(name = "provider_response_code", length = 100)
    private String providerResponseCode;

    @Column(name = "gateway", length = 50)
    private String gateway;

    @Column(name = "transfer_type", length = 20)
    private String transferType;

    @Column(name = "transfer_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal transferAmount = BigDecimal.ZERO;

    @Column(name = "running_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal runningAmount = BigDecimal.ZERO;

    @Column(name = "expected_amount", precision = 15, scale = 2)
    private BigDecimal expectedAmount = BigDecimal.ZERO;

    @Column(name = "difference_amount", precision = 15, scale = 2)
    private BigDecimal differenceAmount = BigDecimal.ZERO;

    @Column(name = "payment_code", length = 100)
    private String paymentCode;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "sub_account", length = 50)
    private String subAccount;

    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "processing_note", columnDefinition = "TEXT")
    private String processingNote;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "raw_payload", columnDefinition = "LONGTEXT")
    private String rawPayload;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.receivedAt == null) {
            this.receivedAt = this.transactionDate != null ? this.transactionDate : this.createdAt;
        }
        if (this.transferAmount == null) this.transferAmount = BigDecimal.ZERO;
        if (this.runningAmount == null) this.runningAmount = BigDecimal.ZERO;
        if (this.expectedAmount == null) this.expectedAmount = BigDecimal.ZERO;
        if (this.differenceAmount == null) this.differenceAmount = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
