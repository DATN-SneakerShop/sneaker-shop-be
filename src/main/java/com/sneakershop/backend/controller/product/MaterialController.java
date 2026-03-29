package com.sneakershop.backend.controller.product;

import com.sneakershop.backend.dto.product.MaterialResponse;
import com.sneakershop.backend.service.product.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {
    private final MaterialService materialService;

    @GetMapping
    public List<MaterialResponse> getAll() {
        return materialService.getAll();
    }
}