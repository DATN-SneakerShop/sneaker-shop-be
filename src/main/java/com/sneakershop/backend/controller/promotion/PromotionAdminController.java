package com.sneakershop.backend.controller.promotion;

import com.sneakershop.backend.dto.promotion.*;
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

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntime(RuntimeException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @GetMapping
    public List<PromotionDTO> getAll() {
        return promotionService.getAll();
    }

    @GetMapping("/{id}")
    public PromotionDTO getDetail(@PathVariable Long id) {
        return promotionService.getDetail(id);
    }

    @PostMapping
    public PromotionDTO create(@RequestBody CreatePromotionRequest request) {
        return promotionService.create(request);
    }

    @PutMapping("/{id}")
    public PromotionDTO update(@PathVariable Long id, @RequestBody UpdatePromotionRequest request) {
        return promotionService.update(id, request);
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> togglePromotion(@PathVariable Long id, @RequestParam Boolean active) {
        promotionService.toggleActive(id, active);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        promotionService.delete(id);
        return ResponseEntity.ok("Success");
    }
}