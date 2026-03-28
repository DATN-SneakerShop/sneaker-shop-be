package com.sneakershop.backend.controller.pricing;

import com.sneakershop.backend.dto.pricing.PriceBoardDTO;
import com.sneakershop.backend.dto.pricing.PriceHistoryDTO;
import com.sneakershop.backend.dto.pricing.PriceRequest;
import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.service.pricing.ProductPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity; // Sử dụng ResponseEntity
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class ProductPriceController {

    private final ProductPriceService productPriceService;

    // ✅ Lấy bảng giá hiện tại (chỉ lấy những giá đang active và chưa xóa)
    @GetMapping("/board")
    public ResponseEntity<List<PriceBoardDTO>> getPriceBoard() {
        return ResponseEntity.ok(productPriceService.getPriceBoard());
    }

    // ✅ Lấy lịch sử giá (Sẽ tự động lọc bỏ các bản ghi is_deleted nhờ @Where ở Entity)
    @GetMapping("/variant/{variantId}")
    public ResponseEntity<List<PriceHistoryDTO>> getPricesByVariant(@PathVariable Long variantId) {
        return ResponseEntity.ok(productPriceService.getPriceHistoryByVariant(variantId));
    }

    // ✅ Cập nhật giá gốc mới
    // Đã có logic chặn giá gốc <= mức giảm nhóm ở Service
    @PostMapping("/variant/{variantId}")
    public ResponseEntity<?> createPrice(
            @PathVariable Long variantId,
            @RequestBody PriceRequest request
    ) {
        try {
            ProductPrice newPrice = productPriceService.updatePrice(variantId, request);
            return ResponseEntity.ok(newPrice);
        } catch (RuntimeException e) {
            // Trả về lỗi 400 kèm message "Giá gốc phải lớn hơn mức giảm..." để FE hiển thị
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ Xóa mềm giá (Soft Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePrice(@PathVariable Long id) {
        try {
            productPriceService.deletePrice(id);
            return ResponseEntity.ok("Đã xóa thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}