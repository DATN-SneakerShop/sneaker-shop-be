package com.sneakershop.backend.controller.promotion;

import com.sneakershop.backend.dto.promotion.CreatePromotionRequest;
import com.sneakershop.backend.dto.promotion.UpdatePromotionRequest;
import com.sneakershop.backend.dto.promotion.PromotionDTO;
import com.sneakershop.backend.service.promotion.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/admin/promotions")
@RequiredArgsConstructor
public class PromotionAdminController {

    private final PromotionService promotionService;

    /**
     * ✅ Danh sách đợt giảm giá
     */
    @GetMapping
    public List<PromotionDTO> getAll() {
        return promotionService.getAll();
    }

    /**
     * ✅ Chi tiết đợt giảm giá
     */
    @GetMapping("/{id}")
    public PromotionDTO getDetail(@PathVariable Long id) {
        return promotionService.getDetail(id);
    }

    /**
     * ✅ Tạo mới đợt giảm giá
     */
    @PostMapping
    public PromotionDTO create(@RequestBody CreatePromotionRequest request) {
        return promotionService.create(request);
    }

    /**
     * ✅ Cập nhật đợt giảm giá
     */
    @PutMapping("/{id}")
    public PromotionDTO update(
            @PathVariable Long id,
            @RequestBody UpdatePromotionRequest request
    ) {
        return promotionService.update(id, request);
    }

    /**
     * ✅ Bật / tắt khuyến mãi
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> togglePromotion(
            @PathVariable Long id,
            @RequestParam Boolean active
    ) {
        promotionService.toggleActive(id, active);
        return ResponseEntity.ok().build();
    }

    /**
     * ✅ Xóa khuyến mãi
     */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        promotionService.delete(id);
        return "Delete success";
    }
}