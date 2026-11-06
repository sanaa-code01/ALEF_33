package com.alef33.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String ice;          // Identifiant Commun de l'Entreprise (optional)

    private String email;

    private String phone;

    private String address;

    private String city;
}