package com.alef33.dto;

import com.alef33.model.DocumentLine;

import java.math.BigDecimal;

public record LineResponse(
        Long id,
        Long productId,
        String designation,
        int quantity,
        BigDecimal unitPriceHT,
        BigDecimal vatRate,
        BigDecimal lineTotalHT
) {
    public static LineResponse from(DocumentLine l) {
        return new LineResponse(l.getId(),
                l.getProduct() != null ? l.getProduct().getId() : null,
                l.getDesignation(), l.getQuantity(), l.getUnitPriceHT(),
                l.getVatRate(), l.getLineTotalHT());
    }
}
