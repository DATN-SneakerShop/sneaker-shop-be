package com.sneakershop.backend.service.pricing;

import com.sneakershop.backend.dto.pricing.*;
import com.sneakershop.backend.entity.pricing.PriceCampaign;
import com.sneakershop.backend.entity.pricing.PriceCampaignItem;
import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.repository.pricing.PriceCampaignItemRepository;
import com.sneakershop.backend.repository.pricing.PriceCampaignRepository;
import com.sneakershop.backend.repository.pricing.ProductPriceRepository;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PriceCampaignService {

    private final PriceCampaignRepository campaignRepository;
    private final PriceCampaignItemRepository itemRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductPriceRepository productPriceRepository;

    /* ================= CREATE CAMPAIGN ================= */

    public CampaignResponse createCampaign(CreateCampaignRequest request) {

        PriceCampaign campaign = new PriceCampaign();

        campaign.setName(request.getName());
        campaign.setStartTime(request.getStartTime());
        campaign.setEndTime(request.getEndTime());
        campaign.setActive(request.getActive() != null ? request.getActive() : true);

        PriceCampaign saved = campaignRepository.save(campaign);

        if (request.getItems() != null) {

            for (CampaignItemDTO dto : request.getItems()) {

                ProductVariant variant = variantRepository.findById(dto.getVariantId())
                        .orElseThrow(() -> new EntityNotFoundException("Variant not found"));

                PriceCampaignItem item = new PriceCampaignItem();

                item.setCampaign(saved);
                item.setVariant(variant);
                item.setPrice(dto.getPrice());

                itemRepository.save(item);
            }

        }

        return mapResponse(saved);
    }

    /* ================= ADD VARIANT PRICE ================= */

    public void addVariantPrice(Long campaignId, Long variantId, BigDecimal price) {

        PriceCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found"));

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Variant not found"));

        PriceCampaignItem item = itemRepository
                .findByCampaign_IdAndVariant_Id(campaignId, variantId)
                .orElse(new PriceCampaignItem());

        item.setCampaign(campaign);
        item.setVariant(variant);
        item.setPrice(price);

        itemRepository.save(item);
    }

    /* ================= GET ACTIVE CAMPAIGN ================= */

    public List<PriceCampaign> getActiveCampaign() {
        return campaignRepository.findActiveCampaign(LocalDateTime.now());
    }

    /* ================= RESPONSE ================= */

    private CampaignResponse mapResponse(PriceCampaign campaign) {

        CampaignResponse res = new CampaignResponse();

        res.setId(campaign.getId());
        res.setName(campaign.getName());
        res.setStartTime(campaign.getStartTime());
        res.setEndTime(campaign.getEndTime());
        res.setActive(campaign.getActive());

        return res;
    }

    public CampaignResponse getCampaignDetail(Long id) {

        PriceCampaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        CampaignResponse res = new CampaignResponse();

        res.setId(campaign.getId());
        res.setName(campaign.getName());
        res.setStartTime(campaign.getStartTime());
        res.setEndTime(campaign.getEndTime());

        List<CampaignItemDTO> items =
                campaign.getItems().stream().map(i -> {

                    CampaignItemDTO dto = new CampaignItemDTO();

                    dto.setVariantId(i.getVariant().getId());
                    dto.setSku(i.getVariant().getSku());
                    dto.setColorway(i.getVariant().getColorway());
                    dto.setSize(i.getVariant().getSize());

                    dto.setPrice(i.getPrice());

                    dto.setOriginalPrice(
                            productPriceRepository
                                    .findFirstByVariantIdAndEndDateIsNull(i.getVariant().getId())
                                    .map(ProductPrice::getPrice)
                                    .orElse(BigDecimal.ZERO)
                    );

                    dto.setImage(i.getVariant().getProduct().getThumbnail());
                    dto.setProductName(i.getVariant().getProduct().getName());

                    return dto;

                }).toList();

        res.setItems(items);

        return res;
    }
    @Transactional
    public CampaignResponse updateCampaign(Long id, UpdateCampaignRequest request) {

        PriceCampaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Campaign not found"));

        campaign.setName(request.getName());
        campaign.setStartTime(request.getStartTime());
        campaign.setEndTime(request.getEndTime());
        campaign.setActive(
                request.getActive() != null ? request.getActive() : true
        );
        campaignRepository.save(campaign);

        List<PriceCampaignItem> existing = itemRepository.findByCampaignId(id);

        for (PriceCampaignItem item : existing) {

            boolean stillExists = request.getItems().stream()
                    .anyMatch(i -> i.getVariantId().equals(item.getVariant().getId()));

            if (!stillExists) {
                itemRepository.delete(item);
            }
        }

        for (CampaignItemDTO dto : request.getItems()) {

            ProductVariant variant = variantRepository.findById(dto.getVariantId())
                    .orElseThrow(() -> new EntityNotFoundException("Variant not found"));

            PriceCampaignItem item = itemRepository
                    .findByCampaign_IdAndVariant_Id(id, dto.getVariantId())
                    .orElse(new PriceCampaignItem());

            item.setCampaign(campaign);
            item.setVariant(variant);
            item.setPrice(dto.getPrice());

            itemRepository.save(item);
        }

        return mapResponse(campaign);
    }
    public List<PriceCampaign> getAllCampaign() {
        return campaignRepository.findAll();
    }

    public BigDecimal findLowestCampaignPrice(Long variantId) {

        List<PriceCampaignItem> items =
                itemRepository.findByVariant_Id(variantId);

        if(items == null || items.isEmpty()){
            return null;
        }

        return items.stream()
                .map(PriceCampaignItem::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }
    public BigDecimal getFinalPrice(Long variantId) {

        LocalDateTime now = LocalDateTime.now();

        List<PriceCampaignItem> items =
                itemRepository.findActiveCampaignItems(variantId, now);

        if(!items.isEmpty()){
            return items.get(0).getPrice();
        }

        return productPriceRepository
                .findFirstByVariantIdAndEndDateIsNull(variantId)
                .map(ProductPrice::getPrice)
                .orElse(BigDecimal.ZERO);
    }

}