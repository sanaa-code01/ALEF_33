package com.alef33.dto;

import com.alef33.model.enums.PaymentMethod;
import com.alef33.model.enums.PaymentType;
import com.alef33.service.dto.PaymentInput;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequest(
        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0") BigDecimal amount,
        LocalDate paymentDate,                                  // optional, default today
        @NotNull(message = "Le mode de règlement est obligatoire") PaymentMethod method,
        PaymentType type,                                       // optional, guessed automatically
        @Size(max = 100) String reference,
        @Size(max = 500) String notes
) {
    public PaymentInput toInput() {
        return new PaymentInput(amount, paymentDate, method, type, reference, notes);
    }
}
