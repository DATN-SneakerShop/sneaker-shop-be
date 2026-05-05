package com.sneakershop.backend.repository.order;

import com.sneakershop.backend.entity.order.PaymentTransaction;
import com.sneakershop.backend.entity.order.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findTopByOrder_IdAndTransactionTypeOrderByCreatedAtDesc(Long orderId, TransactionType transactionType);
    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);
    Optional<PaymentTransaction> findByProviderAndProviderTransactionId(String provider, String providerTransactionId);
    List<PaymentTransaction> findAllByOrder_IdAndTransactionTypeOrderByCreatedAtAsc(Long orderId, TransactionType transactionType);
}
