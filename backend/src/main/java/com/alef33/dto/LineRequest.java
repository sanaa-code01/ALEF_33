package com.alef33.dto;

import com.alef33.service.dto.LineInput;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** One line of a quote. Give a productId, or a designation + unitPriceTTC for a custom line. */
public record LineRequest(
        Long productId,
        @Size(max = 255) String designation,
        @NotNull(message = "La quantité est obligatoire") @Min(value = 1, message = "La quantité doit être au moins 1") Integer quantity,
        @DecimalMin(value = "0.0", message = "Le prix doit être positif ou nul") BigDecimal unitPriceTTC,
        @DecimalMin(value = "0.0", message = "La TVA doit être entre 0 et 100 %") @DecimalMax(value = "100.0", message = "La TVA doit être entre 0 et 100 %") BigDecimal vatRate
) {
    public LineInput toInput() {
        return new LineInput(productId, designation, quantity, unitPriceTTC, vatRate);
    }
}
