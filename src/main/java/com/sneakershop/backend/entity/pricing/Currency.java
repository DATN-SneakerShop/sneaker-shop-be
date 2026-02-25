package com.sneakershop.backend.entity.pricing;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(
        name = "currency",
        indexes = {
                @Index(name = "idx_currency_default", columnList = "is_default")
        }
)
public class Currency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10, nullable = false, unique = true)
    private String code;      // VND, USD

    @Column(length = 50, nullable = false)
    private String name;

    @Column(length = 10, nullable = false)
    private String symbol;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;
}
