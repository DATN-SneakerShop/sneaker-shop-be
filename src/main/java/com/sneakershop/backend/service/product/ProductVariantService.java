package com.sneakershop.backend.service.product;

import com.sneakershop.backend.dto.product.VariantRequest;
import com.sneakershop.backend.entity.product.Product;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.repository.product.ProductRepository;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    public ProductVariant create(Long productId, VariantRequest request) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        ProductVariant v = new ProductVariant();
        v.setSku(request.getSku());
        v.setSize(request.getSize());
        v.setColorway(request.getColorway());
        v.setPrice(request.getPrice());
        v.setSalePrice(request.getSalePrice());
        v.setStock(request.getStock());
        v.setStatus(request.getStatus());
        v.setProduct(product);

        return variantRepository.save(v);
    }
}
