package com.sneakershop.backend.controller.order;

import com.sneakershop.backend.dto.order.CancelOrderRequest;
import com.sneakershop.backend.dto.order.ReturnOrderRequest;
import com.sneakershop.backend.dto.order.UpdateOrderStatusRequest;
import com.sneakershop.backend.dto.order.admin.AdminOrderDetailDTO;
import com.sneakershop.backend.dto.order.admin.CounterPaymentQrResponse;
import com.sneakershop.backend.dto.order.admin.AdminOrderSummaryDTO;
import com.sneakershop.backend.dto.order.admin.AdminOrderUpdateRequest;
import com.sneakershop.backend.entity.order.enums.OrderStatus;
import com.sneakershop.backend.entity.order.enums.PaymentMethod;
import com.sneakershop.backend.entity.order.enums.PaymentStatus;
import com.sneakershop.backend.entity.order.enums.ShippingStatus;
import com.sneakershop.backend.service.order.AdminOrderManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/orders")
public class AdminOrderManagementController {

    private final AdminOrderManagementService adminOrderManagementService;

    @GetMapping
    public ResponseEntity<List<AdminOrderSummaryDTO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) OrderStatus orderStatus,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) ShippingStatus shippingStatus,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return ResponseEntity.ok(adminOrderManagementService.list(
                keyword,
                orderStatus,
                paymentStatus,
                shippingStatus,
                paymentMethod,
                dateFrom,
                dateTo
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminOrderDetailDTO> detail(@PathVariable Long id) {
        return ResponseEntity.ok(adminOrderManagementService.detail(id));
    }


    @GetMapping("/{id}/counter-payment-qr")
    public ResponseEntity<CounterPaymentQrResponse> getCounterPaymentQr(@PathVariable Long id) {
        return ResponseEntity.ok(adminOrderManagementService.getCounterPaymentQr(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminOrderDetailDTO> updateMeta(@PathVariable Long id, @RequestBody AdminOrderUpdateRequest request) {
        return ResponseEntity.ok(adminOrderManagementService.updateMeta(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminOrderDetailDTO> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(adminOrderManagementService.updateOrderStatus(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AdminOrderDetailDTO> cancel(@PathVariable Long id, @Valid @RequestBody CancelOrderRequest request) {
        return ResponseEntity.ok(adminOrderManagementService.cancel(id, request));
    }

    @PostMapping("/{id}/returns")
    public ResponseEntity<AdminOrderDetailDTO> applyReturn(@PathVariable Long id, @Valid @RequestBody ReturnOrderRequest request) {
        return ResponseEntity.ok(adminOrderManagementService.applyReturn(id, request));
    }
}
