package com.alef33.exception;

import java.util.Map;

// Thrown when something is not found in the database (e.g. quote id 999)
public class ResourceNotFoundException extends RuntimeException {

    private static final Map<String, String> FRENCH = Map.of(
            "Customer", "Client",
            "Product", "Produit",
            "Quote", "Devis",
            "Order", "Commande",
            "Invoice", "Facture",
            "Payment", "Règlement",
            "User", "Utilisateur");

    public ResourceNotFoundException(String entity, Long id) {
        super(FRENCH.getOrDefault(entity, entity) + " introuvable (n° " + id + ")");
    }
}
