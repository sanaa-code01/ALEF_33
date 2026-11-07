package com.alef33.dto;

import com.alef33.model.AppUser;
import com.alef33.model.enums.Role;

/** Never contains the password hash */
public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        boolean enabled
) {
    public static UserResponse from(AppUser u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.isEnabled());
    }
}
