package com.sneakershop.backend.repository.pricing;
import com.sneakershop.backend.entity.pricing.VariantPriceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface VariantPriceGroupRepository extends JpaRepository<VariantPriceGroup, Long> {

    Optional<VariantPriceGroup> findByVariantIdAndLoaiKhach(
            Long variantId,
            String loaiKhach
    );
}