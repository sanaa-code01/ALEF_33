package com.alef33.controller;

import com.alef33.dto.InvoiceResponse;
import com.alef33.dto.OrderResponse;
import com.alef33.dto.OrderStatusRequest;
import com.alef33.dto.PaymentRequest;
import com.alef33.dto.PaymentResponse;
import com.alef33.model.Invoice;
import com.alef33.service.DocumentViewService;
import com.alef33.service.OrderService;
import com.alef33.service.PaymentService;
import com.alef33.service.dto.PaymentSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final DocumentViewService views;

    @GetMapping
    @Operation(summary = "List orders with paid / remaining (newest first)")
    public List<OrderResponse> list() {
        return views.orders();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one order with its lines and paid / remaining")
    public OrderResponse get(@PathVariable Long id) {
        return views.order(id);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change status: PENDING -> IN_PRODUCTION -> DELIVERED, or CANCELLED")
    public OrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest request) {
        orderService.changeStatus(id, request.status());
        return views.order(id);
    }

    @PostMapping("/{id}/invoice")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create the invoice of this order (counts payments already made)")
    public InvoiceResponse createInvoice(@PathVariable Long id) {
        Invoice invoice = orderService.createInvoice(id);
        return views.invoice(invoice.getId());
    }

    @GetMapping("/{id}/payments")
    @Operation(summary = "List the payments of this order")
    public List<PaymentResponse> payments(@PathVariable Long id) {
        return paymentService.listForOrder(id).stream().map(PaymentResponse::from).toList();
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a payment (deposit, installment, balance or full)")
    public PaymentResponse addPayment(@PathVariable Long id, @Valid @RequestBody PaymentRequest request) {
        return PaymentResponse.from(paymentService.addPayment(id, request.toInput()));
    }

    @GetMapping("/{id}/payment-summary")
    @Operation(summary = "Total, paid and remaining for this order")
    public PaymentSummary paymentSummary(@PathVariable Long id) {
        return paymentService.summary(id);
    }
}
