package com.sneakershop.backend.controller.product;

import com.sneakershop.backend.dto.product.SoleRequest; // 🔥 Dùng SoleRequest
import com.sneakershop.backend.dto.product.SoleResponse;
import com.sneakershop.backend.service.product.SoleService; // 🔥 Inject đúng SoleService
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/soles")
@RequiredArgsConstructor
public class SoleController {
    private final SoleService soleService;

    @GetMapping
    public List<SoleResponse> getAll() {
        return soleService.getAll();
    }

    @PostMapping
    public SoleResponse create(@RequestBody SoleRequest req) {
        return soleService.create(req);
    }

    @PutMapping("/{id}")
    public SoleResponse update(@PathVariable Long id, @RequestBody SoleRequest req) {
        return soleService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        soleService.delete(id);
    }
}