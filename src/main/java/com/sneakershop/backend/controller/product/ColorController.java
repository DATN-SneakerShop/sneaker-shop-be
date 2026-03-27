package com.sneakershop.backend.controller.product;

import com.sneakershop.backend.dto.product.ColorRequest;
import com.sneakershop.backend.dto.product.ColorResponse;
import com.sneakershop.backend.service.product.ColorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/colors")
@RequiredArgsConstructor
public class ColorController {

    private final ColorService colorService;

    @PostMapping
    public ColorResponse create(@RequestBody ColorRequest request) {
        return colorService.create(request);
    }

    @GetMapping
    public List<ColorResponse> getAll() {
        return colorService.getAll();
    }

    @PutMapping("/{id}")
    public ColorResponse update(@PathVariable Long id, @RequestBody ColorRequest request) {
        return colorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        colorService.delete(id);
    }
}