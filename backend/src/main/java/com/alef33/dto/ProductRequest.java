package com.alef33.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "La référence est obligatoire") @Size(max = 50) String reference,
        @NotBlank(message = "La désignation est obligatoire") @Size(max = 200) String designation,
        @Size(max = 1000) String description,
        @NotNull(message = "Le prix TTC est obligatoire")
        @DecimalMin(value = "0.0", message = "Le prix doit être positif ou nul") BigDecimal unitPriceTTC,
        @DecimalMin(value = "0.0", message = "La TVA doit être entre 0 et 100 %") @DecimalMax(value = "100.0", message = "La TVA doit être entre 0 et 100 %") BigDecimal vatRate,     // optional, default 20
        Boolean active                                                  // optional, default true
) {}
