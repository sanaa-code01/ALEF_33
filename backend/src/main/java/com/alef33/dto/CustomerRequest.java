package com.alef33.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "Le nom du client est obligatoire") @Size(max = 150, message = "Le nom est trop long") String name,
        @Size(max = 20, message = "L'ICE fait au maximum 20 caractères") String ice,
        @Email(message = "L'email n'est pas valide") String email,
        @Size(max = 30, message = "Le téléphone est trop long") String phone,
        @Size(max = 255, message = "L'adresse est trop longue") String address,
        @Size(max = 100, message = "La ville est trop longue") String city
) {}
