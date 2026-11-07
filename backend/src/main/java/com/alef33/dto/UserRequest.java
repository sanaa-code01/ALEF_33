package com.alef33.dto;

import com.alef33.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "Email is required") @Email(message = "Invalid email") String email,
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must have at least 8 characters") String password,
        @NotBlank(message = "Full name is required") @Size(max = 150) String fullName,
        @NotNull(message = "Role is required") Role role
) {}
