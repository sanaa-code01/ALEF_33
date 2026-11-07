package com.alef33.pdf;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything needed to print a quote or an invoice, already prepared.
 * The renderer only draws this; it doesn't touch the database.
 */
public record PdfDocumentData(
        String title,                 // "Devis" or "Facture"
        String number,                // e.g. DEV-2026-0069 (printed as 0069/26)
        LocalDate issueDate,
        String secondDateLabel,       // "Valable jusqu'au" or "Échéance"
        LocalDate secondDate,
        String reference,             // e.g. "Commande : CMD-2026-0002" (optional)
        Customer customer,
        List<Line> lines,
        BigDecimal totalHT,
        BigDecimal totalVat,
        BigDecimal totalTTC,
        boolean showVatBreakdown,     // invoices show HT + TVA + TTC, quotes only TTC
        String notes,
        List<PaymentRow> payments,    // empty for a quote
        BigDecimal paid,              // null for a quote
        BigDecimal remaining,         // null for a quote
        String amountInWordsLabel     // "Arrêté le présent devis à la somme de :"
) {
    public record Customer(String name, String ice, String address, String city, String phone, String email) {}

    public record Line(String designation, int quantity, BigDecimal unitPriceTTC,
                       BigDecimal vatRate, BigDecimal lineTotalTTC) {}

    public record PaymentRow(LocalDate date, String type, String method, String reference, BigDecimal amount) {}
}
