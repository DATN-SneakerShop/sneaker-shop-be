package com.sneakershop.backend.controller.product;

import com.sneakershop.backend.dto.product.SoleResponse;
import com.sneakershop.backend.service.product.SoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/soles")
@RequiredArgsConstructor
public class SoleController {
    private final SoleService soleService;

    @GetMapping
    public List<SoleResponse> getAll() {
        return soleService.getAll();
    }
}