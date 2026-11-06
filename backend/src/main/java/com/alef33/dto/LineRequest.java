package com.alef33.dto;

import com.alef33.service.dto.LineInput;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** One line of a quote. Give a productId, or a designation + unitPriceHT for a custom line. */
public record LineRequest(
        Long productId,
        @Size(max = 255) String designation,
        @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
        @DecimalMin(value = "0.0", message = "Unit price must be 0 or more") BigDecimal unitPriceHT,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal vatRate
) {
    public LineInput toInput() {
        return new LineInput(productId, designation, quantity, unitPriceHT, vatRate);
    }
}
