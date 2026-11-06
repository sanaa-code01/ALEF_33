package com.alef33.service;

import com.alef33.dto.InvoiceResponse;
import com.alef33.dto.OrderResponse;
import com.alef33.dto.QuoteResponse;
import com.alef33.exception.ResourceNotFoundException;
import com.alef33.model.CustomerOrder;
import com.alef33.model.Invoice;
import com.alef33.repository.InvoiceRepository;
import com.alef33.repository.OrderRepository;
import com.alef33.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Turns entities into response DTOs inside a transaction,
 * so lazy links (customer, lines) can be read safely.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentViewService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "id");

    private final QuoteRepository quoteRepository;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentService paymentService;

    // ── Quotes ────────────────────────────────────────────
    public QuoteResponse quote(Long id) {
        return QuoteResponse.from(quoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quote", id)));
    }

    public List<QuoteResponse> quotes() {
        return quoteRepository.findAll(NEWEST_FIRST).stream().map(QuoteResponse::from).toList();
    }

    // ── Orders ────────────────────────────────────────────
    public OrderResponse order(Long id) {
        CustomerOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
        return OrderResponse.from(order, paymentService.summary(id));
    }

    public List<OrderResponse> orders() {
        return orderRepository.findAll(NEWEST_FIRST).stream()
                .map(o -> OrderResponse.from(o, paymentService.summary(o.getId())))
                .toList();
    }

    // ── Invoices ──────────────────────────────────────────
    public InvoiceResponse invoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
        return InvoiceResponse.from(invoice, paymentService.summary(invoice.getOrder().getId()));
    }

    public List<InvoiceResponse> invoices() {
        return invoiceRepository.findAll(NEWEST_FIRST).stream()
                .map(i -> InvoiceResponse.from(i, paymentService.summary(i.getOrder().getId())))
                .toList();
    }
}
