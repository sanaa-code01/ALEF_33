package com.alef33.dto;

import com.alef33.model.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String reference,
        String designation,
        String description,
        BigDecimal unitPriceHT,
        BigDecimal vatRate,
        boolean active
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getReference(), p.getDesignation(),
                p.getDescription(), p.getUnitPriceHT(), p.getVatRate(), p.isActive());
    }
}