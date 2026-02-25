package com.sneakershop.backend.repository.pricing;

import com.sneakershop.backend.entity.pricing.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    // 🔍 Kiểm tra trùng mã tiền tệ (VND, USD…)
    boolean existsByCode(String code);

    // ⭐ Lấy tiền tệ mặc định
    Optional<Currency> findByIsDefaultTrue();


    // 🔥 Bỏ default cũ (chỉ nên gọi khi set default mới)
    @Modifying
    @Query("UPDATE Currency c SET c.isDefault = false WHERE c.isDefault = true")
    void clearDefault();
}