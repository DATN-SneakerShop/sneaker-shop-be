package com.sneakershop.backend.repository.order;

import com.sneakershop.backend.entity.order.Cart;
import com.sneakershop.backend.entity.order.enums.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByIdAndDeletedFalse(Long id);
    Optional<Cart> findByCustomer_IdAndStatusAndDeletedFalse(Long customerId, CartStatus status);
    Optional<Cart> findBySessionKeyAndStatusAndDeletedFalse(String sessionKey, CartStatus status);
}
