package com.sneakershop.backend.controller.promotion;

import com.sneakershop.backend.service.promotion.PromotionReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/promotions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PromotionReportController {
    
    private final PromotionReportService reportService;

    @GetMapping("/active")
    public ResponseEntity<?> getReport() {
        return ResponseEntity.ok(reportService.getActivePromotionReport());
    }
}