package com.sneakershop.backend.entity.pricing;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "price_campaign")
public class PriceCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên chiến dịch
    @Column(nullable = false)
    private String name;

    // Thời gian bắt đầu
    private LocalDateTime startTime;

    // Thời gian kết thúc
    private LocalDateTime endTime;

    // Trạng thái
    @Column(nullable = false)
    private Boolean active = true;

    // Danh sách sản phẩm trong chiến dịch
    @JsonManagedReference
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PriceCampaignItem> items;

}