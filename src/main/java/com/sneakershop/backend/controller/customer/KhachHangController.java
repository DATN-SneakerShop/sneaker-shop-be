package com.sneakershop.backend.controller.customer;

import com.sneakershop.backend.entity.customer.KhachHang;
import com.sneakershop.backend.service.customer.KhachHangService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/khach-hang")
public class KhachHangController {

    private final KhachHangService service;

    public KhachHangController(KhachHangService service) {
        this.service = service;
    }

    // 1️⃣ Danh sách khách hàng
    @GetMapping
    public List<KhachHang> getAll() {
        return service.getAllActive();
    }

    // 2️⃣ Tạo khách hàng
    @PostMapping
    public KhachHang create(@RequestBody KhachHang kh) {
        return service.create(kh);
    }

    // 3️⃣ Sửa + phân loại
    @PutMapping("/{id}")
    public KhachHang update(@PathVariable Long id,
                            @RequestBody KhachHang kh) {
        return service.update(id, kh);
    }

    // 4️⃣ Xoá khách hàng
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
