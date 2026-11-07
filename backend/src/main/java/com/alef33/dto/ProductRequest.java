package com.alef33.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Reference is required") @Size(max = 50) String reference,
        @NotBlank(message = "Designation is required") @Size(max = 200) String designation,
        @Size(max = 1000) String description,
        @NotNull(message = "Unit price TTC is required")
        @DecimalMin(value = "0.0", message = "Unit price must be 0 or more") BigDecimal unitPriceTTC,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal vatRate,     // optional, default 20
        Boolean active                                                  // optional, default true
) {}
