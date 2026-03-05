package com.sneakershop.backend.repository.order;

import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.enums.OrderStatus;
import com.sneakershop.backend.entity.order.enums.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdAndDeletedFalse(Long id);
    List<Order> findAllByDeletedFalseOrderByCreatedAtDesc();
    List<Order> findAllByOrderStatusAndDeletedFalseOrderByCreatedAtDesc(OrderStatus status);

    // Checklist: hiển thị đơn theo khách hàng / theo nhân viên bán
    List<Order> findAllByCustomer_IdAndDeletedFalseOrderByCreatedAtDesc(Long customerId);
    List<Order> findAllByCreatedBy_IdAndDeletedFalseOrderByCreatedAtDesc(Long createdById);

    // Checklist: báo cáo đơn hoàn trả
    List<Order> findAllByReturnStatusIsNotNullAndDeletedFalseOrderByCreatedAtDesc();
    List<Order> findAllByReturnStatusAndDeletedFalseOrderByCreatedAtDesc(ReturnStatus returnStatus);
    
    boolean existsByOrderCode(String orderCode);
}
