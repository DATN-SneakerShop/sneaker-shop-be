package com.sneakershop.backend.repository.pricing;


import com.sneakershop.backend.entity.pricing.TienTe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TienTeRepository extends JpaRepository<TienTe, Long> {
    boolean existsByMa(String ma);
    Optional<TienTe> findByLaMacDinhTrue();
}
