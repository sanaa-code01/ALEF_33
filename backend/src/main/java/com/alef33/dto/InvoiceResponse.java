package com.alef33.dto;

import com.alef33.model.Invoice;
import com.alef33.model.enums.InvoiceStatus;
import com.alef33.service.dto.PaymentSummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceResponse(
        Long id,
        String number,
        Long orderId,
        String orderNumber,
        Long customerId,
        String customerName,
        LocalDate issueDate,
        LocalDate dueDate,
        InvoiceStatus status,
        BigDecimal totalHT,
        BigDecimal totalVat,
        BigDecimal totalTTC,
        BigDecimal paid,
        BigDecimal remaining,
        String notes,
        List<LineResponse> lines
) {
    public static InvoiceResponse from(Invoice i, PaymentSummary summary) {
        return new InvoiceResponse(i.getId(), i.getNumber(),
                i.getOrder() != null ? i.getOrder().getId() : null,
                i.getOrder() != null ? i.getOrder().getNumber() : null,
                i.getCustomer().getId(), i.getCustomer().getName(),
                i.getIssueDate(), i.getDueDate(), i.getStatus(),
                i.getTotalHT(), i.getTotalVat(), i.getTotalTTC(),
                summary.paid(), summary.remaining(), i.getNotes(),
                i.getLines().stream().map(LineResponse::from).toList());
    }
}
