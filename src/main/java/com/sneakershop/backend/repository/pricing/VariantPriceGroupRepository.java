package com.sneakershop.backend.repository.pricing;
import com.sneakershop.backend.entity.pricing.VariantPriceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface VariantPriceGroupRepository extends JpaRepository<VariantPriceGroup, Long> {

    Optional<VariantPriceGroup> findByVariant_IdAndLoaiKhach(
            Long variantId,
            String loaiKhach
    );
}