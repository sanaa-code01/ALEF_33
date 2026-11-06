package com.alef33.dto;

import com.alef33.model.CustomerOrder;
import com.alef33.model.enums.OrderStatus;
import com.alef33.service.dto.PaymentSummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OrderResponse(
        Long id,
        String number,
        Long quoteId,
        String quoteNumber,
        Long customerId,
        String customerName,
        LocalDate orderDate,
        LocalDate deliveryDate,
        OrderStatus status,
        BigDecimal totalHT,
        BigDecimal totalVat,
        BigDecimal totalTTC,
        BigDecimal paid,
        BigDecimal remaining,
        String notes,
        List<LineResponse> lines
) {
    public static OrderResponse from(CustomerOrder o, PaymentSummary summary) {
        return new OrderResponse(o.getId(), o.getNumber(),
                o.getQuote() != null ? o.getQuote().getId() : null,
                o.getQuote() != null ? o.getQuote().getNumber() : null,
                o.getCustomer().getId(), o.getCustomer().getName(),
                o.getOrderDate(), o.getDeliveryDate(), o.getStatus(),
                o.getTotalHT(), o.getTotalVat(), o.getTotalTTC(),
                summary.paid(), summary.remaining(), o.getNotes(),
                o.getLines().stream().map(LineResponse::from).toList());
    }
}
