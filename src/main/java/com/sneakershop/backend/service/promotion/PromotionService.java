package com.sneakershop.backend.service.promotion;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.promotion.*;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.exception.ValidationException;
import com.sneakershop.backend.service.ValidationSupport;
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
import java.util.HashSet;
import java.util.Set;

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
        validatePromotionRequest(request, null);
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

        validatePromotionRequest(request, id);
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

    private void validatePromotionRequest(BasePromotionRequest r, Long currentId) {
        String name = ValidationSupport.trim(r.getName());
        if (name == null) throw new ValidationException("name", "Tên khuyến mãi không được để trống");
        boolean dupName = currentId == null ? promotionRepository.existsByNameNormalized(name) : promotionRepository.existsByNameNormalizedAndIdNot(name, currentId);
        if (dupName) throw new ValidationException("name", "Tên khuyến mãi đã tồn tại.");
        validateTime(r.getStartTime(), r.getEndTime());

        Set<Long> variantIds = new HashSet<>();
        if (r.getDetails() != null) {
            for (PromotionDetailRequest d : r.getDetails()) {
                if (d.getVariantId() == null) throw new ValidationException("variantId", "Biến thể không được để trống.");
                if (!variantIds.add(d.getVariantId())) {
                    throw new ValidationException("variantId", "Biến thể này bị trùng trong danh sách khuyến mãi.");
                }
                Long promotionId = currentId == null ? -1L : currentId;
                if (promotionRepository.existsActiveOverlapForVariant(d.getVariantId(), r.getStartTime(), r.getEndTime(), promotionId)) {
                    throw new ValidationException("variantId", "Biến thể này đã có khuyến mãi trong khoảng thời gian đã chọn.");
                }
            }
        }
    }

    private void mapFromRequest(Promotion promotion, BasePromotionRequest r) {

        if (r.getName() == null || r.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên khuyến mãi không được để trống");
        }

        validateTime(r.getStartTime(), r.getEndTime());

        promotion.setName(ValidationSupport.trim(r.getName()));
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
                if (detailReq.getDiscountType() == DiscountType.AMOUNT && variant.getPrice() != null
                        && detailReq.getDiscountValue().compareTo(variant.getPrice()) > 0) {
                    throw new ValidationException("discountValue", "Giá trị giảm không được vượt quá giá sản phẩm.");
                }

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
            throw new ValidationException("discountValue", "Giá trị giảm không hợp lệ.");
        }
        switch (type) {
            case PERCENT -> {
                if (value.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new ValidationException("discountValue", "Phần trăm giảm giá không được vượt quá 100.");
                }
            }
            case AMOUNT -> {
                if (value.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ValidationException("discountValue", "Giá trị giảm không hợp lệ.");
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
            throw new ValidationException("startTime", "Thời gian bắt đầu phải trước thời gian kết thúc.");
        }
        if (!start.isBefore(end)) {
            throw new ValidationException("startTime", "Thời gian bắt đầu phải trước thời gian kết thúc.");
        }
    }

    public boolean checkName(String name){
        return promotionRepository.existsByNameIgnoreCase(name);
    }

}