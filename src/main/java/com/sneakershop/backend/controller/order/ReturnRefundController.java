package com.sneakershop.backend.controller.order;

import com.sneakershop.backend.dto.order.returning.CreateReturnRefundRequest;
import com.sneakershop.backend.dto.order.returning.ReturnRefundResponse;
import com.sneakershop.backend.service.order.ReturnRefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/returns")
public class ReturnRefundController {

    private final ReturnRefundService returnRefundService;

    @PostMapping
    public ResponseEntity<ReturnRefundResponse> create(@Valid @RequestBody CreateReturnRefundRequest request) {
        return new ResponseEntity<>(returnRefundService.createByCustomer(request), HttpStatus.CREATED);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReturnRefundResponse>> myReturns() {
        return ResponseEntity.ok(returnRefundService.listMine());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReturnRefundResponse> detailMine(@PathVariable Long id) {
        return ResponseEntity.ok(returnRefundService.detailMine(id));
    }
}
