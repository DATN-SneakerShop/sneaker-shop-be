package com.sneakershop.backend.repository.order;

import com.sneakershop.backend.entity.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
