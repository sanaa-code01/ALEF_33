package com.alef33.dto;

import com.alef33.model.Quote;
import com.alef33.model.enums.QuoteStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record QuoteResponse(
        Long id,
        String number,
        Long customerId,
        String customerName,
        LocalDate issueDate,
        LocalDate validUntil,
        QuoteStatus status,
        BigDecimal totalHT,
        BigDecimal totalVat,
        BigDecimal totalTTC,
        String notes,
        List<LineResponse> lines
) {
    public static QuoteResponse from(Quote q) {
        return new QuoteResponse(q.getId(), q.getNumber(),
                q.getCustomer().getId(), q.getCustomer().getName(),
                q.getIssueDate(), q.getValidUntil(), q.getStatus(),
                q.getTotalHT(), q.getTotalVat(), q.getTotalTTC(), q.getNotes(),
                q.getLines().stream().map(LineResponse::from).toList());
    }
}
