package com.alef33.dto;

import com.alef33.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "L'email est obligatoire") @Email(message = "L'email n'est pas valide") String email,
        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères") String password,
        @NotBlank(message = "Le nom complet est obligatoire") @Size(max = 150) String fullName,
        @NotNull(message = "Le rôle est obligatoire") Role role
) {}
