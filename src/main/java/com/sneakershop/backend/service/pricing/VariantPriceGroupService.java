package com.sneakershop.backend.service.pricing;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.pricing.GroupPriceDTO;
import com.sneakershop.backend.dto.pricing.PriceGroupResponse;
import com.sneakershop.backend.dto.promotion.PromotionDTO;
import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.entity.pricing.VariantPriceGroup;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.repository.pricing.ProductPriceRepository;
import com.sneakershop.backend.repository.pricing.VariantPriceGroupRepository;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VariantPriceGroupService {

    private final VariantPriceGroupRepository variantPriceGroupRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductPriceRepository productPriceRepository;
    private final PricingCalculationService pricingCalculationService;

    @Transactional
    @AuditAction(module = "PRICING", action = "CREATE", entity = "VariantPriceGroup",
            description = "Thiết lập giá nhóm khách #{#loaiKhach} cho variant ID #{#variantId}")
    public VariantPriceGroup savePriceGroup(Long variantId, String loaiKhach, BigDecimal price) {

        VariantPriceGroup pg = variantPriceGroupRepository
                .findByVariant_IdAndLoaiKhach(variantId, loaiKhach)
                .orElseGet(() -> {

                    ProductVariant variant =
                            productVariantRepository.findById(variantId)
                                    .orElseThrow(() -> new RuntimeException("Variant not found"));

                    VariantPriceGroup newGroup = new VariantPriceGroup();
                    newGroup.setVariant(variant);
                    newGroup.setLoaiKhach(loaiKhach);
                    return newGroup;
                });

        pg.setPrice(price);
        return variantPriceGroupRepository.save(pg);
    }

    /**
     * Lấy giá theo nhóm khách
     */
    public BigDecimal getPriceByCustomerType(Long variantId, String loaiKhach) {

        Optional<VariantPriceGroup> groupPrice =
                variantPriceGroupRepository
                        .findByVariant_IdAndLoaiKhach(variantId, loaiKhach);

        if (groupPrice.isPresent()) {
            return groupPrice.get().getPrice();
        }

        return productPriceRepository
                .findActivePrice(variantId)
                .map(ProductPrice::getPrice)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giá mặc định"));
    }

    @Transactional
    @AuditAction(module = "PRICING", action = "UPDATE", entity = "VariantPriceGroup",
            description = "Cập nhật giá nhóm khách #{#loaiKhach} cho variant ID #{#variantId}")
    public VariantPriceGroup updatePriceGroup(Long variantId, String loaiKhach, BigDecimal newPrice) {

        VariantPriceGroup existing =
                variantPriceGroupRepository
                        .findByVariant_IdAndLoaiKhach(variantId, loaiKhach)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy bảng giá"));

        existing.setPrice(newPrice);

        return variantPriceGroupRepository.save(existing);
    }

    public List<PriceGroupResponse> getAll() {
        List<ProductVariant> variants = productVariantRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<VariantPriceGroup> allGroups = variantPriceGroupRepository.findAll();

        Map<Long, List<VariantPriceGroup>> groupMap = allGroups.stream()
                .collect(Collectors.groupingBy(g -> g.getVariant().getId()));

        return variants.stream().map(variant -> {
            try {
                if (variant.getProduct() == null || Boolean.TRUE.equals(variant.getProduct().getDeleted())) {
                    return null;
                }

                // Lấy giá niêm yết (Ví dụ: 1.000.000)
                BigDecimal basePrice = productPriceRepository
                        .findActivePrice(variant.getId())
                        .map(ProductPrice::getPrice)
                        .orElse(variant.getPrice());

                List<String> customerTypes = List.of("VIP", "THUONG");

                List<GroupPriceDTO> groups = customerTypes.stream()
                        .map(type -> {
                            VariantPriceGroup group = groupMap.getOrDefault(variant.getId(), List.of())
                                    .stream()
                                    .filter(g -> g.getLoaiKhach().equalsIgnoreCase(type))
                                    .findFirst()
                                    .orElse(null);

                            // Đây là số tiền giảm (300k)
                            BigDecimal groupDiscountAmount = (group != null) ? group.getPrice() : BigDecimal.ZERO;

                            // CỘT 1: Giá sau giảm nhóm (1.000.000 - 300.000 = 700.000)
                            BigDecimal priceAfterGroup = basePrice.subtract(groupDiscountAmount);

                            // CỘT 2: Giá cuối cùng sau KM (Tính ra 350.000)
                            BigDecimal finalPrice = pricingCalculationService.calculateFinalPrice(variant.getId(), type);

                            return new GroupPriceDTO(
                                    type,
                                    priceAfterGroup, // Giá màu đen dòng trên
                                    finalPrice,      // Giá màu đỏ dòng dưới
                                    null,
                                    null
                            );
                        })
                        .toList();

                List<PromotionDTO> promotions = variant.getPromotions()
                        .stream()
                        .map(p -> {
                            PromotionDTO dto = new PromotionDTO();

                            dto.setId(p.getId());
                            dto.setName(p.getName());
                            dto.setCode(p.getCode());
                            dto.setDiscountType(p.getDiscountType());
                            dto.setDiscountValue(p.getDiscountValue());
                            dto.setPriority(p.getPriority());
                            dto.setStartTime(p.getStartTime());
                            dto.setEndTime(p.getEndTime());
                            dto.setActive(p.getActive());

                            return dto;
                        })
                        .toList();

                return new PriceGroupResponse(
                        variant.getId(),
                        variant.getProduct().getName(),
                        variant.getProduct().getSku(),
                        variant.getSku(),
                        variant.getColorway(),
                        variant.getSize(),
                        variant.getProduct().getThumbnail(),
                        variant.getProduct().getBrand(),
                        variant.getProduct().getGender(),
                        variant.getProduct().getMaterial(),
                        variant.getProduct().getModel(),
                        variant.getProduct().getReleaseYear(),
                        variant.getProduct().getDescription(),
                        basePrice,
                        groups,
                        promotions
                );
            } catch (EntityNotFoundException e) {
                // 🔥 Bắt chặt lỗi Proxy của Hibernate văng ra nếu Product bị giấu
                return null;
            }
        }).filter(Objects::nonNull).toList(); // 🔥 Lọc sạch các phần tử null ra khỏi danh sách
    }
}