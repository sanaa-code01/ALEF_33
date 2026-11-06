package com.alef33.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@MappedSuperclass
@Getter
@Setter
public abstract class SalesDocument extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String number;                 // e.g. DEV-2026-0001, CMD-..., FAC-...

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalHT = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalVat = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalTTC = BigDecimal.ZERO;

    @Column(length = 1000)
    private String notes;
}