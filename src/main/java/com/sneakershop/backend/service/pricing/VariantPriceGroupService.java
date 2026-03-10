package com.sneakershop.backend.service.pricing;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.pricing.GroupPriceDTO;
import com.sneakershop.backend.dto.pricing.PriceGroupResponse;
import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.entity.pricing.VariantPriceGroup;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.repository.pricing.ProductPriceRepository;
import com.sneakershop.backend.repository.pricing.VariantPriceGroupRepository;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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

    // ✅ FIX: image_8af5c4.png - Sửa kiểu trả về từ void thành VariantPriceGroup
    @Transactional
    @AuditAction(module = "PRICING", action = "CREATE", entity = "VariantPriceGroup",
            description = "Thiết lập giá nhóm khách #{#loaiKhach} cho variant ID #{#variantId}")
    public VariantPriceGroup savePriceGroup(Long variantId, String loaiKhach, BigDecimal price) {
        VariantPriceGroup pg = variantPriceGroupRepository.findByVariantIdAndLoaiKhach(variantId, loaiKhach)
                .orElseGet(() -> {
                    VariantPriceGroup n = new VariantPriceGroup();
                    n.setVariant(productVariantRepository.findById(variantId).get());
                    n.setLoaiKhach(loaiKhach);
                    return n;
                });
        pg.setPrice(price);
        return variantPriceGroupRepository.save(pg);
    }

    // ✅ FIX: image_8c56c0.png - Hàm lấy giá theo nhóm khách hàng
    public BigDecimal getPriceByCustomerType(Long variantId, String loaiKhach) {
        Optional<VariantPriceGroup> groupPrice = variantPriceGroupRepository.findByVariantIdAndLoaiKhach(variantId, loaiKhach);
        if (groupPrice.isPresent()) {
            return groupPrice.get().getPrice();
        }
        return productPriceRepository.findActivePrice(variantId)
                .map(ProductPrice::getPrice)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giá mặc định đang active"));
    }

    // ✅ FIX: image_8b050d.jpg - Sửa kiểu trả về thành VariantPriceGroup
    @Transactional
    @AuditAction(module = "PRICING", action = "UPDATE", entity = "VariantPriceGroup",
            description = "Cập nhật giá nhóm khách #{#loaiKhach} cho variant ID #{#variantId}")
    public VariantPriceGroup updatePriceGroup(Long variantId, String loaiKhach, BigDecimal price) {
        VariantPriceGroup existing = variantPriceGroupRepository.findByVariantIdAndLoaiKhach(variantId, loaiKhach)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bảng giá"));
        existing.setPrice(price);
        return variantPriceGroupRepository.save(existing);
    }

    public List<PriceGroupResponse> getAll() {
        // 🔥 SẮP XẾP: Lấy giày mới nhất lên đầu bảng Quản lý giá theo nhóm
        List<ProductVariant> variants = productVariantRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        List<VariantPriceGroup> allGroups = variantPriceGroupRepository.findAll();
        Map<Long, List<VariantPriceGroup>> groupMap = allGroups.stream()
                .collect(Collectors.groupingBy(g -> g.getVariant().getId()));

        return variants.stream().map(variant -> {
            BigDecimal basePrice = productPriceRepository.findActivePrice(variant.getId())
                    .map(ProductPrice::getPrice).orElse(variant.getPrice());

            List<GroupPriceDTO> groups = groupMap.getOrDefault(variant.getId(), List.of())
                    .stream().map(g -> new GroupPriceDTO(g.getLoaiKhach(), g.getPrice())).toList();

            return new PriceGroupResponse(variant.getId(), variant.getProduct().getName(), variant.getSku(),
                    variant.getColorway(), variant.getSize(), basePrice, groups);
        }).toList();
    }
}