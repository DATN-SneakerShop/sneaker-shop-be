package com.sneakershop.backend.controller.product;

import com.sneakershop.backend.service.product.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/product-images")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService imageService;

    // 📸 Upload ảnh (dùng cho FE preview)
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Map<String, String> upload(
            @RequestParam("file") MultipartFile file
    ) {
        String url = imageService.uploadTemp(file);
        return Map.of("url", url);
    }
}
