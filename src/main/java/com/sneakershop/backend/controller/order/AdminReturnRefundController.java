package com.sneakershop.backend.controller.order;

import com.sneakershop.backend.dto.order.returning.*;
import com.sneakershop.backend.entity.order.enums.ReturnRequestStatus;
import com.sneakershop.backend.service.order.ReturnRefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/returns")
public class AdminReturnRefundController {

    private final ReturnRefundService returnRefundService;

    @GetMapping
    public ResponseEntity<List<ReturnRefundResponse>> list(@RequestParam(required = false) ReturnRequestStatus status) {
        return ResponseEntity.ok(returnRefundService.listAdmin(status));
    }

    @PostMapping
    public ResponseEntity<ReturnRefundResponse> createByAdmin(@Valid @RequestBody CreateReturnRefundRequest request) {
        return new ResponseEntity<>(returnRefundService.createByAdmin(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReturnRefundResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(returnRefundService.detailAdmin(id));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ReturnRefundResponse> approve(@PathVariable Long id, @RequestBody AdminApproveReturnRequest request) {
        return ResponseEntity.ok(returnRefundService.approve(id, request));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ReturnRefundResponse> reject(@PathVariable Long id, @RequestBody AdminRejectReturnRequest request) {
        return ResponseEntity.ok(returnRefundService.reject(id, request));
    }

    @PutMapping("/{id}/receive")
    public ResponseEntity<ReturnRefundResponse> receive(@PathVariable Long id, @Valid @RequestBody AdminReceiveReturnRequest request) {
        return ResponseEntity.ok(returnRefundService.receive(id, request));
    }

    @PutMapping("/{id}/refund")
    public ResponseEntity<ReturnRefundResponse> refund(@PathVariable Long id, @RequestBody AdminRefundReturnRequest request) {
        return ResponseEntity.ok(returnRefundService.refund(id, request));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ReturnRefundResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(returnRefundService.complete(id));
    }
}
