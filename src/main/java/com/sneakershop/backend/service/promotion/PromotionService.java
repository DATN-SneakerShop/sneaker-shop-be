package com.sneakershop.backend.service.promotion;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.promotion.*;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.entity.promotion.Promotion;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import com.sneakershop.backend.repository.promotion.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final ProductVariantRepository variantRepository;

    public List<PromotionDTO> getAll() {
        return promotionRepository.findAll().stream().map(PromotionDTO::fromEntity).toList();
    }

    public PromotionDTO getDetail(Long id) {
        Promotion p = promotionRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Not found"));
        return PromotionDTO.fromEntity(p);
    }

    // 🔥 CAMERA CHO TẠO MỚI (Gộp chung vào nhóm PRICING)
    @AuditAction(module = "PRICING", action = "CREATE", entity = "Promotion",
            description = "Tạo đợt giảm giá mới: #{#request.name} | Loại: #{#request.discountType} | Mức giảm: #{#request.discountValue}")
    public PromotionDTO create(CreatePromotionRequest request) {
        Promotion p = new Promotion();
        p.setCode("DG" + System.currentTimeMillis());
        mapFromRequest(p, request);
        return PromotionDTO.fromEntity(promotionRepository.save(p));
    }

    // 🔥 CAMERA CHO CẬP NHẬT
    @AuditAction(module = "PRICING", action = "UPDATE", entity = "Promotion",
            description = "Cập nhật đợt giảm giá ID #{#id} | Tên mới: #{#request.name} | Loại: #{#request.discountType}")
    public PromotionDTO update(Long id, UpdatePromotionRequest request) {
        Promotion p = promotionRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Not found"));
        mapFromRequest(p, request);
        return PromotionDTO.fromEntity(promotionRepository.save(p));
    }

    // 🔥 CAMERA CHO VIỆC BẬT/TẮT TRẠNG THÁI
    @AuditAction(module = "PRICING", action = "UPDATE_STATUS", entity = "Promotion",
            description = "Thay đổi trạng thái giảm giá ID #{#id} thành: #{#active ? 'Hoạt động' : 'Tạm ngưng'}")
    public void toggleActive(Long id, Boolean active) {
        Promotion p = promotionRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Not found"));
        if (Boolean.TRUE.equals(active) && p.getEndTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Khuyến mãi hết hạn");
        }
        p.setActive(active);
        promotionRepository.save(p);
    }

    // 🔥 CAMERA CHO XÓA
    @AuditAction(module = "PRICING", action = "DELETE", entity = "Promotion",
            description = "Đã xóa đợt giảm giá ID #{#id} khỏi hệ thống")
    public void delete(Long id) {
        Promotion p = promotionRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Not found"));
        p.setDeleted(true);
        promotionRepository.save(p);
    }

    private void mapFromRequest(Promotion p, BasePromotionRequest r) {
        p.setName(r.getName().trim());
        p.setDiscountType(r.getDiscountType());
        p.setDiscountValue(r.getDiscountValue());
        p.setStartTime(r.getStartTime());
        p.setEndTime(r.getEndTime());
        if (r.getVariantIds() != null) {
            List<ProductVariant> variants = variantRepository.findAllById(r.getVariantIds());
            p.getVariants().clear(); // Fix lỗi trùng lặp khi update
            p.getVariants().addAll(variants);
        }
    }
}