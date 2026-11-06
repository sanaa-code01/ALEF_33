package com.alef33.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@MappedSuperclass
@Getter
@Setter
public abstract class DocumentLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;               // optional: a custom line has no product

    @Column(nullable = false)
    private String designation;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price_ht", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceHT;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Column(name = "line_total_ht", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotalHT = BigDecimal.ZERO;
}