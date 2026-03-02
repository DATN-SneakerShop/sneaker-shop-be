package com.sneakershop.backend.service.product;

import com.sneakershop.backend.dto.product.VariantRequest;
import com.sneakershop.backend.entity.product.Product;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.repository.product.ProductRepository;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    /* ================== CREATE ================== */
    @Transactional
    public ProductVariant create(Long productId, VariantRequest request) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Product not found: " + productId)
                );

        // ✅ validate trùng variant trong product
        if (variantRepository.existsByProduct_IdAndSizeAndSizeTypeAndColorway(
                productId,
                request.getSize(),
                request.getSizeType(),
                request.getColorway()
        )) {
            throw new IllegalArgumentException(
                    "Variant already exists (size + sizeType + colorway)"
            );
        }

        ProductVariant v = new ProductVariant();
        v.setProduct(product);
        v.setSize(request.getSize());
        v.setSizeType(request.getSizeType());
        v.setColorway(request.getColorway());
        v.setStock(request.getStock());
        v.setPrice(request.getPrice());
        v.setSalePrice(request.getSalePrice());
        v.setStatus(
                request.getStock() > 0 ? "Còn_Hàng" : "Hết_Hàng"
        );
        // ✅ SKU generate / validate
        if (request.getSku() != null) {
            if (variantRepository.existsBySku(request.getSku())) {
                throw new IllegalArgumentException(
                        "Variant SKU already exists: " + request.getSku()
                );
            }
            v.setSku(request.getSku());
        }
        return variantRepository.save(v);

    }

    /* ================== UPDATE ================== */
    @Transactional
    public ProductVariant update(
            Long productId,
            Long variantId,
            VariantRequest request
    ) {

        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Variant not found: " + variantId)
                );

        // ✅ bảo vệ: variant phải thuộc product
        if (!v.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException(
                    "Variant does not belong to product: " + productId
            );
        }

        v.setStock(request.getStock());
        v.setStatus(
                request.getStock() > 0 ? "Còn Hàng" : "Hết Hàng"
        );

        return variantRepository.save(v);
    }

    /* ================== DELETE ================== */
    @Transactional
    public void delete(Long productId, Long variantId) {

        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Variant not found: " + variantId)
                );

        if (!v.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException(
                    "Variant does not belong to product: " + productId
            );
        }

        variantRepository.delete(v);
    }
}
