package com.sneakershop.backend.repository.order;

import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByOrderCode(String orderCode);

    @Query("""
        select o from Order o
        left join fetch o.items i
        left join fetch i.variant v
        left join fetch v.product p
        left join fetch o.customer c
        left join fetch o.createdBy u
        where o.id = :id
    """)
    Optional<Order> findDetailById(@Param("id") Long id);

    @Query("""
        select o from Order o
        left join o.customer c
        left join o.createdBy u
        where (:keyword is null or :keyword = '' 
                or lower(o.orderCode) like lower(concat('%', :keyword, '%'))
                or (c is not null and (lower(c.ten) like lower(concat('%', :keyword, '%'))
                                       or lower(c.email) like lower(concat('%', :keyword, '%'))
                                       or lower(c.soDienThoai) like lower(concat('%', :keyword, '%'))))
            )
          and (:status is null or o.orderStatus = :status)
          and (:channel is null or o.channel = :channel)
          and (:paymentStatus is null or o.paymentStatus = :paymentStatus)
          and (:returnStatus is null or o.returnStatus = :returnStatus)
          and (:customerId is null or (c is not null and c.id = :customerId))
          and (:createdById is null or (u is not null and u.id = :createdById))
          and (:fromDate is null or o.createdAt >= :fromDate)
          and (:toDate is null or o.createdAt <= :toDate)
    """)
    Page<Order> search(
            @Param("keyword") String keyword,
            @Param("status") OrderStatus status,
            @Param("channel") SalesChannel channel,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("returnStatus") ReturnStatus returnStatus,
            @Param("customerId") Long customerId,
            @Param("createdById") Long createdById,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}
