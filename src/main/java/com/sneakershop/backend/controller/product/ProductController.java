package com.sneakershop.backend.controller.product;

import com.sneakershop.backend.dto.product.ProductDetailResponse;
import com.sneakershop.backend.dto.product.ProductRequest;
import com.sneakershop.backend.dto.product.ProductResponse;
import com.sneakershop.backend.dto.product.ProductSearchRequest;
import com.sneakershop.backend.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /* ================== CREATE ================== */
    @PostMapping
    public ProductResponse create(
            @Valid @RequestBody ProductRequest request
    ) {
        return productService.create(request);
    }

    /* ================== LIST DEFAULT ================== */
    @GetMapping
    public Page<ProductResponse> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return productService.getProducts(
                PageRequest.of(page, size)
        );
    }

    /* ================== SEARCH ADVANCED ================== */
    @GetMapping("/search")
    public Page<ProductResponse> searchProducts(

            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false) String keyword,

            @RequestParam(required = false) String sizeFilter,
            @RequestParam(required = false) String colorway,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String gender,

            // 🔥 THÊM DÒNG NÀY
            @RequestParam(required = false) String sortPrice,
            @RequestParam(required = false) String sort,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        ProductSearchRequest request = new ProductSearchRequest();
        request.setCategoryIds(categoryIds);
        request.setKeyword(keyword);
        request.setSize(sizeFilter);
        request.setColorway(colorway);
        request.setBrand(brand);
        request.setGender(gender);

        // 🔥 SET SORT
        request.setSortPrice(sortPrice);
        request.setSort(sort);

        return productService.searchAdvanced(
                request,
                PageRequest.of(page, size)
        );
    }

    /* ================== DETAIL (🔥 QUAN TRỌNG) ================== */
    @GetMapping("/{id}")
    public ProductDetailResponse getDetail(
            @PathVariable Long id
    ) {
        return productService.getById(id);
    }
    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return productService.update(id, request);
    }
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }

}
