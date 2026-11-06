package com.alef33.service.dto;

import java.math.BigDecimal;

/** How much is due, paid and left for one order */
public record PaymentSummary(
        String orderNumber,
        BigDecimal totalTTC,
        BigDecimal paid,
        BigDecimal remaining
) {}