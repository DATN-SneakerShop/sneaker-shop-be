package com.sneakershop.backend.controller.voucher;

import com.sneakershop.backend.dto.voucher.CustomerVoucherDTO;
import com.sneakershop.backend.dto.voucher.VoucherRequest;
import com.sneakershop.backend.dto.voucher.VoucherResponse;

import com.sneakershop.backend.entity.voucher.Voucher;
import com.sneakershop.backend.service.voucher.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    // 🔥 GET ALL
    @GetMapping
    public List<VoucherResponse> getAllVouchers() {
        return voucherService.getAllVouchers();
    }

    // 🔥 Filter theo status (optional)
    @GetMapping("/status")
    public List<VoucherResponse> getByStatus(@RequestParam String status) {
        return voucherService.getVouchersByStatus(status);
    }
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
    }
    @PostMapping("/{id}/customers")
    public void assignCustomers(
            @PathVariable Long id,
            @RequestBody List<Long> customerIds
    ) {
        voucherService.saveVoucherCustomers(id, customerIds);
    }

    @PostMapping
    public Voucher create(@RequestBody VoucherRequest dto) {
        return voucherService.createVoucher(dto);
    }
    // Trong VoucherController.java
    @GetMapping("/customers-list")
    public List<CustomerVoucherDTO> getCustomersForVoucher() {
        return voucherService.getAllCustomersForVoucher();
    }
    // 1. Hàm lấy chi tiết để đổ vào form Chỉnh sửa
    @GetMapping("/{id}")
    public VoucherResponse getById(@PathVariable Long id) {
        return voucherService.getVoucherById(id);
    }

    // 2. Hàm cập nhật Voucher (Giải quyết lỗi 405)
    @PutMapping("/{id}")
    public VoucherResponse update(@PathVariable Long id, @RequestBody VoucherRequest dto) {
        return voucherService.updateVoucher(id, dto);
    }
    // Thêm vào VoucherController.java
    @GetMapping("/{id}/customers")
    public List<CustomerVoucherDTO> getAssignedCustomers(@PathVariable Long id) {
        // API này giúp Frontend biết Voucher này đang thuộc về những khách hàng nào
        return voucherService.getCustomersByVoucherId(id);
    }
    // Thêm API này vào VoucherController.java
    @PutMapping("/{id}/status")
    public void toggleStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        String newStatus = body.get("status");
        voucherService.updateStatus(id, newStatus);
    }
    @GetMapping("/available")
    public List<Voucher> getAvailableVouchers(@RequestParam(required = false) Long customerId) {
        return voucherService.getAvailableVouchers(customerId);
    }


}