package com.alef33.dto;

import com.alef33.model.enums.Role;

import java.time.Instant;

public record LoginResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        String email,
        String fullName,
        Role role
) {}
