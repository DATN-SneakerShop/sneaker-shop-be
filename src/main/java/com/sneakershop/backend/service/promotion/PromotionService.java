package com.sneakershop.backend.service.promotion;

import com.sneakershop.backend.dto.promotion.*;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.entity.promotion.Promotion;
import com.sneakershop.backend.repository.pricing.ProductPriceRepository;
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
    private final ProductPriceRepository productPriceRepository;

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

        PromotionDTO dto = PromotionDTO.fromEntity(promotion);

        List<PromotionVariantDTO> variants =
                promotion.getVariants().stream().map(v -> {

                    PromotionVariantDTO item = new PromotionVariantDTO();

                    item.setVariantId(v.getId());
                    item.setProductName(v.getProduct().getName());
                    item.setColor(v.getColorway());
                    item.setSize(Integer.valueOf(v.getSize()));
                    item.setStock(v.getStock());

                    // ===== LẤY GIÁ GỐC =====
                    BigDecimal price = productPriceRepository
                            .findActivePrice(v.getId())
                            .map(p -> p.getPrice())
                            .orElse(BigDecimal.ZERO);

                    item.setPrice(price);

                    // ===== TÍNH GIÁ SAU GIẢM =====
                    BigDecimal discounted = price;

                    switch (promotion.getDiscountType()) {

                        case PERCENT -> discounted =
                                price.subtract(
                                        price.multiply(promotion.getDiscountValue())
                                                .divide(BigDecimal.valueOf(100))
                                );

                        case AMOUNT -> discounted =
                                price.subtract(promotion.getDiscountValue());

                        case BUY_2_GET_1 -> discounted = price;
                    }

                    item.setDiscountedPrice(discounted);

                    // ===== IMAGE =====
                    if (!v.getProduct().getImages().isEmpty()) {
                        item.setImage(
                                v.getProduct()
                                        .getImages()
                                        .get(0)
                                        .getImageUrl()
                        );
                    }

                    return item;

                }).toList();

        dto.setVariants(variants);

        return dto;
    }

    // ================= CREATE =================
    public PromotionDTO create(CreatePromotionRequest request) {

        Promotion promotion = new Promotion();

        mapFromRequest(promotion, request);

        // set code tạm
        promotion.setCode("TEMP");

        Promotion saved = promotionRepository.save(promotion);

        saved.setCode("DG" + saved.getId());

        return PromotionDTO.fromEntity(promotionRepository.save(saved));
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
                .orElseThrow(() -> new EntityNotFoundException("Promotion not found"));

        // Không cho bật nếu đã hết hạn
        if (Boolean.TRUE.equals(active)
                && promotion.getEndTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Khuyến mãi đã hết hạn");
        }

        promotion.setActive(active);

        promotionRepository.save(promotion);
    }

    // ================= DELETE =================

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

            case PERCENT -> {
                if (r.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new RuntimeException("Phần trăm không được vượt quá 100%");
                }
            }

            case AMOUNT -> {
                if (r.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("Giảm tiền phải lớn hơn 0");
                }
            }

            case BUY_2_GET_1 -> {
                if (r.getDiscountValue().compareTo(BigDecimal.ZERO) != 0) {
                    throw new RuntimeException("BUY_2_GET_1 không cần discountValue");
                }
            }
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
    public boolean checkName(String name){
        return promotionRepository.existsByNameIgnoreCase(name);
    }
}