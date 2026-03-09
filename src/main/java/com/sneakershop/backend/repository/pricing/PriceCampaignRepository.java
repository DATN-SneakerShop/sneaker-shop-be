package com.sneakershop.backend.repository.pricing;

import com.sneakershop.backend.entity.pricing.PriceCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PriceCampaignRepository extends JpaRepository<PriceCampaign, Long> {
    @Query("""
SELECT c
FROM PriceCampaign c
WHERE c.active = true
AND :now BETWEEN c.startTime AND c.endTime
""")
    List<PriceCampaign> findActiveCampaign(LocalDateTime now);


}