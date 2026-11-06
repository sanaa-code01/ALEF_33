package com.alef33.service.dto;

import com.alef33.model.enums.PaymentMethod;
import com.alef33.model.enums.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * What the user sends to record a payment.
 * paymentDate and type are optional: today's date and an automatic label are used.
 */
public record PaymentInput(
        BigDecimal amount,
        LocalDate paymentDate,
        PaymentMethod method,
        PaymentType type,
        String reference,
        String notes
) {}