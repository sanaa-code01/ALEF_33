package com.alef33.controller;

import com.alef33.dto.InvoiceResponse;
import com.alef33.service.DocumentViewService;
import com.alef33.service.PdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices")
public class InvoiceController {

    private final DocumentViewService views;
    private final PdfService pdfService;

    @GetMapping
    @Operation(summary = "List invoices with paid / remaining (newest first)")
    public List<InvoiceResponse> list() {
        return views.invoices();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one invoice with its lines and paid / remaining")
    public InvoiceResponse get(@PathVariable Long id) {
        return views.invoice(id);
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Download the invoice as a PDF (with payments and remaining amount)")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        return PdfResponses.of(pdfService.invoicePdf(id));
    }
}
