package com.sneakershop.backend.service.promotion;

import com.sneakershop.backend.dto.promotion.*;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.entity.promotion.Promotion;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import com.sneakershop.backend.repository.promotion.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final ProductVariantRepository variantRepository;


    // ================= GET =================

    public List<PromotionDTO> getAll() {
        return promotionRepository.findAll()
                .stream()
                .map(PromotionDTO::fromEntity)
                .toList();
    }

    public PromotionDTO getDetail(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Promotion not found"));

        return PromotionDTO.fromEntity(promotion);
    }

    public List<PromotionDTO> getActivePromotions(Long variantId) {
        return promotionRepository
                .findAllActivePromotionsByVariant(variantId, LocalDateTime.now())
                .stream()
                .map(PromotionDTO::fromEntity)
                .toList();
    }

    // ================= CREATE =================

    public PromotionDTO create(CreatePromotionRequest request) {

        validateTime(request.getStartTime(), request.getEndTime());

        String name = request.getName().trim();

        if (promotionRepository.existsByNameIgnoreCase(name)) {
            throw new RuntimeException("Tên khuyến mãi đã tồn tại");
        }

        Promotion promotion = new Promotion();
        promotion.setCode(generateCode());

        request.setName(name);
        mapFromRequest(promotion, request);

        return PromotionDTO.fromEntity(
                promotionRepository.save(promotion)
        );
    }

    // ================= UPDATE =================

    public PromotionDTO update(Long id, UpdatePromotionRequest request) {

        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Promotion not found"));

        validateTime(request.getStartTime(), request.getEndTime());

        mapFromRequest(promotion, request);

        return PromotionDTO.fromEntity(
                promotionRepository.save(promotion)
        );
    }

    // ================= TOGGLE =================

    public void toggleActive(Long id, Boolean active) {

        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("giảm giá không tồn "));

        // Nếu đã hết hạn thì không cho bật
        if (promotion.getEndTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Giảm giá đã hết hạn");
        }

        promotion.setActive(active);
        promotionRepository.save(promotion);
    }

    // ================= DELETE =================

    public void delete(Long id) {

        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Promotion not found"));

        promotion.setDeleted(true);   // đánh dấu đã xóa
        promotion.setActive(false);   // tắt luôn promotion

        promotionRepository.save(promotion);
    }

    // ================= PRIVATE =================

    private void mapFromRequest(Promotion promotion, BasePromotionRequest r) {

        if (r.getName() == null || r.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên khuyến mãi không được để trống");
        }

        if (r.getDiscountType() == null) {
            throw new RuntimeException("Thiếu loại giảm giá");
        }

        if (r.getDiscountValue() == null) {
            throw new RuntimeException("Thiếu giá trị giảm giá");
        }

        if (r.getDiscountValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Giá trị giảm giá không hợp lệ");
        }

        // ===== VALIDATE THEO TYPE =====
        switch (r.getDiscountType()) {

            case PERCENT:
                if (r.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new RuntimeException("Phần trăm không được vượt quá 100%");
                }
                break;

            case AMOUNT:
                if (r.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("Giảm tiền phải lớn hơn 0");
                }
                break;

            case BUY_2_GET_1:
                if (r.getDiscountValue().compareTo(BigDecimal.ZERO) != 0) {
                    throw new RuntimeException("BUY_2_GET_1 không cần discountValue");
                }
                break;
        }

        if (r.getPriority() != null && r.getPriority() < 0) {
            throw new RuntimeException("Priority không được âm");
        }

        validateTime(r.getStartTime(), r.getEndTime());

        promotion.setName(r.getName().trim());
        promotion.setDiscountType(r.getDiscountType());
        promotion.setDiscountValue(r.getDiscountValue());
        promotion.setStartTime(r.getStartTime());
        promotion.setEndTime(r.getEndTime());
        promotion.setActive(r.getActive() != null ? r.getActive() : true);
        promotion.setPriority(r.getPriority() != null ? r.getPriority() : 0);

        promotion.getVariants().clear();

        if (r.getVariantIds() != null && !r.getVariantIds().isEmpty()) {

            List<ProductVariant> variants =
                    variantRepository.findAllById(r.getVariantIds());

            if (variants.size() != r.getVariantIds().size()) {
                throw new RuntimeException("Có sản phẩm không tồn tại");
            }

            promotion.getVariants().addAll(variants);
        }
    }

    private void validateTime(LocalDateTime start, LocalDateTime end) {

        if (start == null || end == null) {
            throw new RuntimeException("Thiếu thời gian");
        }

        if (!start.isBefore(end)) {
            throw new RuntimeException("Thời gian bắt đầu phải trước kết thúc");
        }

        if (end.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Không thể tạo khuyến mãi trong quá khứ");
        }
    }

    private String generateCode() {
        Integer max = promotionRepository.findMaxCodeNumber();
        int next = (max == null ? 1000 : max + 1);
        return "DG" + next;
    }
}