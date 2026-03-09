package com.sneakershop.backend.repository.pricing;

import com.sneakershop.backend.entity.pricing.PriceCampaignItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PriceCampaignItemRepository extends JpaRepository<PriceCampaignItem, Long> {
    Optional<PriceCampaignItem> findByCampaign_IdAndVariant_Id(Long campaignId, Long variantId);

    @Query("""
SELECT i
FROM PriceCampaignItem i
WHERE i.variant.id = :variantId
AND i.campaign.active = true
AND :now BETWEEN i.campaign.startTime AND i.campaign.endTime
ORDER BY i.campaign.startTime ASC
""")
    List<PriceCampaignItem> findActiveCampaignItems(
            Long variantId,
            LocalDateTime now
    );

    List<PriceCampaignItem> findByCampaignId(Long campaignId);
    List<PriceCampaignItem> findByVariant_Id(Long variantId);
}