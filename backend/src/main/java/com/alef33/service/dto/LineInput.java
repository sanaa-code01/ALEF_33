package com.alef33.service.dto;

import java.math.BigDecimal;

/**
 * What the user sends for one line.
 * If productId is set, missing designation / price / VAT are taken from the product.
 */
public record LineInput(
        Long productId,
        String designation,
        int quantity,
        BigDecimal unitPriceTTC,
        BigDecimal vatRate
) {}
