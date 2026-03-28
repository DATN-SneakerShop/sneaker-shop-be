package com.sneakershop.backend.repository.pricing;
import com.sneakershop.backend.entity.pricing.VariantPriceGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
public interface VariantPriceGroupRepository extends JpaRepository<VariantPriceGroup, Long> {

    Optional<VariantPriceGroup> findByVariant_IdAndLoaiKhach(
            Long variantId,
            String loaiKhach
    );
    List<VariantPriceGroup> findAllByVariant_Id(Long variantId);
}