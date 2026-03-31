package com.sneakershop.backend.service.promotion;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.promotion.*;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.entity.promotion.DiscountType;
import com.sneakershop.backend.entity.promotion.Promotion;
import com.sneakershop.backend.entity.promotion.PromotionDetail;
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

        // DTO đã tự động lấy giá chuẩn và map dữ liệu, không cần viết logic phức tạp ở đây nữa
        return PromotionDTO.fromEntity(promotion);
    }

    // ================= CREATE =================

    @AuditAction(module = "PRICING", action = "CREATE", entity = "Promotion",
            description = "Tạo đợt giảm giá mới: #{#request.name}")
    public PromotionDTO create(CreatePromotionRequest request) {

        Promotion promotion = new Promotion();
        mapFromRequest(promotion, request);
        promotion.setCode("TEMP");

        Promotion saved = promotionRepository.save(promotion);
        saved.setCode("DG" + saved.getId());

        return PromotionDTO.fromEntity(promotionRepository.save(saved));
    }

    // ================= UPDATE =================

    @AuditAction(module = "PRICING", action = "UPDATE", entity = "Promotion",
            description = "Cập nhật đợt giảm giá ID #{#id} | Tên mới: #{#request.name}")
    public PromotionDTO update(Long id, UpdatePromotionRequest request) {

        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Promotion not found"));

        validateTime(request.getStartTime(), request.getEndTime());
        mapFromRequest(promotion, request);

        return PromotionDTO.fromEntity(promotionRepository.save(promotion));
    }

    // ================= TOGGLE =================

    @AuditAction(module = "PRICING", action = "UPDATE_STATUS", entity = "Promotion",
            description = "Thay đổi trạng thái giảm giá ID #{#id} thành: #{#active ? 'Hoạt động' : 'Tạm ngưng'}")
    public void toggleActive(Long id, Boolean active) {

        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Promotion not found"));

        if (Boolean.TRUE.equals(active)
                && promotion.getEndTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Khuyến mãi đã hết hạn");
        }

        promotion.setActive(active);
        promotionRepository.save(promotion);
    }

    // ================= DELETE =================

    @AuditAction(module = "PRICING", action = "DELETE", entity = "Promotion",
            description = "Đã xóa đợt giảm giá ID #{#id} khỏi hệ thống")
    public void delete(Long id) {

        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Promotion not found"));

        promotion.setDeleted(true);
        promotion.setActive(false);
        promotionRepository.save(promotion);
    }

    // ================= PRIVATE =================

    private void mapFromRequest(Promotion promotion, BasePromotionRequest r) {

        if (r.getName() == null || r.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên khuyến mãi không được để trống");
        }

        validateTime(r.getStartTime(), r.getEndTime());

        promotion.setName(r.getName().trim());
        promotion.setStartTime(r.getStartTime());
        promotion.setEndTime(r.getEndTime());
        promotion.setActive(r.getActive() != null ? r.getActive() : true);
        promotion.setPriority(r.getPriority() != null ? r.getPriority() : 0);

        if (promotion.getPromotionDetails() != null) {
            promotion.getPromotionDetails().clear();
        } else {
            promotion.setPromotionDetails(new java.util.ArrayList<>());
        }

        if (r.getDetails() != null && !r.getDetails().isEmpty()) {
            for (PromotionDetailRequest detailReq : r.getDetails()) {

                validateDiscountParams(detailReq.getDiscountType(), detailReq.getDiscountValue());

                ProductVariant variant = variantRepository.findById(detailReq.getVariantId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Variant ID: " + detailReq.getVariantId()));

                PromotionDetail detail = new PromotionDetail();
                detail.setPromotion(promotion);
                detail.setVariant(variant);
                detail.setDiscountType(detailReq.getDiscountType());
                detail.setDiscountValue(detailReq.getDiscountValue());

                promotion.getPromotionDetails().add(detail);
            }
        }
    }

    private void validateDiscountParams(DiscountType type, BigDecimal value) {
        if (type == null || value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Thông số giảm giá không hợp lệ");
        }
        switch (type) {
            case PERCENT -> {
                if (value.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new RuntimeException("Phần trăm không được vượt quá 100%");
                }
            }
            case AMOUNT -> {
                if (value.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("Giảm tiền phải lớn hơn 0");
                }
            }
            case BUY_2_GET_1 -> {
                if (value.compareTo(BigDecimal.ZERO) != 0) {
                    throw new RuntimeException("BUY_2_GET_1 không cần nhập giá trị");
                }
            }
        }
    }

    private void validateTime(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new RuntimeException("Thiếu thời gian");
        }
        if (!start.isBefore(end)) {
            throw new RuntimeException("Thời gian bắt đầu phải trước kết thúc");
        }
    }

    public boolean checkName(String name){
        return promotionRepository.existsByNameIgnoreCase(name);
    }

}