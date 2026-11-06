package com.alef33.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "Name is required") @Size(max = 150) String name,
        @Size(max = 20) String ice,
        @Email(message = "Invalid email") String email,
        @Size(max = 30) String phone,
        @Size(max = 255) String address,
        @Size(max = 100) String city
) {}