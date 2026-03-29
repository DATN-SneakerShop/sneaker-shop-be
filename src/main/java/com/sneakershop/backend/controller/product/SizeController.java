package com.sneakershop.backend.controller.product;

import com.sneakershop.backend.dto.product.SizeResponse;
import com.sneakershop.backend.service.product.SizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sizes")
@RequiredArgsConstructor
public class SizeController {
    private final SizeService sizeService;

    @GetMapping
    public List<SizeResponse> getAll() {
        return sizeService.getAll();
    }
}