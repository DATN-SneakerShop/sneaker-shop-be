package com.sneakershop.backend.controller.product;

import com.sneakershop.backend.dto.product.SizeRequest;
import com.sneakershop.backend.dto.product.SizeResponse;
import com.sneakershop.backend.service.product.SizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/sizes")
@RequiredArgsConstructor
public class SizeController {
    private final SizeService sizeService;

    @GetMapping
    public List<SizeResponse> getAll() {
        return sizeService.getAll();
    }

    @PostMapping
    public SizeResponse create(@RequestBody SizeRequest req) {
        return sizeService.create(req);
    }

    @PutMapping("/{id}")
    public SizeResponse update(@PathVariable Long id, @RequestBody SizeRequest req) {
        return sizeService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        sizeService.delete(id);
    }
}