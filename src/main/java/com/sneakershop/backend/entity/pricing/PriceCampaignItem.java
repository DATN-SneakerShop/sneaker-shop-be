package com.sneakershop.backend.entity.pricing;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.sneakershop.backend.entity.product.ProductVariant;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "price_campaign_item")
public class PriceCampaignItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Chiến dịch
    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "campaign_id")
    private PriceCampaign campaign;

    // Variant sản phẩm
    @ManyToOne
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    // Giá trong chiến dịch
    private BigDecimal price;

}