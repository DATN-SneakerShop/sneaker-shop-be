package com.sneakershop.backend.controller.pricing;

import com.sneakershop.backend.dto.pricing.*;
import com.sneakershop.backend.entity.pricing.PriceCampaign;
import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.entity.product.Product;
import com.sneakershop.backend.repository.pricing.ProductPriceRepository;
import com.sneakershop.backend.repository.product.CategoryRepository;
import com.sneakershop.backend.repository.product.ProductRepository;
import com.sneakershop.backend.service.pricing.PriceCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/price-campaign")
@RequiredArgsConstructor
public class PriceCampaignController {

    private final PriceCampaignService campaignService;
    private final ProductRepository productRepository;
    private final ProductPriceRepository productPriceRepository;


    @GetMapping("/products-with-variants")
    public List<ProductCampaignDTO> getProductsWithVariants() {

        return productRepository.findAll().stream().map(product -> {

            ProductCampaignDTO dto = new ProductCampaignDTO();

            dto.setId(product.getId());
            dto.setName(product.getName());
            dto.setBrand(product.getBrand());
            dto.setThumbnail(product.getThumbnail());

            List<VariantCampaignDTO> variants =
                    product.getVariants().stream().map(v -> {

                        VariantCampaignDTO vd = new VariantCampaignDTO();

                        vd.setId(v.getId());
                        vd.setSku(v.getSku());
                        vd.setColorway(v.getColorway());
                        vd.setSize(v.getSize());
                        BigDecimal price = productPriceRepository
                                .findFirstByVariantIdAndEndDateIsNull(v.getId())
                                .map(ProductPrice::getPrice)
                                .orElse(BigDecimal.ZERO);

                        vd.setPrice(price);

                        vd.setStock(v.getStock());

                        return vd;

                    }).toList();

            dto.setVariants(variants);

            return dto;

        }).toList();
    }
    /* ================= CREATE ================= */

    @PostMapping
    public CampaignResponse create(@RequestBody CreateCampaignRequest request) {
        return campaignService.createCampaign(request);
    }

    /* ================= ADD PRODUCT ================= */

    @PostMapping("/{id}/items")
    public void addVariant(
            @PathVariable Long id,
            @RequestParam Long variantId,
            @RequestParam BigDecimal price
    ) {

        campaignService.addVariantPrice(id, variantId, price);
    }

    /* ================= GET CAMPAIGN ================= */
    @GetMapping
    public List<PriceCampaign> getAllCampaign() {
        return campaignService.getAllCampaign();
    }
    @GetMapping("/{id}")
    public CampaignResponse getDetail(@PathVariable Long id) {
        return campaignService.getCampaignDetail(id);
    }
    @PutMapping("/campaign/{id}")
    public CampaignResponse updateCampaign(
            @PathVariable Long id,
            @RequestBody UpdateCampaignRequest request
    ) {
        return campaignService.updateCampaign(id, request);
    }
    @GetMapping("/check-price")
    public CheckPriceResponse checkPrice(@RequestParam Long variantId) {

        BigDecimal lowest = campaignService.findLowestCampaignPrice(variantId);

        CheckPriceResponse res = new CheckPriceResponse();
        res.setLowestPrice(lowest);

        return res;
    }
    @GetMapping("/final-price")
    public BigDecimal getFinalPrice(@RequestParam Long variantId) {
        return campaignService.getFinalPrice(variantId);
    }

}