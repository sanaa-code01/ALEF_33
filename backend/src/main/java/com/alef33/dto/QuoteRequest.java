package com.alef33.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record QuoteRequest(
        @NotNull(message = "Customer is required") Long customerId,
        LocalDate validUntil,                                   // optional, default today + 30 days
        @Size(max = 1000) String notes,
        @NotEmpty(message = "A quote needs at least one line") List<@Valid LineRequest> lines
) {}
