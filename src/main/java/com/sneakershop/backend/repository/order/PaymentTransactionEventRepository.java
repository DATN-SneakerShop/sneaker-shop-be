package com.sneakershop.backend.repository.order;

import com.sneakershop.backend.entity.order.PaymentTransactionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionEventRepository extends JpaRepository<PaymentTransactionEvent, Long> {
    Optional<PaymentTransactionEvent> findByProviderAndProviderTransactionId(String provider, String providerTransactionId);
    List<PaymentTransactionEvent> findByOrder_IdOrderByReceivedAtAscCreatedAtAsc(Long orderId);
}
