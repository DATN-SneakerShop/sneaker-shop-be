package com.sneakershop.backend.controller.order;

import com.sneakershop.backend.dto.order.*;
import com.sneakershop.backend.entity.order.enums.*;
import com.sneakershop.backend.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;

import static org.springframework.format.annotation.DateTimeFormat.ISO;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ================= CREATE =================
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SALES')")
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }

    // ================= LIST + FILTER + PAGINATION =================
    @GetMapping
    public Page<OrderResponse> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,

            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) SalesChannel channel,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) ReturnStatus returnStatus,

            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long createdById,

            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime toDate
    ) {
        return orderService.search(
                keyword,
                status,
                channel,
                paymentStatus,
                returnStatus,
                customerId,
                createdById,
                fromDate,
                toDate,
                PageRequest.of(page, size)
        );
    }

    // ================= DETAIL =================
    @GetMapping("/{id}")
    public OrderResponse detail(@PathVariable Long id) {
        return orderService.getDetail(id);
    }

    // ================= UPDATE STATUS =================
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SALES')")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatus(id, request);
    }

    // ================= UPDATE PAYMENT =================
    @PutMapping("/{id}/payment")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SALES')")
    public OrderResponse updatePayment(@PathVariable Long id, @Valid @RequestBody UpdatePaymentRequest request) {
        return orderService.updatePayment(id, request);
    }

    // ================= CANCEL =================
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SALES')")
    public OrderResponse cancel(@PathVariable Long id, @Valid @RequestBody CancelOrderRequest request) {
        return orderService.cancel(id, request);
    }

    // ================= RETURN =================
    @PostMapping("/{id}/return")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('SALES')")
    public OrderResponse returnOrder(@PathVariable Long id, @Valid @RequestBody ReturnOrderRequest request) {
        return orderService.returnOrder(id, request);
    }

    // ================= SOFT DELETE =================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void softDelete(@PathVariable Long id) {
        orderService.softDelete(id);
    }
}
