package com.sneakershop.backend.controller.product;

import com.sneakershop.backend.dto.product.ColorResponse;
import com.sneakershop.backend.dto.product.SizeResponse;
import com.sneakershop.backend.service.product.ColorService;
import com.sneakershop.backend.service.product.SizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/storefront")
@RequiredArgsConstructor
public class StorefrontFilterController {

    private final ColorService colorService;
    private final SizeService sizeService;

    @GetMapping("/colors")
    public List<ColorResponse> getColors() {
        return colorService.getAll();
    }

    @GetMapping("/sizes")
    public List<SizeResponse> getSizes() {
        return sizeService.getAll();
    }
}