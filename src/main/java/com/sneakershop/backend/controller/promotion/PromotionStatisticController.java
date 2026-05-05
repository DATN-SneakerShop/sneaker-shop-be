package com.sneakershop.backend.controller.promotion;

import com.sneakershop.backend.service.promotion.PromotionStatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/promotion-dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PromotionStatisticController {

    private final PromotionStatisticService statisticService;

    @GetMapping
    public ResponseEntity<?> getDashboard() {
        return ResponseEntity.ok(statisticService.getDashboardData());
    }
}