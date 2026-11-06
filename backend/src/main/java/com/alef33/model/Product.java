package com.alef33.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private String designation;

    @Column(length = 1000)
    private String description;

    @Column(name = "unit_price_ht", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceHT;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate = new BigDecimal("20.00");   // TVA Maroc 20 %

    private boolean active = true;
}