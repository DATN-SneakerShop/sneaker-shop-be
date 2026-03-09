package com.sneakershop.backend.controller.product;

import com.sneakershop.backend.dto.product.*;
import com.sneakershop.backend.entity.product.Product;
import com.sneakershop.backend.entity.product.ProductTag;
import com.sneakershop.backend.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import com.sneakershop.backend.repository.product.ProductTagRepository;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductTagRepository productTagRepository;
    @GetMapping("/promotion/{id}")
    public Page<Product> getProductsByPromotion(
            @PathVariable Long id,
            Pageable pageable
    ) {
        return productService.getProductsByPromotion(id, pageable);
    }
    @GetMapping("/best-selling")
    public Page<ProductResponse> getBestSellingProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return productService.getBestSellingProducts(
                PageRequest.of(page, size)
        );
    }
    /* ✅ FIX LỖI 500: Chuyển IllegalArgumentException thành mã lỗi 400 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleLogicError(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @PostMapping
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @GetMapping
    public Page<ProductResponse> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.getProducts(PageRequest.of(page, size));
    }

    @GetMapping("/search")
    public Page<ProductResponse> searchProducts(

            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size

    ) {

        ProductSearchRequest request = new ProductSearchRequest();

        request.setCategoryIds(categoryIds);
        request.setKeyword(keyword);
        request.setSortPrice(sortPrice);
        request.setSort(sort);

        return productService.searchAdvanced(
                request,
                PageRequest.of(page, size)
        );

    }
    @GetMapping("/{id}")
    public ProductDetailResponse getDetail(@PathVariable Long id) {
        return productService.getById(id);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
    @PostMapping("/{productId}/tags/{tagId}")
    public void addTag(
            @PathVariable Long productId,
            @PathVariable Long tagId
    ) {
        productService.addTagToProduct(productId, tagId);
    }
    @PutMapping("/{productId}/tags")
    public void updateTags(
            @PathVariable Long productId,
            @RequestBody UpdateProductTagsRequest request
    ) {
        productService.updateProductTags(productId, request.getTagIds());
    }
    @DeleteMapping("/{productId}/tags/{tagId}")
    public void removeTag(
            @PathVariable Long productId,
            @PathVariable Long tagId
    ) {
        productService.removeTagFromProduct(productId, tagId);
    }
    @GetMapping("/tags")
    public List<ProductTag> getAllTags() {
        return productTagRepository.findAll();
    }
    @GetMapping("/tag/{tagName}")
    public Page<ProductResponse> getProductsByTag(
            @PathVariable String tagName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return productService.getProductsByTag(
                tagName,
                PageRequest.of(page, size)
        );
    }
    @GetMapping("/created-date")
    public Page<ProductResponse> getProductsByCreatedDate(
            @RequestParam String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        LocalDate createdDate = LocalDate.parse(date);

        return productService.getProductsByCreatedDate(
                createdDate,
                PageRequest.of(page, size)
        );
    }
    @GetMapping("/filter-by-date")
    public Page<ProductResponse> filterProductsByDate(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        return productService.filterProductsByDate(
                start,
                end,
                PageRequest.of(page, size)
        );
    }
    @PutMapping("/{id}/status")
    public ProductResponse updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        return productService.updateStatus(id, status);
    }
    @PutMapping("/batch-status")
    public ResponseEntity<?> batchUpdateStatus(
            @RequestBody BatchUpdateStatusRequest request
    ) {

        productService.batchUpdateStatus(
                request.getIds(),
                request.getStatus()
        );

        return ResponseEntity.ok("Batch update thành công");
    }
    @GetMapping("/updated")
    public ResponseEntity<?> getUpdatedProducts(

            @RequestParam int page,
            @RequestParam int size

    ) {

        return ResponseEntity.ok(
                productService.getUpdatedProducts(page, size)
        );

    }
    @DeleteMapping("/batch")
    public ResponseEntity<?> batchDelete(@RequestBody List<Long> ids) {

        productService.batchDelete(ids);

        return ResponseEntity.ok("Batch delete success");
    }
    @GetMapping("/{id}/history")
    public List<ProductHistoryResponse> getProductHistory(@PathVariable Long id) {

        return productService.getProductHistory(id);

    }
}