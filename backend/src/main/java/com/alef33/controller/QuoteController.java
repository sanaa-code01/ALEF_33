package com.alef33.controller;

import com.alef33.dto.LineRequest;
import com.alef33.dto.OrderResponse;
import com.alef33.dto.QuoteRequest;
import com.alef33.dto.QuoteResponse;
import com.alef33.model.CustomerOrder;
import com.alef33.model.Quote;
import com.alef33.service.DocumentViewService;
import com.alef33.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
@Tag(name = "Quotes")
public class QuoteController {

    private final QuoteService quoteService;
    private final DocumentViewService views;

    @GetMapping
    @Operation(summary = "List quotes (newest first)")
    public List<QuoteResponse> list() {
        return views.quotes();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one quote with its lines")
    public QuoteResponse get(@PathVariable Long id) {
        return views.quote(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a quote (status DRAFT, totals calculated)")
    public QuoteResponse create(@Valid @RequestBody QuoteRequest request) {
        Quote quote = quoteService.create(request.customerId(),
                request.lines().stream().map(LineRequest::toInput).toList(),
                request.validUntil(), request.notes());
        return views.quote(quote.getId());
    }

    @PostMapping("/{id}/send")
    @Operation(summary = "Mark as sent to the customer (DRAFT -> SENT)")
    public QuoteResponse send(@PathVariable Long id) {
        quoteService.send(id);
        return views.quote(id);
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Customer accepted (DRAFT/SENT -> ACCEPTED)")
    public QuoteResponse accept(@PathVariable Long id) {
        quoteService.accept(id);
        return views.quote(id);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Customer rejected (DRAFT/SENT -> REJECTED)")
    public QuoteResponse reject(@PathVariable Long id) {
        quoteService.reject(id);
        return views.quote(id);
    }

    @PostMapping("/{id}/convert")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Turn an accepted quote into an order")
    public OrderResponse convert(@PathVariable Long id) {
        CustomerOrder order = quoteService.convertToOrder(id);
        return views.order(order.getId());
    }
}
