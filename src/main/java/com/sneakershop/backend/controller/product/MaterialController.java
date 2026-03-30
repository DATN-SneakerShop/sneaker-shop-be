package com.sneakershop.backend.controller.product;

import com.sneakershop.backend.dto.product.MaterialRequest; // 🔥 Dùng MaterialRequest
import com.sneakershop.backend.dto.product.MaterialResponse;
import com.sneakershop.backend.service.product.MaterialService; // 🔥 Inject đúng MaterialService
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/materials")
@RequiredArgsConstructor
public class MaterialController {
    private final MaterialService materialService;

    @GetMapping
    public List<MaterialResponse> getAll() {
        return materialService.getAll();
    }

    @PostMapping
    public MaterialResponse create(@RequestBody MaterialRequest req) {
        return materialService.create(req);
    }

    @PutMapping("/{id}")
    public MaterialResponse update(@PathVariable Long id, @RequestBody MaterialRequest req) {
        return materialService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        materialService.delete(id);
    }
}