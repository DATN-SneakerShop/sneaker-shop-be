package com.sneakershop.backend.service.pricing;
import com.sneakershop.backend.dto.pricing.GroupPriceDTO;
import com.sneakershop.backend.dto.pricing.PriceGroupResponse;
import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.entity.pricing.VariantPriceGroup;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.repository.pricing.ProductPriceRepository;
import com.sneakershop.backend.repository.pricing.VariantPriceGroupRepository;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VariantPriceGroupService {

    private final VariantPriceGroupRepository variantPriceGroupRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductPriceRepository productPriceRepository;

    @Transactional
    public void savePriceGroup(Long variantId, String loaiKhach, BigDecimal price) {

        VariantPriceGroup priceGroup = variantPriceGroupRepository
                .findByVariantIdAndLoaiKhach(variantId, loaiKhach)
                .orElseGet(() -> {
                    ProductVariant variant = productVariantRepository.findById(variantId)
                            .orElseThrow(() -> new RuntimeException("Variant not found"));

                    VariantPriceGroup newGroup = new VariantPriceGroup();
                    newGroup.setVariant(variant);
                    newGroup.setLoaiKhach(loaiKhach);
                    return newGroup;
                });

        priceGroup.setPrice(price);

        variantPriceGroupRepository.save(priceGroup);
    }

    public BigDecimal getPriceByCustomerType(Long variantId, String loaiKhach) {

        // 1️⃣ Ưu tiên giá nhóm khách
        Optional<VariantPriceGroup> groupPrice =
                variantPriceGroupRepository
                        .findByVariantIdAndLoaiKhach(variantId, loaiKhach);

        if (groupPrice.isPresent()) {
            return groupPrice.get().getPrice();
        }

        // 2️⃣ Fallback về giá default active
        return productPriceRepository
                .findActivePrice(variantId)
                .map(ProductPrice::getPrice)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy giá mặc định đang active"));
    }

    @Transactional
    public VariantPriceGroup updatePriceGroup(
            Long variantId,
            String loaiKhach,
            BigDecimal newPrice) {

        VariantPriceGroup existing = variantPriceGroupRepository
                .findByVariantIdAndLoaiKhach(variantId, loaiKhach)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy bảng giá"));

        existing.setPrice(newPrice);

        return variantPriceGroupRepository.save(existing);
    }
    public List<PriceGroupResponse> getAll() {

        List<ProductVariant> variants = productVariantRepository.findAll();

        // load all group price 1 lần
        List<VariantPriceGroup> allGroups = variantPriceGroupRepository.findAll();

        // group theo variantId
        Map<Long, List<VariantPriceGroup>> groupMap =
                allGroups.stream()
                        .collect(Collectors.groupingBy(
                                g -> g.getVariant().getId()
                        ));

        return variants.stream().map(variant -> {

            BigDecimal basePrice = productPriceRepository
                    .findActivePrice(variant.getId())
                    .map(ProductPrice::getPrice)
                    .orElse(null);

            List<GroupPriceDTO> groups =
                    groupMap.getOrDefault(variant.getId(), List.of())
                            .stream()
                            .map(g -> new GroupPriceDTO(
                                    g.getLoaiKhach(),
                                    g.getPrice()))
                            .toList();

            return new PriceGroupResponse(
                    variant.getId(),
                    variant.getProduct().getName(),
                    variant.getSku(),
                    variant.getColorway(),
                    variant.getSize(),
                    basePrice,
                    groups
            );

        }).toList();
    }

}