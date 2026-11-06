package com.alef33.dto;

import com.alef33.model.Payment;
import com.alef33.model.enums.PaymentMethod;
import com.alef33.model.enums.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentResponse(
        Long id,
        BigDecimal amount,
        LocalDate paymentDate,
        PaymentMethod method,
        PaymentType type,
        String reference,
        String notes
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getAmount(), p.getPaymentDate(),
                p.getMethod(), p.getType(), p.getReference(), p.getNotes());
    }
}
