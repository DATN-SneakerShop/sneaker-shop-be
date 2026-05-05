package com.sneakershop.backend.controller.order;

import com.sneakershop.backend.dto.order.CancelOrderRequest;
import com.sneakershop.backend.dto.order.OrderResponse;
import com.sneakershop.backend.dto.order.RefundRequest;
import com.sneakershop.backend.service.order.StorefrontOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class StorefrontOrderController {
    private final StorefrontOrderService orderService;

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderDetail(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<String> cancel(@PathVariable Long id, @Valid @RequestBody CancelOrderRequest request) {
        orderService.cancelOrder(id, request);
        return ResponseEntity.ok("Hủy đơn thành công");
    }

    @PostMapping("/{id}/delivered")
    public ResponseEntity<String> delivered(@PathVariable Long id) {
        orderService.markDelivered(id);
        return ResponseEntity.ok("Cập nhật giao hàng thành công");
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<String> refund(@PathVariable Long id, @Valid @RequestBody RefundRequest request) {
        orderService.refundOrder(id, request);
        return ResponseEntity.ok("Hoàn tiền thành công");
    }
}
