package com.alef33.dto;

import com.alef33.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(
        @NotNull(message = "Status is required") OrderStatus status
) {}
